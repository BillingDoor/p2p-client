package botnet_p2p.socket_layer;

import botnet_p2p.model.Communication;
import botnet_p2p.model.Peer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class SocketLayer extends Thread {
    private static final Logger logger = LogManager.getLogger(SocketLayer.class);

    private final MessageReceiver messageReceiver;
    private final NodeManager nodeManager;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    private Queue<PendingMessage> waitingForWrite;

    private CountDownLatch initLatch;
    private int port;


    public SocketLayer(BlockingQueue<ByteBuffer> receivedMessages,
                       CountDownLatch initLatch,
                       int port) {
        this.initLatch = initLatch;
        this.port = port;
        this.nodeManager = new NodeManager();
        this.messageReceiver = new MessageReceiver(receivedMessages, nodeManager);


        this.waitingForWrite = new LinkedList<>();
    }

    public void send(Communication<ByteBuffer> communication) throws IOException {
        logger.info("sending message to "
                + communication.getPeer().getAddress()
                + ":"
                + communication.getPeer().getPort()
        );
        this._send(communication.getData(), communication.getPeer());
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress("localhost", port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            if (initLatch != null) {
                this.initLatch.countDown();
            }

            while (true) {
                // blocking call, waiting for at least one ready channel
                int channels = selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();

                    if (key.isAcceptable()) {
                        logger.info("server: new connection is possible");
                        handleNewIncomingConnection(selector);
                    }

                    SocketAddress remoteAddress = null;
                    try {
                        if (key.isValid() && key.isConnectable()) {
                            logger.info("finishing connect");
                            SocketChannel channel = (SocketChannel) key.channel();
                            remoteAddress = channel.getRemoteAddress();
                            if (!channel.finishConnect()) {
                                logger.error("connect did not finish");
                            } else {
                                handleConnectionEstablished(channel);
                                logger.info("connect finished");
                            }
                        }

                        if (key.isReadable()) {
                            logger.trace("incoming message");
                            messageReceiver.handleNewMessage(key.channel());
                        }
                    } catch (ConnectException e) {
                        logger.error("connect to " + remoteAddress + " did not finish, removing");

                        // remove messages pending be send to this node
                        Iterator<PendingMessage> iter = waitingForWrite.iterator();
                        while (iter.hasNext()) {
                            PendingMessage m = iter.next();
                            if (m.destination.equals(remoteAddress)) {
                                iter.remove();
                                break;
                            }
                        }

                        // remove node
                        nodeManager.removeNode(remoteAddress);
                    }


                    it.remove();
                }

                // handle pending message write requests
                sendPendingMessages();

                // handle pending connect requests
                connectWaitingConnections();
            }
        } catch (InterruptedIOException e) {
            Thread.currentThread().interrupt();
            logger.info("Interrupted via InterruptedIOException");
        } catch (IOException e) {
            if (isInterrupted()) {
                logger.info("interrupted");
                Thread.currentThread().interrupt();
            } else {
                e.printStackTrace();
            }
        }
        logger.info("closing - loop ended");
    }

    private synchronized void _send(ByteBuffer bytes, Peer peer) throws IOException {
        InetSocketAddress destAddress = new InetSocketAddress(peer.getAddress(), peer.getPort());
        Optional<Node> node = nodeManager.getConnectedNodeByAddress(destAddress);
        if (node.isPresent()) {
            logger.info("already connected, sending message");
            node.get().socketChannel.write(bytes);
        } else {
            logger.info("not connected, message goes to queue");
            waitingForWrite.add(new PendingMessage(destAddress, bytes));
            selector.wakeup();
        }
    }

    private void handleConnectionEstablished(SocketChannel socketChannel) throws IOException {
        Optional<Node> node = nodeManager.getNodeByAddress((InetSocketAddress) socketChannel.getRemoteAddress());
        if (!node.isPresent()) {
            throw new RuntimeException("unknown node");
        }
        node.get().status = NodeStatus.CONNECTED;
        node.get().socketChannel = socketChannel;
        socketChannel.register(selector, SelectionKey.OP_READ);
        logger.info("connected to: " + getDestinationDescription(node.get().address));
    }

    private void sendToSocketChannel(ByteBuffer pendingMessage, SocketChannel socketChannel) throws IOException {
        socketChannel.write(pendingMessage);
    }

    private synchronized void sendPendingMessages() throws IOException {
        for (Iterator<PendingMessage> iter = waitingForWrite.iterator(); iter.hasNext(); ) {
            PendingMessage pendingMessage = iter.next();
            Optional<Node> node = nodeManager.getConnectedNodeByAddress(pendingMessage.destination);

            if (node.isPresent()) {
                logger.info("sending pending message to " + getDestinationDescription(pendingMessage.destination));
                sendToSocketChannel(pendingMessage.message, node.get().socketChannel);
                iter.remove();
            } else {
                if (!nodeManager.getNodeByAddress(pendingMessage.destination).isPresent()) {
                    nodeManager.add(new Node(pendingMessage.destination, NodeStatus.WAITING_FOR_CONNECT));
                }
            }
        }
    }

    private void connectTo(InetSocketAddress inetSocketAddress) throws IOException {
        logger.info("connecting to " + getDestinationDescription(inetSocketAddress));
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        boolean result = socketChannel.connect(inetSocketAddress);
        if (result) {
            handleConnectionEstablished(socketChannel);
        } else {
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            logger.info("connect will be finished later");
        }
    }

    private void connectWaitingConnections() {
        nodeManager.getByStatus(NodeStatus.WAITING_FOR_CONNECT)
                .forEach(botnetNode -> {
                    botnetNode.status = NodeStatus.CONNECTING;
                    try {
                        connectTo(botnetNode.address);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void handleNewIncomingConnection(Selector selector) throws IOException {
        SocketChannel clientSocket = serverSocketChannel.accept();
        clientSocket.configureBlocking(false);
        clientSocket.register(selector, SelectionKey.OP_READ);
        logger.info("server: new connection established");

        // connections are one way only so we do not add it to our nodes list
    }

    private String getDestinationDescription(InetSocketAddress inetSocketAddress) {
        return inetSocketAddress.getAddress().getHostAddress() + ":" + inetSocketAddress.getPort();
    }

    @Override
    public synchronized void interrupt() {
        super.interrupt();

        if (selector != null) {
            selector.keys().forEach(selectionKey -> {
                try {
                    selectionKey.channel().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            logger.info("clients sockets closed");
        }
        nodeManager.closeSockets();
    }

    public void shutdown() {
        logger.info("closing");
        this.interrupt();
    }
}

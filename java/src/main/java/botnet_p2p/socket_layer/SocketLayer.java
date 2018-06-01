package botnet_p2p.socket_layer;

import botnet_p2p.model.Communication;
import botnet_p2p.model.Peer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class SocketLayer extends Thread {
    private static final Logger logger = LogManager.getLogger(SocketLayer.class);

    private final MessageReceiver messageReceiver;
    private final NodeManager nodeManager;
    private Selector selector;
    private Queue<PendingMessage> waitingForWrite;

    private CountDownLatch initLatch;


    public SocketLayer(BlockingQueue<ByteBuffer> receivedMessages, CountDownLatch initLatch) {
        this.initLatch = initLatch;
        this.messageReceiver = new MessageReceiver(receivedMessages);
        this.nodeManager = new NodeManager();

        this.waitingForWrite = new LinkedList<>();
    }

    public void send(Communication<ByteBuffer> communication) throws IOException {
        logger.info("sending message to " + communication.getPeer().getAddress());
        this._send(communication.getData(), communication.getPeer());
    }

    @Override
    public void run() {
        logger.info("starting client");
        try {
            selector = Selector.open();

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

                    try {
                        if (key.isValid() && key.isConnectable()) {
                            logger.info("finishing connect");
                            SocketChannel channel = (SocketChannel) key.channel();
                            if (!channel.finishConnect()) {
                                logger.error("connect did not finish");
                            } else {
                                handleConnectionEstablished(channel);
                                logger.info("connect finished");
                            }
                        }

                        if (key.isReadable()) {
                            logger.info("incoming message");
                            messageReceiver.handleNewMessage(key.channel());
                        }
                    } catch (ConnectException e) {
                        logger.error("connect did not finish, TODO remove node?");
                    }



                    it.remove();
                }

                // handle pending message write requests
                sendPendingMessages();

                // handle pending connect requests
                connectWaitingConnections();
            }
        } catch (IOException e) {
            if (isInterrupted()) {
                logger.info("mainThread interrupted");
                Thread.currentThread().interrupt();
            } else {
                e.printStackTrace();
            }
        }
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

    private String getDestinationDescription(InetSocketAddress inetSocketAddress) {
        return inetSocketAddress.getAddress().getHostAddress() + ":" + inetSocketAddress.getPort();
    }

    @Override
    public void interrupt() {
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
    }
}

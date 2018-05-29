package botnet_p2p;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class Client extends Thread {
    private static final Logger logger = LogManager.getLogger(Client.class);
    private final MessageReceiver messageReceiver;
    private final NodeManager nodeManager;
    private Selector selector;
    private Queue<PendingMessage> waitingForWrite;
    private CountDownLatch initLatch;


    public Client(CountDownLatch initLatch,
                  MessageReceiver messageReceiver,
                  NodeManager nodeManager) {
        waitingForWrite = new LinkedList<>();
        this.messageReceiver = messageReceiver;
        this.nodeManager = nodeManager;
        this.initLatch = initLatch;
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

    public synchronized void sendMessage(MessageOuterClass.Message message, String address, int port) throws IOException {
        InetSocketAddress destAddress = new InetSocketAddress(address, port);
        Optional<BotnetNode> node = nodeManager.getConnectedNodeByAddress(destAddress);
        if (node.isPresent()) {
            logger.info("already connected, sending message");
            node.get().socketChannel.write(ByteBuffer.wrap(message.toByteArray()));
        } else {
            logger.info("not connected, message goes to queue");
            waitingForWrite.add(new PendingMessage(destAddress, message));
            selector.wakeup();
        }
    }

    public void sendMessage(MessageOuterClass.Message message, SocketChannel receiver) throws IOException {
        receiver.write(ByteBuffer.wrap(message.toByteArray()));
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

    private synchronized void sendPendingMessages() throws IOException {
        for (Iterator<PendingMessage> iter = waitingForWrite.iterator(); iter.hasNext(); ) {
            PendingMessage pendingMessage = iter.next();
            Optional<BotnetNode> node = nodeManager.getConnectedNodeByAddress(pendingMessage.destination);

            if (node.isPresent()) {
                logger.info("sending pending message to " + getDestinationDescription(pendingMessage.destination));
                sendPendingMessageNow(pendingMessage, node.get().socketChannel);
                iter.remove();
            } else {
                if (!nodeManager.getNodeByAddress(pendingMessage.destination).isPresent()) {
                    nodeManager.add(new BotnetNode(pendingMessage.destination, NodeStatus.WAITING_FOR_CONNECT));
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


    private void sendPendingMessageNow(PendingMessage pendingMessage, SocketChannel socketChannel) throws IOException {
        socketChannel.write(ByteBuffer.wrap(pendingMessage.message.toByteArray()));
    }

    private void handleConnectionEstablished(SocketChannel socketChannel) throws IOException {
        Optional<BotnetNode> node = nodeManager.getNodeByAddress((InetSocketAddress) socketChannel.getRemoteAddress());
        if (!node.isPresent()) {
            throw new RuntimeException("unknown node");
        }
        node.get().status = NodeStatus.CONNECTED;
        node.get().socketChannel = socketChannel;
        socketChannel.register(selector, SelectionKey.OP_READ);
        logger.info("connected to: " + getDestinationDescription(node.get().address));
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


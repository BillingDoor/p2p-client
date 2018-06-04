package botnet_p2p.business_logic_layer;

import static botnet_p2p.MessageOuterClass.Message;

import botnet_p2p.model.KademliaPeer;
import com.google.protobuf.ByteString;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

import static org.junit.Assert.*;

public class ChunkReaderTest {

    public static final KademliaPeer KADEMLIA_PEER = new KademliaPeer("127.0.0.1", 3000, "guid");
    public static final String SAVE_LOCATION = "./src/test/resources/tests/";

    @Test
    public void readSingleChunk() throws IOException {
        Message.FileChunkMsg singleChunk = Message.FileChunkMsg.newBuilder()
                .setData(ByteString.copyFrom("some data".getBytes()))
                .setFileName("file")
                .setFileSize(9)
                .setOrdinal(0)
                .setUuid("xyz")
                .build();

        ChunkReader chunkReader = new ChunkReader(SAVE_LOCATION);
        chunkReader.read(
                singleChunk,
                KADEMLIA_PEER
        );

        String targetFileName = new String(Base64.getEncoder().encode("file".getBytes()));
        assertEquals("Files are different",
                "some data",
                FileUtils.readFileToString(new File(SAVE_LOCATION + targetFileName), "utf-8"));
    }

    @Test
    public void readMultiChunk() throws IOException {
        Message.FileChunkMsg chunk1 = Message.FileChunkMsg.newBuilder()
                .setData(ByteString.copyFrom("other".getBytes()))
                .setFileName("file")
                .setFileSize(10)
                .setOrdinal(0)
                .setUuid("xyz")
                .build();
        Message.FileChunkMsg chunk2 = Message.FileChunkMsg.newBuilder()
                .setData(ByteString.copyFrom(" data".getBytes()))
                .setFileName("file")
                .setFileSize(10)
                .setOrdinal(1)
                .setUuid("xyz")
                .build();

        ChunkReader chunkReader = new ChunkReader(SAVE_LOCATION);
        chunkReader.read(
                chunk1,
                KADEMLIA_PEER
        );
        chunkReader.read(
                chunk2,
                KADEMLIA_PEER
        );

        String targetFileName = new String(Base64.getEncoder().encode("file".getBytes()));
        assertEquals("Files are different",
                "other data",
                FileUtils.readFileToString(new File(SAVE_LOCATION + targetFileName), "utf-8"));
    }

    @Test
    public void readMultiChunkNotAligned() throws IOException {
        Message.FileChunkMsg chunk1 = Message.FileChunkMsg.newBuilder()
                .setData(ByteString.copyFrom("other".getBytes()))
                .setFileName("file")
                .setFileSize(8)
                .setOrdinal(0)
                .setUuid("xyz")
                .build();
        Message.FileChunkMsg chunk2 = Message.FileChunkMsg.newBuilder()
                .setData(ByteString.copyFrom("YESno".getBytes()))
                .setFileName("file")
                .setFileSize(8)
                .setOrdinal(1)
                .setUuid("xyz")
                .build();

        ChunkReader chunkReader = new ChunkReader(SAVE_LOCATION);
        chunkReader.read(
                chunk1,
                KADEMLIA_PEER
        );
        chunkReader.read(
                chunk2,
                KADEMLIA_PEER
        );

        String targetFileName = new String(Base64.getEncoder().encode("file".getBytes()));
        assertEquals("Files are different",
                "otherYES",
                FileUtils.readFileToString(new File(SAVE_LOCATION + targetFileName), "utf-8"));
    }
}
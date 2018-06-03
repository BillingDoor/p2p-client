package botnet_p2p.business_logic_layer;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FileReaderTest {
    private static final String INPUT_DUMMY_2 = "src\\main\\resources\\dummy2.txt";
    private static final String INPUT_DUMMY_2_OUT = "src\\main\\resources\\dummy2_out.txt";


    @Test
    public void readFile1() {
        final String INPUT_DUMMY_1 = "src\\main\\resources\\dummy1.txt";

        FileReader fileReader = new FileReader();
        List<byte[]> bytes = fileReader.readFile(INPUT_DUMMY_1, 128).chunks;

        assertEquals(bytes.get(0)[0], '1');
        assertEquals(bytes.get(0)[1], '2');
        assertEquals(bytes.get(0)[2], '3');
        assertEquals(bytes.get(0)[3], '4');
    }

    @Test
    public void readFile_16() throws IOException {
        // file to chunks
        FileReader fileReader = new FileReader();
        FileReader.ChunkedFile chunks = fileReader.readFile(INPUT_DUMMY_2, 16);


        // chunks to file
        fileReader.dummyChunksToFile(chunks, INPUT_DUMMY_2_OUT);

        // verify
        assertEquals("Files are different",
                FileUtils.readFileToString(new File(INPUT_DUMMY_2), "utf-8"),
                FileUtils.readFileToString(new File(INPUT_DUMMY_2_OUT), "utf-8"));
    }

    @Test
    public void readFile_256() throws IOException {
        // file to chunks
        FileReader fileReader = new FileReader();
        FileReader.ChunkedFile chunks = fileReader.readFile(INPUT_DUMMY_2, 256);


        // chunks to file
        fileReader.dummyChunksToFile(chunks, INPUT_DUMMY_2_OUT);

        // verify
        assertEquals("Files are different",
                FileUtils.readFileToString(new File(INPUT_DUMMY_2), "utf-8"),
                FileUtils.readFileToString(new File(INPUT_DUMMY_2_OUT), "utf-8"));
    }
}
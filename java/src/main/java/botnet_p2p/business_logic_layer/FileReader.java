package botnet_p2p.business_logic_layer;

import lombok.AllArgsConstructor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class FileReader {
    /**
     * @param srcPath the path to the source file
     * @param chunkSize the size of a single chunk in bytes
     * @return the chunked file object
     */
    ChunkedFile readFile(String srcPath, int chunkSize) {
        Path path = FileSystems.getDefault().getPath(srcPath);

        List<byte[]> chunks = new ArrayList<>();
        FileInputStream fis = null;
        long size = 0;
        try {
            fis = new FileInputStream(path.toFile());
            byte[] buffer = new byte[chunkSize];
            int read = 0;
            while ((read = fis.read(buffer)) > 0) {
                size += read;
                chunks.add(buffer.clone());
            }
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ChunkedFile(chunks, size);
    }

    void dummyChunksToFile(ChunkedFile chunkedFile, String dstPath) {
        Path path = FileSystems.getDefault().getPath(dstPath);

        FileOutputStream fileOutputStream = null;
        try {
            long read = 0;
            int limit = (chunkedFile.chunks.size() - 1) * chunkedFile.chunks.get(0).length;
            fileOutputStream = new FileOutputStream(path.toFile());
            for (byte[] chunk : chunkedFile.chunks) {
                fileOutputStream.write(chunk);
                read += chunk.length;
                if (read >= limit) {
                    break;
                }
            }
            int remaining = (int) (chunkedFile.size - read);
            fileOutputStream.write(chunkedFile.chunks.get(chunkedFile.chunks.size()-1), 0, remaining);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @AllArgsConstructor
    class ChunkedFile {
        public List<byte[]> chunks;
        public long size;
    }
}

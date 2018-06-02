package old_crap

import (
	"os"
	"io"
)

type fileChunk []byte

type chunkedFile struct{
	path   string
	chunks []fileChunk
}


func buildFileFromChunks() {

}

func createChunkedFile(path string, chunkSize int) (chunkedFile, error) {
	var result chunkedFile
	chunks := make([]fileChunk, 0, 0)
	file, err := os.Open(path)
	if err != nil {
		return result, err
	}
	defer file.Close()

	for err != nil || err != io.EOF {
		chunk := make(fileChunk, 0, chunkSize)
		_, err = file.Read(chunk)
		chunks = append(chunks, chunk)
	}
	result.chunks = chunks
	result.path = path
	return result, nil
}

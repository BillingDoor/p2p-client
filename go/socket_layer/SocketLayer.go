package socket_layer

import (
	"github.com/lampo100/botnet_p2p/models"
	"net"
	"strconv"
	"log"
	"io"
	"encoding/binary"
	"sync"
)

var port uint32
var incomingMessagesChannel chan []byte

var terminateChannel chan struct{}
var hasTerminated chan struct{}

var openedConnections map[string]chan []byte
var openedConnectionsErrors map[string]chan error
var mutex = &sync.Mutex{}

func InitLayer(serverPort uint32, messageChannel chan []byte, terminate chan struct{}, thisTerminated chan struct{}) {
	openedConnections = make(map[string]chan []byte)
	openedConnectionsErrors = make(map[string]chan error)
	port = serverPort
	incomingMessagesChannel = messageChannel
	terminateChannel = terminate
	hasTerminated = thisTerminated
	go serverRoutine()
	log.Println("[SL] Initialized")
}

func serverRoutine() {
	listener, err := net.Listen("tcp4", ":"+strconv.Itoa(int(port)))
	if err != nil {
		log.Printf("[SL] Listening at port %d failed, %s\n", port, err)
		return
	}
	log.Printf("[SL] Listeninig at port: %d", port)
	defer listener.Close()
	newConnection := make(chan net.Conn)
	go func() {
		for {
			c, err := listener.Accept()
			if err != nil {
				return
			}
			newConnection <- c
		}
	}()
	for {
		select {
		case <-terminateChannel:
			log.Println("[SL] Terminated")
			hasTerminated <- struct{}{}
			return
		case conn := <-newConnection:
			log.Println("[SL] New connection at:", conn.RemoteAddr().String())
			go spawnConnection(conn)
		}
	}
}

func spawnConnection(conn net.Conn) {
	defer conn.Close()

	dataChannel := make(chan []byte, 1)

	go func() {
		for {
			sizeBuffer := make([]byte, 4)
			_, err := io.ReadFull(conn, sizeBuffer)
			if err != nil {
				if err != io.EOF {
					log.Printf("[SL] Error reading message %v\n", err)
				}
				return
			}
			size := int(binary.BigEndian.Uint32(sizeBuffer))
			buffer := make([]byte, size)
			n, err := io.ReadFull(conn, buffer)
			if err != nil {
				if err != io.EOF {
					log.Printf("[SL] Error reading message %v\n", err)
				}
				return
			}
			dataChannel <- buffer[:n]
		}
		log.Printf("[SL] Closing connection with %v\n", conn.RemoteAddr())
	}()

	for {
		select {
		case data := <-dataChannel:
			log.Printf("[SL] Read %v bytes from %v\n", len(data), conn.RemoteAddr())
			incomingMessagesChannel <- data
			break
		case <-terminateChannel:
			log.Printf("[SL] Closing connection with %v\n", conn.RemoteAddr())
			return
		}
	}
}

func Send(target models.Node, data []byte) error {
	addr := target.Host + ":" + strconv.Itoa(int(target.Port))

	sizeBytes := make([]byte, 4)
	binary.BigEndian.PutUint32(sizeBytes, uint32(len(data)))
	prefixedData := append(sizeBytes, data...)

	mutex.Lock()
	c, ok := openedConnections[addr]
	if !ok {
		channel := make(chan []byte, 1)
		errorChannel := make(chan error, 1)
		openedConnections[addr] = channel
		openedConnectionsErrors[addr] = errorChannel
		go handleConn(target, addr, channel, errorChannel)
		channel <- prefixedData
	} else {
		c <- prefixedData
	}
	mutex.Unlock()

	errorChannel, _ := openedConnectionsErrors[addr]
	err := <-errorChannel
	return err
}


func handleConn(target models.Node, addr string, channel chan []byte, errorChannel chan error) {
	log.Printf("[SL] Connecting to server: %v\n", addr)
	defer delete(openedConnections, addr)
	defer delete(openedConnectionsErrors, addr)

	conn, err := net.Dial("tcp4", addr)
	if err != nil {
		errorChannel <- err
		return
	}
	defer conn.Close()
	for {
		select {
		case d := <-channel:
			log.Printf("[SL] Sending message to %v\n", target)
			n, err := conn.Write(d)
			log.Printf("[SL] Sent %d bytes to %v\n", n, conn.RemoteAddr())
			errorChannel <- err
			break
		case <-terminateChannel:
			log.Printf("[SL] Closing connection with %v\n", conn.RemoteAddr())
			return
		}
	}
	log.Printf("[SL] Closing connection with %v\n", conn.RemoteAddr())
	delete(openedConnections, addr)
	delete(openedConnectionsErrors, addr)
}
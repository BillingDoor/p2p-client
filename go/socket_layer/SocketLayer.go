package socket_layer

import (
	"github.com/lampo100/botnet_p2p/models"
	"net"
	"strconv"
	"log"
	"io"
)

var port uint32
var incomingMessagesChannel chan []byte

var terminateChannel chan struct{}
var hasTerminated chan struct{}

func InitLayer(serverPort uint32, messageChannel chan []byte, terminate chan struct{}, thisTerminated chan struct{}) {
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
		log.Fatalf("[SL] Listening at port %d failed, %s", port, err)
		return
	}
	log.Printf("[SL] Listeninig at port: %d", port)
	defer listener.Close()
	newConnection := make(chan net.Conn)
	go func() {
		for {
			c, err := listener.Accept()
			if err != nil {
				//TODO: handle closed socket
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
	buffer := make([]byte, 0, 12000)
	n, err := conn.Read(buffer)
	if err == io.EOF {
		return
	}
	incomingMessagesChannel <- buffer[:n]
}

func Send(target models.Node, data []byte) error {
	addr := target.Host + ":" + strconv.Itoa(int(target.Port))
	log.Printf("[SL] Connecting to: %v\n", addr)

	conn, err := net.Dial("tcp4", addr)
	if err != nil {
		return err
	}
	defer conn.Close()
	log.Printf("[SL] Sending message to %v\n", target)
	conn.Write(data)
	return nil
}

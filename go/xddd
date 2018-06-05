package main

import (
	"github.com/lampo100/botnet_p2p/application_layer"
	"os"
	"strconv"
)

func main() {
	listenPort, _ := strconv.Atoi(os.Args[1])
	connectPort, _ := strconv.Atoi(os.Args[2])
	application_layer.RunApplication(uint32(listenPort), uint32(connectPort))
}
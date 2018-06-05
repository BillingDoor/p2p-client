package main

import (
	"github.com/lampo100/botnet_p2p/application_layer"
	"os"
	"strconv"
)

func main() {
	ip := os.Args[1]
	listenPort, _ := strconv.Atoi(os.Args[2])
	connectPort, _ := strconv.Atoi(os.Args[3])
	application_layer.RunApplication(ip, uint32(listenPort), uint32(connectPort))
}

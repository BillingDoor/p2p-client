package main

import (
	"github.com/lampo100/botnet_p2p/application_layer"
	"os"
	"strconv"
)

func main() {
	port, _ := strconv.Atoi(os.Args[1])
	application_layer.RunApplication(uint32(port))
}
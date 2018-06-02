package p2p_layer

import (
	"github.com/lampo100/botnet_p2p/models"
	"log"
	"github.com/lampo100/botnet_p2p/message_layer"
)

var routingTable models.BucketList
var BBLMessageChannel chan models.Message
var myNode models.Node

var terminateChannel chan struct{}
var hasTerminated chan struct{}
var nextLayerTerminated chan struct{}

func InitLayer(selfNode models.Node, messageChannel chan models.Message, terminate chan struct{}, thisTerminated chan struct{}) {
	BBLMessageChannel = messageChannel
	myNode = selfNode
	terminateChannel = terminate
	hasTerminated = thisTerminated
	nextLayerTerminated = make(chan struct{})
	routingTable.Init(myNode)
	message_layer.InitLayer(myNode, BBLMessageChannel, terminateChannel, nextLayerTerminated)
	log.Println("[P2] Initialized")
	go func() {
		<-terminateChannel
		<-nextLayerTerminated
		log.Println("[P2] Terminated")
		hasTerminated <- struct{}{}
	}()
}

func Ping(selfNode, targetNode models.Node) error {
	return message_layer.Ping(selfNode, targetNode)
}

func PingResponse(selfNode, targetNode models.Node) error {
	return message_layer.PingResponse(selfNode, targetNode)
}

func FindNode(selfNode, targetNode models.Node, guid models.UUID) error {
	return message_layer.FindNode(selfNode, targetNode, guid)
}

func FoundNodes(selfNode, targetNode models.Node, guid models.UUID) error {
	return message_layer.FoundNodes(selfNode, targetNode, routingTable.NearestNodes(guid, 100))
}

func AddNodeToRoutingTable(node models.Node) {
	routingTable.Insert(node)
}

func RemoveFromRoutingTable(node models.Node) {
	routingTable.Remove(node)
}

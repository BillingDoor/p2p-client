package p2p_layer

import (
	"github.com/lampo100/botnet_p2p/models"
	"log"
	"github.com/lampo100/botnet_p2p/message_layer"
)

var routingTable models.BucketList
var BBLMessageChannel chan models.Message
var myNode models.Node

func InitLayer(selfNode models.Node, messageChannel chan models.Message) {
	BBLMessageChannel = messageChannel
	myNode = selfNode
	routingTable.Init(myNode)
	message_layer.InitLayer(myNode, BBLMessageChannel)
	log.Println("[P2] Initialized")
}

func Ping(selfNode, targetNode models.Node) {
	message_layer.Ping(selfNode, targetNode)
}

func PingResponse(selfNode, targetNode models.Node) {
	message_layer.PingResponse(selfNode, targetNode)
}

func FindNode(selfNode, targetNode models.Node, guid models.UUID) {
	message_layer.FindNode(selfNode, targetNode, guid)
}

func FoundNodes(selfNode, targetNode models.Node, guid models.UUID) {
	message_layer.FoundNodes(selfNode, targetNode, routingTable.NearestNodes(guid, 100))
}

func AddNodeToRoutingTable(node models.Node) {
	routingTable.Insert(node)
}

func RemoveFromRoutingTable(node models.Node) {
	routingTable.Remove(node)
}
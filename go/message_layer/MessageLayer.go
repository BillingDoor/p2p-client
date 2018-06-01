package message_layer

import (
	"github.com/lampo100/botnet_p2p/models"
	"github.com/lampo100/botnet_p2p/socket_layer"
	"log"
	"github.com/golang/protobuf/proto"
)

var myNode models.Node
var messageChannel chan models.Message
var dataChannel chan []byte

func createBaseMessage(sender, receiver models.Node, messageType models.Message_MessageType) models.Message {
	message := models.Message{
		Sender: &models.Message_Contact{
			Guid:  sender.Guid.String(),
			IP:    sender.Host,
			Port:  sender.Port,
			IsNAT: sender.IsNAT,
		},
		Receiver: &models.Message_Contact{
			Guid:  receiver.Guid.String(),
			IP:    receiver.Host,
			Port:  receiver.Port,
			IsNAT: receiver.IsNAT,
		},
		Type: messageType,
	}
	return message
}

func sendMessage(target models.Node, msg models.Message) error {
	bytes, err := proto.Marshal(&msg)
	if err != nil {
		return err
	}
	err = socket_layer.Send(target, bytes)
	return err
}

func messageRoutine() {
	for {
		select {
		case data := <-dataChannel:
			var msg models.Message
			err := proto.Unmarshal(data, &msg)
			if err != nil {
				log.Printf("[ML] Error decoding message: %v\n", err)
				continue
			}
			messageChannel <- msg
			break
		//Terminate signal
		}
	}
}

func InitLayer(selfNode models.Node, messChannel chan models.Message) {
	myNode = selfNode
	messageChannel = messChannel
	socket_layer.InitLayer(myNode.Port, dataChannel)
	go messageRoutine()
	log.Println("[ML] Initialized")
}

func FindNode(sender, receiver models.Node, guid models.UUID) error {
	message := createBaseMessage(sender, receiver, models.Message_FIND_NODE)
	message.Payload = &models.Message_FindNode{
		FindNode: &models.Message_FindNodeMsg{
			Guid: guid.String(),
		},
	}
	return sendMessage(receiver, message)
}

func FoundNodes(sender, receiver models.Node, nodes []models.Node) error {
	nodesMessages := make([]*models.Message_Contact, 0)

	for _, n := range nodes {
		nodesMessages = append(nodesMessages, &models.Message_Contact{
			Guid:  n.Guid.String(),
			IP:    n.Host,
			Port:  n.Port,
			IsNAT: n.IsNAT,
		})
	}

	message := createBaseMessage(sender, receiver, models.Message_FOUND_NODES)
	message.Payload = &models.Message_FoundNodes{
		FoundNodes: &models.Message_FoundNodesMsg{
			Nodes: nodesMessages,
		},
	}
	return sendMessage(receiver, message)
}

func Ping(sender, receiver models.Node) error {
	message := createBaseMessage(sender, receiver, models.Message_PING)
	return sendMessage(receiver, message)
}

func PingResponse(sender, receiver models.Node) error {
	message := createBaseMessage(sender, receiver, models.Message_PING_RESPONSE)
	return sendMessage(receiver, message)
}

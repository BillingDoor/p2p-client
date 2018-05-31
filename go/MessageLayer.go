package main

import "github.com/golang/protobuf/proto"

func createFindNodeMessage(selfNode, receiverNode Node, guid UUID) ([]byte, error) {
	message := Message{
		Sender:   selfNode.guid.String(),
		Receiver: receiverNode.guid.String(),
		Type:     Message_FIND_NODE,
		Payload: &Message_FindNode{
			&Message_FindNodeMsg{
				Guid: selfNode.guid.String(),
			},
		},
	}
	return proto.Marshal(&message)
}

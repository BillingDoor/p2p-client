package main

func createBaseMessage(sender, receiver Node, messageType Message_MessageType) Message {
	message := Message{
		Sender: &Message_Contact{
			Guid:  sender.guid.String(),
			IP:    sender.host,
			Port:  sender.port,
			IsNAT: sender.isNAT,
		},
		Receiver: &Message_Contact{
			Guid:  receiver.guid.String(),
			IP:    receiver.host,
			Port:  receiver.port,
			IsNAT: receiver.isNAT,
		},
		Type: messageType,
	}
	return message
}

func createFindNodeMessage(sender, receiver Node, guid UUID) Message {
	message := createBaseMessage(sender, receiver, Message_FIND_NODE)
	message.Payload = &Message_FindNode{
		FindNode: &Message_FindNodeMsg{
			Guid: guid.String(),
		},
	}
	return message
}

func createFoundNodesMessage(sender, receiver Node, nodes []Node) Message {
	nodesMessages := make([]*Message_Contact, 0)

	for _, n := range nodes {
		nodesMessages = append(nodesMessages, &Message_Contact{
			Guid: n.guid.String(),
			IP: n.host,
			Port: n.port,
			IsNAT: n.isNAT,
		})
	}

	message := createBaseMessage(sender, receiver, Message_FOUND_NODES)
	message.Payload = &Message_FoundNodes{
		FoundNodes: &Message_FoundNodesMsg{
			Nodes: nodesMessages,
		},
	}
	return message
}



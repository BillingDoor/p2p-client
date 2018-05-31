package main

var selfNode Node

func generateSelfNode() (Node, error) {
	ip, err := getRemoteIP()
	if err != nil {
		return Node{}, err
	}
	isNAT, err := checkNAT()
	if err != nil {
		return Node{}, err
	}

	node := Node{
		host:  ip,
		isNAT: isNAT,
		guid:  GenerateGUID(),
		port:  defaultPort,
	}
	return node, nil
}

func joinNetwork(bootstrapNode Node) (bool, error) {
	node, err := generateSelfNode()
	if err != nil {
		return false, err
	}
	selfNode = node
	findNode(bootstrapNode, selfNode)
	return true, nil
}

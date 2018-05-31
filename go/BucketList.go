package main

import "sort"

type Bucket []Node
type BucketList struct {
	bucketSize    int
	bucketsNumber int
	buckets       []Bucket
	hostNode      Node
}

func (b *BucketList) Init(node Node) {
	b.hostNode = node
}

func (b *Bucket) contains(node Node) bool {
	for _, a := range *b {
		if a.guid == node.guid {
			return true
		}
	}
	return false
}

func (l *BucketList) insert(node Node) {
	bucketNumber := l.hostNode.guid.largestDifferingBit(node.guid)
	bucket := l.buckets[bucketNumber]
	if len(bucket) >= l.bucketSize {
		bucket = bucket [1:]
	}
	if !bucket.contains(node) {
		bucket = append(bucket, node)
	}
}

func (l *BucketList) removeNode(node Node) {
	//TODO: remove element from list
}

func (l *BucketList) nearestNodes(targetUUID UUID, limit int) []Node {
	nodes := make([]Node, 0)
	numResults := limit
	if numResults > l.bucketSize {
		numResults = l.bucketSize
	}
	for _, bucket := range l.buckets {
		for _, node := range bucket {
			nodes = append(nodes, node)
		}
	}
	sort.Slice(nodes, func(i, j int) bool {
		return nodes[i].guid < nodes[j].guid
	})
	return nodes
}
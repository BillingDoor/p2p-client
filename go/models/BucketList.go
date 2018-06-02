package models

import (
	"sort"
	"fmt"
)

type Bucket []Node
type BucketList struct {
	bucketSize    int
	bucketsNumber int
	buckets       []Bucket
	hostNode      Node
}

func (b *Bucket) Contains(node Node) bool {
	for _, a := range *b {
		if a.Guid == node.Guid {
			return true
		}
	}
	return false
}

func (b *Bucket) IndexOf(node Node) int {
	for idx, a := range *b {
		if a.Guid == node.Guid {
			return idx
		}
	}
	return -1
}

func (b *BucketList) Init(node Node) {
	b.hostNode = node
	b.bucketSize = 10
	b.bucketsNumber = 64
	b.buckets = make([]Bucket, b.bucketsNumber)
}

func (l *BucketList) Insert(node Node) {
	bucketNumber := l.hostNode.Guid.largestDifferingBit(node.Guid)
	bucket := l.buckets[bucketNumber]
	if len(bucket) >= l.bucketSize {
		bucket = bucket [1:]
	}
	if !bucket.Contains(node) {
		bucket = append(bucket, node)
	}
	l.buckets[bucketNumber] = bucket
}

func (l *BucketList) Remove(node Node) {
	for _, bucket := range l.buckets {
		if bucket.Contains(node) {
			i := bucket.IndexOf(node)
			bucket = append(bucket[:i], bucket[i+1:]...)
		}
	}
}

func (l *BucketList) NearestNodes(targetUUID UUID, limit int) []Node {
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
		return nodes[i].Guid < nodes[j].Guid
	})
	return nodes
}

func (l *BucketList) String() string {
	str := ""
	for idx, b := range l.buckets {
		if len(b) != 0 {
			str += fmt.Sprintf("Bucket %v, size %v:\n", idx, len(b))
			for _, node := range b {
				str += fmt.Sprintf("\t%v\n", node)
			}
		}
	}
	return str
}

func (l* BucketList) GetAllNodes() []Node {
	nodes := make([]Node, 0)
	for _, bucket := range l.buckets {
		for _, node := range bucket {
			nodes = append(nodes, node)
		}
	}
	return nodes
}
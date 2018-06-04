package models

import (
	"math/rand"
	"strconv"
	"fmt"
)

type UUID uint64

type Node struct {
	Host  string
	Port  uint32
	Guid  UUID
	IsNAT bool
}

func GenerateGUID() UUID {
	return UUID(rand.Uint64())
}

func (a UUID) distance(b UUID) UUID {
	return a ^ b
}

func (a UUID) largestDifferingBit(b UUID) int {
	distance := a.distance(b)
	length := -1
	for distance != 0 {
		distance = distance >> 1
		length++
	}
	if length > 0 {
		return length
	}
	return 0
}

func (a UUID) String() string {
	return fmt.Sprint(uint64(a))
}

func GuidFromString(str string) UUID {
	val, err := strconv.ParseUint(str, 10, 64)
	if err != nil {
		val = 0
	}
	return UUID(uint64(val))
}

func (a *Node) Equals(b Node) bool {
	return a.Guid == b.Guid
}

func (msg *Message_Contact) ToNode() Node {
	return Node{
		Guid:  GuidFromString(msg.Guid),
		Host:  msg.IP,
		Port:  msg.Port,
		IsNAT: msg.IsNAT,
	}
}

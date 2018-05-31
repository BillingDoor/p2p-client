package main

import (
	"math/rand"
	"strconv"
)

type UUID uint64

type Node struct {
	host string
	port uint32
	guid  UUID
	isNAT bool
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

func (a *UUID) String() string {
	return strconv.Itoa(int(*a))
}
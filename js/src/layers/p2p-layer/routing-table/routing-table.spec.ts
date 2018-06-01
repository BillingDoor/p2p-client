import { sortBy, prop } from 'ramda';

import { Contact } from '@models';
import { RoutingTable } from './routing-table';

describe('RoutingTable', () => {
  let selfNode: Contact;
  let routingTable: RoutingTable;

  beforeEach(() => {
    selfNode = new Contact({
      address: {
        host: 'foo',
        port: 1234
      },
      guid: '7'
    });
    routingTable = new RoutingTable(selfNode);
  });

  describe('Scenario: adding node', () => {
    let node: Contact;
    const guid = '6';

    beforeEach(() => {
      node = new Contact({
        address: {
          host: 'bar',
          port: 2345
        },
        guid
      });
    });

    it('Method: "addNode" should exist', () => {
      const result = typeof routingTable.addNode === 'function';
      expect(result).toBe(true);
    });

    it('Method: "getNodeByGUID" should exist', () => {
      const result = typeof routingTable.getNodeByGUID === 'function';
      expect(result).toBe(true);
    });

    describe('When: node not added', () => {
      it('should not have node', () => {
        const result = routingTable.getNodeByGUID(guid);
        expect(result).toBe(undefined);
      });
    });

    describe('When: node added', () => {
      beforeEach(() => {
        routingTable.addNode(node);
      });

      it('should have node', () => {
        const result = routingTable.getNodeByGUID(guid);
        expect(result).toBe(node);
      });

      it('should allow to add another node', () => {
        const anotherGUID = '3';
        const anotherNode = new Contact({
          address: {
            host: 'baz',
            port: 3456
          },
          guid: anotherGUID
        });

        routingTable.addNode(anotherNode);
        const result = routingTable.getNodeByGUID(anotherGUID);
        expect(result).toBe(anotherNode);
      });
    });
  });

  describe('Method: "removeNode"', () => {
    let node: Contact;
    const guid = '6';

    beforeEach(() => {
      node = new Contact({
        address: {
          host: 'bar',
          port: 2345
        },
        guid
      });
    });

    it('should exist', () => {
      const result = typeof routingTable.removeNode === 'function';
      expect(result).toBe(true);
    });

    describe('When: node is in routing table', () => {
      beforeEach(() => {
        routingTable.addNode(node);
      });

      it('should remove node', () => {
        routingTable.removeNode(node);
        const result = routingTable.getNodeByGUID(guid);
        expect(result).toBe(undefined);
      });
    });

    describe('When: two nodes are in routing table', () => {
      let otherNode: Contact;
      const otherGUID = '9';

      beforeEach(() => {
        otherNode = new Contact({
          address: {
            host: 'baz',
            port: 3456
          },
          guid: otherGUID
        });
        routingTable.addNode(node);
        routingTable.addNode(otherNode);
      });

      it('should remove only the given node', () => {
        routingTable.removeNode(node);

        const nodeResult = routingTable.getNodeByGUID(guid);
        const otherNodeResult = routingTable.getNodeByGUID(otherGUID);
        expect(nodeResult).toBe(undefined);
        expect(otherNodeResult).toBe(otherNode);
      });
    });
  });

  describe('Method: "getNearestNodes"', () => {
    let node: Contact;
    let nodes: Contact[];

    beforeEach(() => {
      node = new Contact({
        address: {
          host: 'bar',
          port: 2345
        }
      });
      nodes = [
        new Contact({
          address: {
            host: 'baz',
            port: 3456
          }
        }),
        new Contact({
          address: {
            host: 'bis',
            port: 4567
          }
        })
      ];
      [...nodes, node].forEach((node) => routingTable.addNode(node));
    });

    it('should exist', () => {
      const result = typeof routingTable.getNearestNodes === 'function';
      expect(result).toBe(true);
    });

    describe('When: called with guid', () => {
      it('should not return node with that guid', () => {
        const nearestNodes = routingTable.getNearestNodes(node.guid);
        const result = nearestNodes.includes(node);
        expect(result).toBe(false);
      });

      it('should return nodes', () => {
        const nearestNodes = routingTable.getNearestNodes(node.guid);
        const result = sortBy<Contact>(prop('guid'), nearestNodes);
        const expected = sortBy<Contact>(prop('guid'), nodes);
        expect(result).toEqual(expected);
      });
    });
  });
});

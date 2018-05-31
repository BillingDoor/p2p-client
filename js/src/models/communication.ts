import { Address } from "./address";

export interface Communication<T> {
  data: T;
  address: Address;
}

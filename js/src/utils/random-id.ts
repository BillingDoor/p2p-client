import * as bigInt from 'big-integer';

export function generateID() {
  return bigInt.randBetween(0, 2 ** 64).toString();
}

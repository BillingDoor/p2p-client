export function mostSignificantBit(value: bigInt.BigInteger) {
  value = value.or(value.shiftRight(1));
  value = value.or(value.shiftRight(2));
  value = value.or(value.shiftRight(4));
  value = value.or(value.shiftRight(8));
  value = value.or(value.shiftRight(16));
  value = value.or(value.shiftRight(32));
  value = value.plus(1);
  return value.shiftRight(1);
}

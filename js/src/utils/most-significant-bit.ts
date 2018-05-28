export function mostSignificantBit(value: number) {
  value |= value >> 1;
  value |= value >> 2;
  value |= value >> 4;
  value |= value >> 8;
  value |= value >> 16;
  value++;
  return value >> 1;
}

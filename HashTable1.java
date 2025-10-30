final class HashTable1 extends AbstractHashTable {
    public HashTable1(int capacity) { super(capacity); }

    // FNV-1a like 32-bit with small avalanche; then floorMod to capacity
    @Override
    protected int hash(String key) {
        long h = 0x811C9DC5L; // FNV offset 32-bit
        for (int i = 0; i < key.length(); i++) {
            h ^= key.charAt(i);
            h *= 0x01000193L; // FNV prime 32-bit
        }
        // finalizer (mix)
        h ^= (h >>> 13);
        h *= 0x85EBCA6BL;
        h ^= (h >>> 16);
        return Math.floorMod((int) h, capacity);
    }
}
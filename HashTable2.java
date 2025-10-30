final class HashTable2 extends AbstractHashTable {
    public HashTable2(int capacity) { super(capacity); }

    // djb2 variant (h*33 ^ c) with extra mix; then floorMod
    @Override
    protected int hash(String key) {
        long h = 5381L;
        for (int i = 0; i < key.length(); i++) {
            h = ((h << 5) + h) ^ key.charAt(i); // h*33 ^ c
        }
        // mix
        h ^= (h >>> 15);
        h *= 0x27D4EB2DL;
        h ^= (h >>> 15);
        return Math.floorMod((int) h, capacity);
    }
}
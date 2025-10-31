final class HashTable2 extends AbstractHashTable {
    public HashTable2(int capacidade) { super(capacidade); }

    // djb2 (h*33 ^ c) com mix; índice por mod positivo manual
    @Override
    protected int hash(String chave) {
        long h = 5381L;
        for (int i = 0; i < chave.length(); i++) {
            h = ((h << 5) + h) ^ chave.charAt(i); // h*33 ^ c
        }
        // mistura simples
        h = h ^ (h >>> 15);
        h = h * 0x27D4EB2DL;
        h = h ^ (h >>> 15);
        int h32 = (int) h;
        return modPositivo(h32, capacidade);
    }
}

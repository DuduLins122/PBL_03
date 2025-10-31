final class HashTable1 extends AbstractHashTable {
    public HashTable1(int capacidade) { super(capacidade); }

    // FNV-1a 32-bit com um leve mix final, índice por mod positivo manual
    @Override
    protected int hash(String chave) {
        long h = 0x811C9DC5L; // offset basis
        for (int i = 0; i < chave.length(); i++) {
            h = h ^ chave.charAt(i);
            h = h * 0x01000193L; // prime
        }
        // mistura simples
        h = h ^ (h >>> 13);
        h = h * 0x85EBCA6BL;
        h = h ^ (h >>> 16);
        int h32 = (int) h;
        return modPositivo(h32, capacidade);
    }
}

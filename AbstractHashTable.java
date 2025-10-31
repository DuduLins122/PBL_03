abstract class AbstractHashTable {
    protected static final int MAX_CAPACITY = 32; // regra da professora
    protected final Node[] tabela;
    protected final int capacidade;
    protected int tamanho;
    protected long colisoesTotais;

    public AbstractHashTable(int capacidadeDesejada) {
        int cap = capacidadeDesejada;
        if (cap <= 0 || cap > MAX_CAPACITY) cap = MAX_CAPACITY;
        this.capacidade = cap;
        this.tabela = new Node[capacidade];
        this.tamanho = 0;
        this.colisoesTotais = 0;
    }

    protected abstract int hash(String chave);

    public final void insert(String chave) {
        if (chave == null) throw new NullPointerException("chave nula");
        int idx = hash(chave);
        Node cabeca = tabela[idx];

        if (cabeca != null) {
            Node atual = cabeca;
            while (atual != null) {
                if (iguais(atual.chave, chave)) return; // evita duplicata
                atual = atual.proximo;
            }
            colisoesTotais = colisoesTotais + 1;
        }

        Node novo = new Node(chave);
        novo.proximo = cabeca;
        tabela[idx] = novo;
        tamanho = tamanho + 1;
    }

    public final boolean contains(String chave) {
        if (chave == null) throw new NullPointerException("chave nula");
        int idx = hash(chave);
        Node atual = tabela[idx];
        while (atual != null) {
            if (iguais(atual.chave, chave)) return true;
            atual = atual.proximo;
        }
        return false;
    }

    public final int size() { return tamanho; }
    public final int capacity() { return capacidade; }
    public final long collisionsTotal() { return colisoesTotais; }

    public final int[] distribution() {
        int[] dist = new int[capacidade];
        for (int i = 0; i < capacidade; i++) {
            int c = 0;
            Node atual = tabela[i];
            while (atual != null) { c = c + 1; atual = atual.proximo; }
            dist[i] = c;
        }
        return dist;
    }

    public final int[] collisionsPerBucket() {
        int[] dist = distribution();
        int[] col = new int[capacidade];
        for (int i = 0; i < capacidade; i++) {
            int v = dist[i] - 1;
            if (v < 0) v = 0;
            col[i] = v;
        }
        return col;
    }

    public final int maxChainLength() {
        int max = 0;
        for (int i = 0; i < capacidade; i++) {
            int len = 0;
            Node atual = tabela[i];
            while (atual != null) { len = len + 1; atual = atual.proximo; }
            if (len > max) max = len;
        }
        return max;
    }

    public final int nonEmptyBuckets() {
        int c = 0;
        for (int i = 0; i < capacidade; i++) if (tabela[i] != null) c = c + 1;
        return c;
    }

    public final double avgChainLenNonEmpty() {
        int naoVazios = nonEmptyBuckets();
        if (naoVazios == 0) return 0.0;
        return ((double) tamanho) / ((double) naoVazios);
    }

    private boolean iguais(String a, String b) {
        return a.equals(b); 
    }

    
    protected final int modPositivo(int valor, int m) {
        int r = valor % m;
        if (r < 0) r = r + m;
        return r;
    }
}

import java.util.Objects;

abstract class AbstractHashTable {
    protected static final int MAX_CAPACITY = 32;
    protected final Node[] table;
    protected final int capacity;
    protected int size;
    protected long collisionsTotal;

    public AbstractHashTable(int capacity) {
        if (capacity <= 0 || capacity > MAX_CAPACITY) capacity = MAX_CAPACITY;
        this.capacity = capacity;
        this.table = new Node[capacity];
        this.size = 0;
        this.collisionsTotal = 0;
    }

    protected abstract int hash(String key);

    public final void insert(String key) {
        Objects.requireNonNull(key, "key");
        int idx = hash(key);
        Node head = table[idx];
        // Count collision if bucket already has at least one element
        if (head != null) {
            // check duplicate while walking
            Node cur = head;
            while (cur != null) {
                if (cur.key.equals(key)) return; // no duplicates
                cur = cur.next;
            }
            collisionsTotal++;
        }
        // prepend
        Node nn = new Node(key);
        nn.next = head;
        table[idx] = nn;
        size++;
    }

    public final boolean contains(String key) {
        Objects.requireNonNull(key, "key");
        int idx = hash(key);
        Node cur = table[idx];
        while (cur != null) {
            if (cur.key.equals(key)) return true;
            cur = cur.next;
        }
        return false;
    }

    public final int size() { return size; }
    public final int capacity() { return capacity; }
    public final long collisionsTotal() { return collisionsTotal; }

    /** Distribution: how many keys per position (bucket). */
    public final int[] distribution() {
        int[] dist = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            int c = 0;
            Node cur = table[i];
            while (cur != null) { c++; cur = cur.next; }
            dist[i] = c;
        }
        return dist;
    }

    /** Collisions per bucket defined as max(0, chainLength-1). */
    public final int[] collisionsPerBucket() {
        int[] dist = distribution();
        int[] col = new int[capacity];
        for (int i = 0; i < capacity; i++) col[i] = Math.max(0, dist[i] - 1);
        return col;
    }

    public final int maxChainLength() {
        int max = 0;
        for (int i = 0; i < capacity; i++) {
            int len = 0;
            Node cur = table[i];
            while (cur != null) { len++; cur = cur.next; }
            if (len > max) max = len;
        }
        return max;
    }

    public final int nonEmptyBuckets() {
        int c = 0;
        for (int i = 0; i < capacity; i++) if (table[i] != null) c++;
        return c;
    }

    public final double avgChainLenNonEmpty() {
        int neb = nonEmptyBuckets();
        return neb == 0 ? 0.0 : (double) size / neb;
    }
}
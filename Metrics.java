final class Metrics {
    final String title;
    final long insertTimeNs;
    final long searchTimeNs;
    final long collisionsTotal;
    final int[] distribution;
    final int[] collisionsPerBucket;
    final int size;
    final int capacity;
    final int maxChain;
    final int nonEmptyBuckets;
    final double avgChainNonEmpty;

    Metrics(String title,
            long insertTimeNs, long searchTimeNs, long collisionsTotal,
            int[] distribution, int[] collisionsPerBucket,
            int size, int capacity, int maxChain, int nonEmptyBuckets, double avgChainNonEmpty) {
        this.title = title;
        this.insertTimeNs = insertTimeNs;
        this.searchTimeNs = searchTimeNs;
        this.collisionsTotal = collisionsTotal;
        this.distribution = distribution;
        this.collisionsPerBucket = collisionsPerBucket;
        this.size = size;
        this.capacity = capacity;
        this.maxChain = maxChain;
        this.nonEmptyBuckets = nonEmptyBuckets;
        this.avgChainNonEmpty = avgChainNonEmpty;
    }

    String asText() {
        StringBuilder sb = new StringBuilder();
        sb.append("==== ").append(title).append(" ====\n");
        sb.append("Tamanho (size): ").append(size).append("\n");
        sb.append("Capacidade: ").append(capacity).append("\n");
        sb.append("Colisões totais (inserção): ").append(collisionsTotal).append("\n");
        sb.append("Tempo de inserção (ns): ").append(insertTimeNs).append("\n");
        sb.append("Tempo de busca (ns): ").append(searchTimeNs).append("\n");
        sb.append("Buckets não-vazios: ").append(nonEmptyBuckets).append("\n");
        sb.append("Tamanho máximo de cadeia: ").append(maxChain).append("\n");
        sb.append("Tamanho médio das cadeias (apenas buckets não-vazios): ")
          .append(String.format(java.util.Locale.ROOT, "%.3f", avgChainNonEmpty)).append("\n");
        sb.append("\nDistribuição (#chaves por posição):\n");
        for (int i = 0; i < distribution.length; i++) {
            sb.append(String.format("%02d", i)).append(": ").append(distribution[i]).append("\n");
        }
        sb.append("\nColisões por posição (chainLen-1, mínimo 0):\n");
        for (int i = 0; i < collisionsPerBucket.length; i++) {
            sb.append(String.format("%02d", i)).append(": ").append(collisionsPerBucket[i]).append("\n");
        }
        return sb.toString();
    }
}
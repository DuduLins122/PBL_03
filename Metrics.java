final class Metrics {
    final String titulo;
    final long tempoInsercaoNs;
    final long tempoBuscaNs;
    final long colisoesTotais;
    final int[] distribuicao;
    final int[] colisoesPorPosicao;
    final int tamanho;
    final int capacidade;
    final int cadeiaMaxima;
    final int baldesNaoVazios;
    final double mediaCadeiaNaoVazia;

    Metrics(String titulo,
            long tempoInsercaoNs, long tempoBuscaNs, long colisoesTotais,
            int[] distribuicao, int[] colisoesPorPosicao,
            int tamanho, int capacidade, int cadeiaMaxima, int baldesNaoVazios, double mediaCadeiaNaoVazia) {
        this.titulo = titulo;
        this.tempoInsercaoNs = tempoInsercaoNs;
        this.tempoBuscaNs = tempoBuscaNs;
        this.colisoesTotais = colisoesTotais;
        this.distribuicao = distribuicao;
        this.colisoesPorPosicao = colisoesPorPosicao;
        this.tamanho = tamanho;
        this.capacidade = capacidade;
        this.cadeiaMaxima = cadeiaMaxima;
        this.baldesNaoVazios = baldesNaoVazios;
        this.mediaCadeiaNaoVazia = mediaCadeiaNaoVazia;
    }

    // monta todo o texto na “mão” com concatenação
    String asText() {
        String s = "";
        s = s + "==== " + titulo + " ====\n";
        s = s + "Tamanho (size): " + tamanho + "\n";
        s = s + "Capacidade: " + capacidade + "\n";
        s = s + "Colisoes totais (insercao): " + colisoesTotais + "\n";
        s = s + "Tempo de insercao (ns): " + tempoInsercaoNs + "\n";
        s = s + "Tempo de busca (ns): " + tempoBuscaNs + "\n";
        s = s + "Buckets nao-vazios: " + baldesNaoVazios + "\n";
        s = s + "Tamanho maximo de cadeia: " + cadeiaMaxima + "\n";
        s = s + "Tamanho medio das cadeias (apenas buckets nao-vazios): " + formatarDecimal(mediaCadeiaNaoVazia, 3) + "\n";

        s = s + "\nDistribuicao (#chaves por posicao):\n";
        for (int i = 0; i < distribuicao.length; i++) {
            s = s + doisDigitos(i) + ": " + distribuicao[i] + "\n";
        }

        s = s + "\nColisoes por posicao (tamanhoDaCadeia-1, minimo 0):\n";
        for (int i = 0; i < colisoesPorPosicao.length; i++) {
            s = s + doisDigitos(i) + ": " + colisoesPorPosicao[i] + "\n";
        }
        return s;
    }

    // Formata um double com N casas decimais sem usar String.format/Locale
    private String formatarDecimal(double valor, int casas) {
        // arredonda simples: multiplica, arredonda, divide
        double mult = pot10(casas);
        long inteiro = (long) (valor * mult + 0.5d);
        long parteInt = inteiro / (long) mult;
        long parteDec = inteiro % (long) mult;

        String s = "" + parteInt;
        s = s + ".";
        String dec = "" + parteDec;
        // completa com zeros à esquerda
        while (dec.length() < casas) dec = "0" + dec;
        s = s + dec;
        return s;
    }

    private long pot10(int n) {
        long r = 1;
        for (int i = 0; i < n; i++) r = r * 10L;
        return r;
    }

    private String doisDigitos(int v) {
        if (v < 10) return "0" + v;
        return "" + v;
    }
}

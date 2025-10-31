import java.io.*;
import java.nio.charset.StandardCharsets;

public class Main {
    private static final int CAPACIDADE = 32; // regra: máx. 32

    public static void main(String[] args) throws Exception {
        // ===== Descoberta automática do caminho do arquivo =====
        String caminho;
        if (args.length > 0) {
            caminho = args[0];
        } else {
            File dentroDoSrc = new File("src/female_names.txt");
            File naRaiz = new File("female_names.txt");
            if (dentroDoSrc.exists()) {
                caminho = dentroDoSrc.getPath();
            } else if (naRaiz.exists()) {
                caminho = naRaiz.getPath();
            } else {
                System.out.println("Uso: java Main <caminho/para/female_names.txt>");
                System.out.println("Tentativas automáticas falharam:");
                System.out.println(" - " + dentroDoSrc.getAbsolutePath());
                System.out.println(" - " + naRaiz.getAbsolutePath());
                return;
            }
        }

        String[] nomes = carregarNomes(caminho);
        if (nomes.length == 0) {
            System.out.println("Arquivo vazio ou não encontrado: " + new File(caminho).getAbsolutePath());
            return;
        }

        // ===== Tabela 1 =====
        AbstractHashTable tabela1 = new HashTable1(CAPACIDADE);

        long t0 = System.nanoTime();
        for (int i = 0; i < nomes.length; i++) {
            tabela1.insert(nomes[i]);
        }
        long t1 = System.nanoTime();
        long tempoInsercao1 = t1 - t0;
        long tempoBusca1 = medirBusca(tabela1, nomes);

        Metrics m1 = new Metrics(
                "Tabela Hash 1 (FNV-1a-like)",
                tempoInsercao1, tempoBusca1, tabela1.collisionsTotal(),
                tabela1.distribution(), tabela1.collisionsPerBucket(),
                tabela1.size(), tabela1.capacity(), tabela1.maxChainLength(),
                tabela1.nonEmptyBuckets(), tabela1.avgChainLenNonEmpty()
        );

        // ===== Tabela 2 =====
        AbstractHashTable tabela2 = new HashTable2(CAPACIDADE);

        t0 = System.nanoTime();
        for (int i = 0; i < nomes.length; i++) {
            tabela2.insert(nomes[i]);
        }
        t1 = System.nanoTime();
        long tempoInsercao2 = t1 - t0;
        long tempoBusca2 = medirBusca(tabela2, nomes);

        Metrics m2 = new Metrics(
                "Tabela Hash 2 (djb2-like)",
                tempoInsercao2, tempoBusca2, tabela2.collisionsTotal(),
                tabela2.distribution(), tabela2.collisionsPerBucket(),
                tabela2.size(), tabela2.capacity(), tabela2.maxChainLength(),
                tabela2.nonEmptyBuckets(), tabela2.avgChainLenNonEmpty()
        );

        // ===== Console (exigido) =====
        imprimirRelatorioConsole(m1, "1");
        imprimirRelatorioConsole(m2, "2");

        // ===== PDF (mesmo conteúdo do console) =====
        PdfReportWriter.writeReport(
                "relatorio_hash.pdf",
                "Relatório – TDE 03 – Tabelas Hash",
                new String[]{
                        "==== RESULTADOS – CONSOLE (mesmo conteúdo aqui) ====",
                        m1.asText(),
                        "",
                        m2.asText()
                }
        );

        System.out.println();
        String caminhoCompleto = new java.io.File("relatorio_hash.pdf").getAbsolutePath();
        System.out.println("PDF gerado em: " + caminhoCompleto);
    }

    private static void imprimirRelatorioConsole(Metrics m, String etiqueta) {
        System.out.println("====================================================");
        System.out.println(m.asText());
        System.out.println();
        System.out.println("-- DISTRIBUICAO POR POSICAO (Tabela " + etiqueta + ") --");
        imprimirArray("Chaves por posicao", m.distribuicao);
        imprimirArray("Colisoes por posicao", m.colisoesPorPosicao);
        System.out.println("====================================================");
    }

    private static void imprimirArray(String titulo, int[] vetor) {
        System.out.println(titulo + " (cap=" + vetor.length + "):");
        // imprime como 0:x | 1:y | ...
        String linha = "";
        for (int i = 0; i < vetor.length; i++) {
            String pedaco = i + ":" + vetor[i];
            if (i == 0) linha = pedaco;
            else linha = linha + " | " + pedaco;
        }
        System.out.println(linha);
    }

    private static long medirBusca(AbstractHashTable tabela, String[] nomes) {
        int qtdPresentes = 500;
        if (nomes.length < qtdPresentes) qtdPresentes = nomes.length;

        long inicio = System.nanoTime();
        // buscas presentes (amostragem com stride fixo)
        for (int i = 0; i < qtdPresentes; i++) {
            int idx = (i * 7) % nomes.length;
            tabela.contains(nomes[idx]);
        }
        // buscas ausentes
        for (int i = 0; i < 500; i++) {
            tabela.contains("__NAO_EXISTE__" + i);
        }
        long fim = System.nanoTime();
        return fim - inicio;
    }

    private static String[] carregarNomes(String caminho) throws IOException {
        int linhas = contarLinhasNaoVazias(caminho);
        String[] saida = new String[linhas];
        InputStream in = new FileInputStream(caminho);
        InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        try {
            String linha;
            int i = 0;
            while ((linha = br.readLine()) != null) {
                String limpo = trim(linha);
                if (limpo.length() == 0) continue;
                saida[i] = limpo;
                i = i + 1;
            }
        } finally {
            br.close();
        }
        return saida;
    }

    private static int contarLinhasNaoVazias(String caminho) throws IOException {
        InputStream in = new FileInputStream(caminho);
        InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        try {
            int c = 0;
            String linha;
            while ((linha = br.readLine()) != null) {
                String limpo = trim(linha);
                if (limpo.length() == 0) continue;
                c = c + 1;
            }
            return c;
        } finally {
            br.close();
        }
    }

    // trim manual (evita dependência de lib externa e mantém “modo hardcore”)
    private static String trim(String s) {
        int ini = 0;
        int fim = s.length() - 1;
        while (ini <= fim && ehEspaco(s.charAt(ini))) ini = ini + 1;
        while (fim >= ini && ehEspaco(s.charAt(fim))) fim = fim - 1;
        if (ini > fim) return "";
        return s.substring(ini, fim + 1);
    }

    private static boolean ehEspaco(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}

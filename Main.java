
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public class Main {
    private static final int CAPACITY = 32;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        if (args.length == 0) {
            System.out.println("Uso: java Main <caminho/para/female_names.txt>");
            return;
        }
        String path = args[0];
        String[] names = loadNames(path);
        if (names.length == 0) {
            System.out.println("Arquivo vazio ou não encontrado.");
            return;
        }

        // Tabela 1
        AbstractHashTable h1 = new HashTable1(CAPACITY);
        long t0 = System.nanoTime();
        for (String s : names) h1.insert(s);
        long t1 = System.nanoTime();
        long insert1 = t1 - t0;
        long search1 = measureSearch(h1, names);

        Metrics m1 = new Metrics(
                "Tabela Hash 1 (FNV-1a-like)",
                insert1, search1, h1.collisionsTotal(),
                h1.distribution(), h1.collisionsPerBucket(),
                h1.size(), h1.capacity(), h1.maxChainLength(), h1.nonEmptyBuckets(), h1.avgChainLenNonEmpty()
        );

        // Tabela 2
        AbstractHashTable h2 = new HashTable2(CAPACITY);
        t0 = System.nanoTime();
        for (String s : names) h2.insert(s);
        t1 = System.nanoTime();
        long insert2 = t1 - t0;
        long search2 = measureSearch(h2, names);

        Metrics m2 = new Metrics(
                "Tabela Hash 2 (djb2-like)",
                insert2, search2, h2.collisionsTotal(),
                h2.distribution(), h2.collisionsPerBucket(),
                h2.size(), h2.capacity(), h2.maxChainLength(), h2.nonEmptyBuckets(), h2.avgChainLenNonEmpty()
        );

        // Console report (EXIGIDO): tempos, colisões, distribuição por bucket
        printConsoleReport(m1, m2);

        // PDF (DIFERENCIAL): mesmo conteúdo do console
        PdfReportWriter.write("relatorio_hash.pdf", "Relatório – TDE 03 – Tabelas Hash",
                Arrays.asList(
                        "==== RESULTADOS – CONSOLE (mesmo conteúdo aqui) ====",
                        m1.toString(),
                        "",
                        m2.toString()
                )
        );

        System.out.println("\nPDF gerado: relatorio_hash.pdf");
    }

    private static void printConsoleReport(Metrics m1, Metrics m2) {
        System.out.println("====================================================");
        System.out.println(m1.toString());
        System.out.println("\n-- DISTRIBUIÇÃO POR BUCKET (Tabela 1) --");
        printArray("Chaves por posição", m1.distribution);
        printArray("Colisões por posição", m1.collisionsPerBucket);

        System.out.println("\n====================================================");
        System.out.println(m2.toString());
        System.out.println("\n-- DISTRIBUIÇÃO POR BUCKET (Tabela 2) --");
        printArray("Chaves por posição", m2.distribution);
        printArray("Colisões por posição", m2.collisionsPerBucket);
        System.out.println("====================================================");
    }

    private static void printArray(String title, int[] arr) {
        System.out.println(title + " (cap=" + arr.length + "):");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(i).append(":").append(arr[i]);
            if (i < arr.length - 1) sb.append(" | ");
        }
        System.out.println(sb.toString());
    }

    private static long measureSearch(AbstractHashTable h, String[] names) {
        // 500 presentes + 500 ausentes
        int hits = Math.min(500, names.length);
        long t0 = System.nanoTime();
        for (int i = 0; i < hits; i++) {
            h.contains(names[(i * 7) % names.length]); // stride p/ amostrar bem
        }
        for (int i = 0; i < 500; i++) {
            h.contains("___NAO_EXISTE___" + i);
        }
        long t1 = System.nanoTime();
        return t1 - t0;
    }

    private static String[] loadNames(String path) throws IOException {
        int count = countLines(path);
        String[] out = new String[count];
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                out[i++] = line;
            }
        }
        return out;
    }

    private static int countLines(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            int c = 0;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() == 0) continue;
                c++;
            }
            return c;
        }
    }
}

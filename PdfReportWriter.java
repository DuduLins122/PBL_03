import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PdfReportWriter {

    private PdfReportWriter() {}

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int LEFT = 25;
    private static final int RIGHT = 72;
    private static final int TOP_Y = 800;
    private static final int BOTTOM = 72;

    private static final String BASE_FONT = "/Courier";
    private static final int FONT_SIZE_TITLE = 14;
    private static final int FONT_SIZE_TEXT  = 10;
    private static final int LEADING = 13;

    private static final int COLS_WRAP = 80;

    public static void writeReport(String path, String title, String[] lines) throws IOException {
        List<String> all = new ArrayList<>();
        all.add(title == null ? "" : title);
        all.add("");

        for (String blk : lines) {
            if (blk == null) continue;
            String[] split = blk.replace("\r", "").split("\n");
            all.addAll(Arrays.asList(split));
        }

        List<String> formatted = formatSections(all);

        int linesPerPage = Math.max(10, (TOP_Y - BOTTOM) / LEADING);
        String[] wrapped = wrapLines(formatted.toArray(new String[0]), COLS_WRAP);
        List<List<String>> pages = paginate(Arrays.asList(wrapped), linesPerPage);

        PDF pdf = new PDF();
        int idCatalog = pdf.newId();
        int idPages = pdf.newId();
        int idFont = pdf.newId();

        int n = pages.size();
        int[] idPage = new int[n];
        int[] idStream = new int[n];

        for (int i = 0; i < n; i++) {
            byte[] content = buildPageStream(pages.get(i), i + 1, n);
            idStream[i] = pdf.newId();
            pdf.obj(idStream[i], "<< /Length " + content.length + " >>\nstream\n", content, "\nendstream\n");

            idPage[i] = pdf.newId();
            pdf.obj(idPage[i],
                    "<< /Type /Page /Parent " + idPages + " 0 R " +
                    "/MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "] " +
                    "/Resources << /Font << /F1 " + idFont + " 0 R >> >> " +
                    "/Contents " + idStream[i] + " 0 R >>\n");
        }

        pdf.obj(idFont, "<< /Type /Font /Subtype /Type1 /BaseFont " + BASE_FONT + " /Encoding /WinAnsiEncoding >>\n");

        StringBuilder kids = new StringBuilder("[");
        for (int i = 0; i < n; i++) kids.append(" ").append(idPage[i]).append(" 0 R");
        kids.append(" ]");
        pdf.obj(idPages, "<< /Type /Pages /Count " + n + " /Kids " + kids + " >>\n");

        pdf.obj(idCatalog, "<< /Type /Catalog /Pages " + idPages + " 0 R >>\n");

        byte[] bytes = pdf.finish();
        try (OutputStream out = new FileOutputStream(path)) {
            out.write(bytes);
        }
    }

    private static List<String> formatSections(List<String> in) {
        List<String> out = new ArrayList<>();
        boolean dentroTabela = false;

        for (int i = 0; i < in.size(); i++) {
            String s = in.get(i);
            if (s == null) s = "";
            String t = s.trim();

            if (t.startsWith("==== Tabela Hash 1")) {
                out.add("");
                out.add("TABELA HASH 1");
                out.add("");
                out.add("MÉTRICAS PRINCIPAIS");
                dentroTabela = true;
                continue;
            }
            if (t.startsWith("==== Tabela Hash 2")) {
                out.add("");
                out.add("TABELA HASH 2");
                out.add("");
                out.add("MÉTRICAS PRINCIPAIS");
                dentroTabela = true;
                continue;
            }

            if (t.startsWith("Distribuicao (#chaves por posicao):")) {
                out.add("");
                out.add("DISTRIBUIÇÃO DE CHAVES");
                continue;
            }
            if (t.startsWith("Colisoes por posicao (tamanhoDaCadeia-1, minimo 0):")) {
                out.add("");
                out.add("COLISÕES POR POSIÇÃO");
                continue;
            }

            if (t.startsWith("====") && !t.startsWith("==== Tabela")) {
                continue;
            }

            if (dentroTabela && t.length() == 0) {
                out.add(""); 
                continue;
            }

            out.add(s);
        }
        return out;
    }

    private static byte[] buildPageStream(List<String> lines, int pageNum, int totalPages) {
        StringBuilder sb = new StringBuilder(8192);

        sb.append("BT\n");
        sb.append("/F1 ").append(FONT_SIZE_TEXT).append(" Tf\n");
        sb.append(LEADING).append(" TL\n");
        sb.append(LEFT).append(" ").append(TOP_Y).append(" Td\n");

        if (!lines.isEmpty()) {
            String first = lines.get(0) == null ? "" : lines.get(0);
            sb.append("/F1 ").append(FONT_SIZE_TITLE).append(" Tf\n");
            int usableWidth = PAGE_WIDTH - LEFT - RIGHT;
            int colsPerLine = (int)Math.floor((double)usableWidth / (FONT_SIZE_TEXT * 0.6));
            if (colsPerLine <= 0) colsPerLine = COLS_WRAP;
            int centerCol = colsPerLine / 2;
            int titleLen = first.length();
            int titleCol = Math.max(0, centerCol - titleLen / 2);
            int shiftX = (int)Math.round(titleCol * (FONT_SIZE_TEXT * 0.6));
            sb.append(shiftX).append(" 0 Td\n");
            appendTj(sb, first);
            sb.append("T*\nT*\n");
            sb.append(-shiftX).append(" 0 Td\n");
            sb.append("/F1 ").append(FONT_SIZE_TEXT).append(" Tf\n");

            lines = new ArrayList<>(lines.subList(1, lines.size()));
        }

        for (String s : lines) {
            if (s == null || s.isEmpty()) sb.append("T*\n");
            else { appendTj(sb, s); sb.append("T*\n"); }
        }
        sb.append("ET\n");

        String footer = "Pagina " + pageNum + "/" + totalPages;
        sb.append("BT\n");
        sb.append("/F1 ").append(FONT_SIZE_TEXT).append(" Tf\n");
        int footerY = BOTTOM - 20; if (footerY < 20) footerY = 20;
        int footerX = PAGE_WIDTH - RIGHT - (int)Math.round(footer.length() * (FONT_SIZE_TEXT * 0.6));
        if (footerX < LEFT) footerX = LEFT;
        sb.append(footerX).append(" ").append(footerY).append(" Td\n");
        appendTj(sb, footer);
        sb.append("ET\n");

        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static void appendTj(StringBuilder sb, String s) {
        sb.append('(').append(escapePdfText(s)).append(") Tj\n");
    }

    private static String escapePdfText(String s) {
        if (s == null) return "";
        StringBuilder r = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == ')' || c == '\\') r.append('\\').append(c);
            else r.append(c);
        }
        return r.toString();
    }

    private static String[] wrapLines(String[] input, int maxCols) {
        List<String> out = new ArrayList<>();
        if (input == null) return new String[] { "" };
        for (String raw : input) {
            String s = raw == null ? "" : raw;
            if (s.isEmpty()) { out.add(""); continue; }
            int pos = 0;
            while (pos < s.length()) {
                int limit = Math.min(pos + maxCols, s.length());
                int cut = lastSpaceBefore(s, limit);
                if (cut <= pos) cut = limit;
                String piece = s.substring(pos, cut).trim();
                out.add(piece);
                pos = cut;
            }
        }
        return out.toArray(new String[0]);
    }

    private static int lastSpaceBefore(String s, int limitExclusive) {
        int p = Math.min(limitExclusive, s.length());
        int space = s.lastIndexOf(' ', p - 1);
        int tab = s.lastIndexOf('\t', p - 1);
        return Math.max(space, tab);
    }

    private static List<List<String>> paginate(List<String> all, int linesPerPage) {
        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < all.size(); i += linesPerPage) {
            pages.add(new ArrayList<>(all.subList(i, Math.min(i + linesPerPage, all.size()))));
        }
        return pages;
    }

    private static final class PDF {
        private final ByteArrayOutput out = new ByteArrayOutput();
        private long[] offsets = new long[32];
        private int count = 0;

        PDF() { write("%PDF-1.4\n"); }

        int newId() {
            count++;
            if (count >= offsets.length) offsets = Arrays.copyOf(offsets, offsets.length * 2);
            return count;
        }

        void obj(int id, String body) {
            begin(id);
            write(body);
            end();
        }

        void obj(int id, String head, byte[] stream, String tail) {
            begin(id);
            write(head);
            write(stream);
            write(tail);
            end();
        }

        byte[] finish() {
            long xrefStart = out.size();
            write("xref\n");
            write("0 " + (count + 1) + "\n");
            write("0000000000 65535 f \n");
            for (int i = 1; i <= count; i++) {
                pad10(offsets[i]);
                write(" 00000 n \n");
            }
            write("trailer << /Size " + (count + 1) + " /Root 1 0 R >>\n");
            write("startxref\n");
            write(Long.toString(xrefStart) + "\n");
            write("%%EOF");
            return out.toByteArray();
        }

        private void begin(int id) {
            offsets[id] = out.size();
            write(id + " 0 obj\n");
        }

        private void end() { write("endobj\n"); }

        private void write(String s) { out.write(s.getBytes(StandardCharsets.ISO_8859_1)); }

        private void write(byte[] b) { out.write(b); }

        private void pad10(long pos) {
            String p = Long.toString(pos);
            String z = "0000000000";
            String dez = z.substring(0, 10 - p.length()) + p;
            write(dez);
        }
    }

    private static final class ByteArrayOutput {
        private byte[] data = new byte[4096];
        private int len = 0;

        void write(byte[] arr) {
            ensure(len + arr.length);
            System.arraycopy(arr, 0, data, len, arr.length);
            len += arr.length;
        }

        int size() { return len; }

        byte[] toByteArray() { return Arrays.copyOf(data, len); }

        private void ensure(int need) {
            if (need <= data.length) return;
            int cap = Math.max(need, data.length * 2);
            data = Arrays.copyOf(data, cap);
        }
    }
}

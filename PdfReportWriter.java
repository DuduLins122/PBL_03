
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** Minimal PDF writer (1+ pages, Courier), WinAnsiEncoding to suport Latin-1 (acentos).
 *  *** ATENÇÃO: NÃO USA NENHUMA ESTRUTURA PRONTA DO JAVA ***
 *  Implementa um vetor dinâmico manual para armazenar objetos PDF.
 */
final class PdfReportWriter {

    // WinAnsi charset para textos no PDF (cobre acentos pt-BR de forma razoável)
    private static final Charset PDF_CHARSET = Charset.forName("windows-1252");

    // ------------------------- Utilidades internas -------------------------
    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 32);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // escapar parênteses e barra invertida dentro de literais de texto PDF
            if (c == '(' || c == ')' || c == '\\') {
                out.append('\\').append(c);
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\t') {
                out.append("\\t");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // Representa um objeto PDF (id e bytes do conteúdo do objeto)
    private static final class Obj {
        final int id;
        final byte[] data;
        Obj(int id, byte[] data) { this.id = id; this.data = data; }
    }

    /** "Vetor" dinâmico manual de Obj (sem usar ArrayList). */
    private static final class ObjBuffer {
        private Obj[] arr;
        private int size;

        ObjBuffer(int initialCapacity) {
            if (initialCapacity < 1) initialCapacity = 1;
            arr = new Obj[initialCapacity];
            size = 0;
        }

        void add(Obj o) {
            if (size == arr.length) {
                // crescer capacidade manualmente (dobrar)
                Obj[] novo = new Obj[arr.length * 2];
                // copiar manualmente
                for (int i = 0; i < arr.length; i++) novo[i] = arr[i];
                arr = novo;
            }
            arr[size++] = o;
        }

        int size() { return size; }

        Obj get(int idx) {
            return arr[idx];
        }
    }

    // ------------------------- API pública -------------------------
    /** Gera um PDF simples com título e um corpo de múltiplas linhas. */
    static void writeReport(String path, String title, String[] lines) throws IOException {
        // -------------------- Preparação de objetos --------------------
        ObjBuffer objects = new ObjBuffer(16);
        int objId = 1;

        // 1) Catalog
        final int catalogId = objId++;
        objects.add(new Obj(catalogId, str(
            "<< /Type /Catalog /Pages 2 0 R >>\n"
        )));

        // 2) Pages
        final int pagesId = objId++;
        objects.add(new Obj(pagesId, str(
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n"
        )));

        // 3) Page (A4 595x842)
        final int pageId = objId++;
        objects.add(new Obj(pageId, str(
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\n"
        )));

        // 4) Fonte (Courier com WinAnsiEncoding)
        final int fontId = objId++;
        objects.add(new Obj(fontId, str(
            "<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>\n"
        )));

        // 5) Conteúdo da página
        final int contentsId = objId++;
        ByteArrayOutputStream body = new ByteArrayOutputStream(4096);
        body.write("BT\n".getBytes(StandardCharsets.US_ASCII));
        body.write("/F1 12 Tf\n".getBytes(StandardCharsets.US_ASCII));

        // Título
        if (title == null) title = "";
        String ttl = escape(title);
        body.write(String.format("1 0 0 1 50 800 Tm (%s) Tj\n", ttl).getBytes(StandardCharsets.US_ASCII));

        // Corpo (linhas), com quebra vertical
        int y = 780;
        for (int i = 0; i < (lines == null ? 0 : lines.length); i++) {
            String line = lines[i];
            if (line == null) line = "";
            String esc = escape(line);
            body.write(String.format("1 0 0 1 50 %d Tm (%s) Tj\n", y, esc).getBytes(StandardCharsets.US_ASCII));
            y -= 14;
            if (y < 40) break; // versão simples: 1 página
        }
        body.write("ET\n".getBytes(StandardCharsets.US_ASCII));
        byte[] stream = body.toByteArray();
        ByteArrayOutputStream contents = new ByteArrayOutputStream(2048);
        contents.write(String.format("<< /Length %d >>\nstream\n", stream.length).getBytes(StandardCharsets.US_ASCII));
        contents.write(stream);
        contents.write("\nendstream\n".getBytes(StandardCharsets.US_ASCII));
        objects.add(new Obj(contentsId, contents.toByteArray()));

        // -------------------- Escrita do arquivo PDF --------------------
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        baos.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

        // tabela de offsets (xref) — armazenar posições
        long[] offsets = new long[objects.size() + 1]; // índice 0 não usado
        for (int i = 0; i < offsets.length; i++) offsets[i] = 0L;

        for (int i = 0; i < objects.size(); i++) {
            Obj o = objects.get(i);
            offsets[o.id] = baos.size();
            baos.write(String.format("%d 0 obj\n", o.id).getBytes(StandardCharsets.US_ASCII));
            baos.write(o.data);
            baos.write("endobj\n".getBytes(StandardCharsets.US_ASCII));
        }

        long xrefPos = baos.size();
        baos.write("xref\n".getBytes(StandardCharsets.US_ASCII));
        baos.write(String.format("0 %d\n", objects.size() + 1).getBytes(StandardCharsets.US_ASCII));
        // entrada 0 obrigatória
        baos.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (int i = 1; i <= objects.size(); i++) {
            long off = offsets[i];
            if (off < 0) off = 0;
            String line = String.format("%010d 00000 n \n", off);
            baos.write(line.getBytes(StandardCharsets.US_ASCII));
        }
        String trailer = "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefPos + "\n%%EOF\n";
        baos.write(trailer.getBytes(StandardCharsets.US_ASCII));

        try (FileOutputStream fos = new FileOutputStream(path)) {
            baos.writeTo(fos);
        }
    }

    private static byte[] str(String fmt, Object... args) {
        return String.format(fmt, args).getBytes(StandardCharsets.US_ASCII);
    }
}

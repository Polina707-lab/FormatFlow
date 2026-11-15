
package formatflow.ui.preview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class FilePreviewService {

    private static final int MAX_CHARACTERS = 10_000;
    private static final String TRUNCATED_MESSAGE =
            "\n\n... Preview truncated ...";

    public String readPreview(Path path) throws IOException {

        if (!Files.isRegularFile(path)) {
            throw new IOException("File does not exist.");
        }

        String filename = path.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        if (filename.endsWith(".xml")) {
            return readXmlPreview(path);
        }

        return readTextPreview(path);
    }

    private String readTextPreview(Path path) throws IOException {

        StringBuilder preview = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8)) {

            char[] buffer = new char[1024];

            while (preview.length() <= MAX_CHARACTERS) {

                int remaining =
                        MAX_CHARACTERS + 1 - preview.length();

                int read = reader.read(
                        buffer,
                        0,
                        Math.min(buffer.length, remaining)
                );

                if (read == -1) {
                    break;
                }

                preview.append(buffer, 0, read);
            }
        }

        if (preview.length() > MAX_CHARACTERS) {
            preview.setLength(MAX_CHARACTERS);
            preview.append(TRUNCATED_MESSAGE);
        }

        return preview.toString();
    }

    private String readXmlPreview(Path path) throws IOException {

        XMLStreamReader reader = null;

        try (InputStream input = Files.newInputStream(path)) {

            XMLInputFactory factory =
                    XMLInputFactory.newInstance();

            // Запрещаем DTD и внешние сущности.
            factory.setProperty(
                    XMLInputFactory.SUPPORT_DTD, false
            );

            factory.setProperty(
                    XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                    false
            );

            reader = factory.createXMLStreamReader(input);

            StringBuilder preview = new StringBuilder();

            int depth = 0;
            boolean previousWasEnd = false;
            boolean hasText = false;

            while (reader.hasNext()
                    && preview.length() < MAX_CHARACTERS) {

                int event = reader.next();

                switch (event) {

                    case XMLStreamConstants.START_ELEMENT -> {

                        if (depth > 0) {
                            preview.append("\n");
                        }

                        appendIndent(preview, depth);

                        preview.append("<")
                                .append(reader.getLocalName());

                        for (int i = 0;
                             i < reader.getAttributeCount();
                             i++) {

                            preview.append(" ")
                                    .append(
                                            reader.getAttributeLocalName(i)
                                    )
                                    .append("=\"")
                                    .append(
                                            escapeXml(
                                                    reader.getAttributeValue(i)
                                            )
                                    )
                                    .append("\"");
                        }

                        preview.append(">");

                        depth++;
                        previousWasEnd = false;
                        hasText = false;
                    }

                    case XMLStreamConstants.CHARACTERS,
                         XMLStreamConstants.CDATA -> {

                        String text = reader.getText();

                        if (!text.isBlank()) {
                            preview.append(escapeXml(text));
                            hasText = true;
                        }
                    }

                    case XMLStreamConstants.END_ELEMENT -> {

                        depth--;

                        if (previousWasEnd && !hasText) {
                            preview.append("\n");
                            appendIndent(preview, depth);
                        }

                        preview.append("</")
                                .append(reader.getLocalName())
                                .append(">");

                        previousWasEnd = true;
                        hasText = false;
                    }

                    default -> {
                        // Остальные события не отображаем.
                    }
                }
            }

            boolean truncated = reader.hasNext();

            if (preview.length() > MAX_CHARACTERS) {
                preview.setLength(MAX_CHARACTERS);
                truncated = true;
            }

            if (truncated) {
                preview.append(TRUNCATED_MESSAGE);
            }

            return preview.toString();

        } catch (XMLStreamException e) {
            throw new IOException(
                    "Failed to read XML preview: "
                            + e.getMessage(),
                    e
            );

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException ignored) {
                }
            }
        }
    }

    private void appendIndent(
            StringBuilder builder,
            int depth
    ) {
        builder.append("  ".repeat(Math.max(0, depth)));
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }
}

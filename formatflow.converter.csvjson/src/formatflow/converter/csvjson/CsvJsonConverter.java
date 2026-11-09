
package formatflow.converter.csvjson;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import formatflow.api.FileConverter;

public class CsvJsonConverter implements FileConverter {

    @Override
    public String getSourceFormat() {
        return "csv";
    }

    @Override
    public String getTargetFormat() {
        return "json";
    }

    @Override
    public void convert(File source, File target) throws IOException {

        List<String> lines = Files.readAllLines(
                source.toPath(),
                StandardCharsets.UTF_8
        );

        if (lines.isEmpty()) {
            throw new IOException("CSV-файл пустой");
        }

        // Читаем заголовки
        String[] headers = parseCsvLine(lines.get(0), 1);

        List<String> objects = new ArrayList<>();

        // Читаем строки данных
        for (int i = 1; i < lines.size(); i++) {

            String[] values = parseCsvLine(
                    lines.get(i), i + 1
            );

            if (values.length != headers.length) {
                throw new IOException(
                        "Некорректное число столбцов в строке " + (i + 1)
                );
            }

            // Формируем JSON-объект
            List<String> fields = new ArrayList<>();

            for (int j = 0; j < headers.length; j++) {
                fields.add(
                        "\"" + escapeJson(headers[j]) + "\": "
                        + "\"" + escapeJson(values[j]) + "\""
                );
            }

            objects.add(
                    "  {\n    "
                    + String.join(",\n    ", fields)
                    + "\n  }"
            );
        }

        String json = "[\n"
                + String.join(",\n", objects)
                + "\n]";

        Files.writeString(
                target.toPath(),
                json,
                StandardCharsets.UTF_8
        );
    }

    private String[] parseCsvLine(
            String line,
            int lineNumber
    ) throws IOException {

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inQuotes = false;
        boolean afterQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char ch = line.charAt(i);

            if (inQuotes) {

                if (ch == '"') {

                    if (i + 1 < line.length()
                            && line.charAt(i + 1) == '"') {

                        current.append('"');
                        i++;

                    } else {
                        inQuotes = false;
                        afterQuotes = true;
                    }

                } else {
                    current.append(ch);
                }

            } else if (afterQuotes) {

                if (ch == ',') {
                    values.add(current.toString());
                    current.setLength(0);
                    afterQuotes = false;
                } else {
                    throw new IOException(
                            "Некорректный CSV в строке " + lineNumber
                    );
                }

            } else {

                if (ch == ',') {

                    values.add(current.toString());
                    current.setLength(0);

                } else if (ch == '"' && current.length() == 0) {

                    inQuotes = true;

                } else if (ch == '"') {

                    throw new IOException(
                            "Некорректные кавычки в строке " + lineNumber
                    );

                } else {

                    current.append(ch);
                }
            }
        }

        if (inQuotes) {
            throw new IOException(
                    "Незакрытые кавычки в строке " + lineNumber
            );
        }

        values.add(current.toString());

        return values.toArray(new String[0]);
    }

    private String escapeJson(String value) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {

            char ch = value.charAt(i);

            switch (ch) {
                case '\\' -> result.append("\\\\");
                case '"' -> result.append("\\\"");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        result.append(
                                String.format("\\u%04x", (int) ch)
                        );
                    } else {
                        result.append(ch);
                    }
                }
            }
        }

        return result.toString();
    }
}

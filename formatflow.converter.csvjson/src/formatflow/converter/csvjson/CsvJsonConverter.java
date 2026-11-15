
package formatflow.converter.csvjson;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import com.google.gson.stream.JsonWriter;

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

    try (BufferedReader reader = Files.newBufferedReader(
            source.toPath(),
            StandardCharsets.UTF_8
    )) {

        String headerLine = reader.readLine();

        if (headerLine == null) {
            throw new IOException("CSV-файл пустой");
        }

        String[] headers = parseCsvLine(headerLine, 1);

        try (BufferedWriter writer = Files.newBufferedWriter(
                target.toPath(),
                StandardCharsets.UTF_8
        );
             JsonWriter jsonWriter = new JsonWriter(writer)) {

            jsonWriter.setIndent("  ");
            jsonWriter.beginArray();

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {

                lineNumber++;

                String[] values = parseCsvLine(line, lineNumber);

                if (values.length != headers.length) {
                    throw new IOException(
                            "Некорректное число столбцов в строке "
                                    + lineNumber
                    );
                }

                jsonWriter.beginObject();

                for (int i = 0; i < headers.length; i++) {
                    jsonWriter.name(headers[i]);
                    jsonWriter.value(values[i]);
                }

                jsonWriter.endObject();
            }

            jsonWriter.endArray();
        }
    }
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

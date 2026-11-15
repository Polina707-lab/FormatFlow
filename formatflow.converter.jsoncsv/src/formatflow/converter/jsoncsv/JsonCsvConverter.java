
package formatflow.converter.jsoncsv;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import formatflow.api.FileConverter;

public class JsonCsvConverter implements FileConverter {

    @Override
    public String getSourceFormat() {
        return "json";
    }

    @Override
    public String getTargetFormat() {
        return "csv";
    }

    @Override
    public void convert(File source, File target) throws IOException {

        // Первый проход: определяем все названия столбцов.
        Set<String> headers = new LinkedHashSet<>();

        try (Reader reader = Files.newBufferedReader(
                source.toPath(), StandardCharsets.UTF_8);
             JsonReader jsonReader = new JsonReader(reader)) {

            jsonReader.setStrictness(
                    com.google.gson.Strictness.STRICT
            );

            readObjects(jsonReader, headers, null);

        }

        List<String> headerList = new ArrayList<>(headers);

        // Второй проход: записываем CSV.
        try (Reader reader = Files.newBufferedReader(
                source.toPath(), StandardCharsets.UTF_8);
             JsonReader jsonReader = new JsonReader(reader);
             Writer writer = Files.newBufferedWriter(
                     target.toPath(), StandardCharsets.UTF_8)) {

            jsonReader.setStrictness(
                    com.google.gson.Strictness.STRICT
            );

            writeCsvRow(writer, headerList);

            readObjects(jsonReader, null, new CsvRowWriter() {

                @Override
                public void write(
                        java.util.Map<String, String> object
                ) throws IOException {

                    List<String> values = new ArrayList<>();

                    for (String header : headerList) {
                        values.add(object.getOrDefault(header, ""));
                    }

                    writeCsvRow(writer, values);
                }
            });
        }
    }

    @FunctionalInterface
    private interface CsvRowWriter {
        void write(java.util.Map<String, String> object)
                throws IOException;
    }

    private void readObjects(
            JsonReader reader,
            Set<String> headers,
            CsvRowWriter rowWriter
    ) throws IOException {

        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            throw new IOException("JSON root must be an array");
        }

        reader.beginArray();

        while (reader.hasNext()) {

            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                throw new IOException(
                        "Every array element must be an object"
                );
            }

            java.util.Map<String, String> object =
                    new java.util.LinkedHashMap<>();

            reader.beginObject();

            while (reader.hasNext()) {

                String name = reader.nextName();

                JsonToken token = reader.peek();

                String value;

                switch (token) {
                    case STRING, NUMBER -> value = reader.nextString();
                    case BOOLEAN -> value =
                            String.valueOf(reader.nextBoolean());
                    case NULL -> {
                        reader.nextNull();
                        value = "";
                    }
                    default -> throw new IOException(
                            "Nested JSON values are not supported"
                    );
                }

                if (headers != null) {
                    headers.add(name);
                }

                if (rowWriter != null) {
                    object.put(name, value);
                }
            }

            reader.endObject();

            if (rowWriter != null) {
                rowWriter.write(object);
            }
        }

        reader.endArray();

        if (reader.peek() != JsonToken.END_DOCUMENT) {
            throw new IOException(
                    "Unexpected content after JSON array"
            );
        }
    }

    private void writeCsvRow(
            Writer writer,
            List<String> values
    ) throws IOException {

        for (int i = 0; i < values.size(); i++) {

            if (i > 0) {
                writer.write(",");
            }

            writer.write(escapeCsv(values.get(i)));
        }

        writer.write(System.lineSeparator());
    }

    private String escapeCsv(String value) {

        if (value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r")) {

            return "\""
                    + value.replace("\"", "\"\"")
                    + "\"";
        }

        return value;
    }
}

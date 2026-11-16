
package formatflow.converter.jsoncsv;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

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

            readObjects(jsonReader, null, object -> {

                List<String> values = new ArrayList<>();

                for (String header : headerList) {
                    values.add(object.getOrDefault(header, ""));
                }

                writeCsvRow(writer, values);
            });
        }
    }

    @FunctionalInterface
    private interface CsvRowWriter {
        void write(Map<String, String> object)
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

            Map<String, String> object = new LinkedHashMap<>();

            readObject(reader, "", object);

            if (headers != null) {
                headers.addAll(object.keySet());
            }

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

    private void readObject(
            JsonReader reader,
            String prefix,
            Map<String, String> object
    ) throws IOException {

        reader.beginObject();

        while (reader.hasNext()) {

            String name = reader.nextName();

            String key = prefix.isEmpty()
                    ? name
                    : prefix + "." + name;

            JsonToken token = reader.peek();

            switch (token) {

                case BEGIN_OBJECT ->
                        readObject(reader, key, object);

                case BEGIN_ARRAY ->
                        object.put(key, readArray(reader));

                case STRING, NUMBER ->
                        object.put(key, reader.nextString());

                case BOOLEAN ->
                        object.put(
                                key,
                                String.valueOf(reader.nextBoolean())
                        );

                case NULL -> {
                    reader.nextNull();
                    object.put(key, "");
                }

                default -> throw new IOException(
                        "Unsupported JSON value at " + reader.getPath()
                );
            }
        }

        reader.endObject();
    }

    private String readArray(JsonReader reader) throws IOException {

        StringWriter buffer = new StringWriter();

        try (JsonWriter writer = new JsonWriter(buffer)) {
            writer.setSerializeNulls(true);
            copyJsonValue(reader, writer);
        }

        return buffer.toString();
    }

    private void copyJsonValue(
            JsonReader reader,
            JsonWriter writer
    ) throws IOException {

        JsonToken token = reader.peek();

        switch (token) {

            case BEGIN_OBJECT -> {
                reader.beginObject();
                writer.beginObject();

                while (reader.hasNext()) {
                    String name = reader.nextName();
                    writer.name(name);
                    copyJsonValue(reader, writer);
                }

                reader.endObject();
                writer.endObject();
            }

            case BEGIN_ARRAY -> {
                reader.beginArray();
                writer.beginArray();

                while (reader.hasNext()) {
                    copyJsonValue(reader, writer);
                }

                reader.endArray();
                writer.endArray();
            }

            case STRING ->
                    writer.value(reader.nextString());

            case NUMBER -> {
                String number = reader.nextString();
                writer.jsonValue(number);
            }

            case BOOLEAN ->
                    writer.value(reader.nextBoolean());

            case NULL -> {
                reader.nextNull();
                writer.nullValue();
            }

            default -> throw new IOException(
                    "Unsupported JSON value at " + reader.getPath()
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

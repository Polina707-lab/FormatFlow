package formatflow.converter.csvjson;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
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
        try (Reader fileReader = Files.newBufferedReader(
                source.toPath(), StandardCharsets.UTF_8);
             PushbackReader reader = new PushbackReader(
                     new BufferedReader(fileReader), 1)) {

            String[] headers = readCsvRecord(reader, 1);
            if (headers == null) {
                throw new IOException("CSV-файл пустой");
            }
            validateHeaders(headers);

            try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(
                    target.toPath(), StandardCharsets.UTF_8))) {
                writer.setIndent("  ");
                writer.setSerializeNulls(true);
                writer.beginArray();

                int recordNumber = 1;
                String[] values;
                while ((values = readCsvRecord(reader, recordNumber + 1)) != null) {
                    recordNumber++;
                    if (values.length != headers.length) {
                        throw new IOException(
                                "Некорректное число столбцов в записи " + recordNumber);
                    }

                    Map<String, Object> object = new LinkedHashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        insertValue(object, headers[i], parseValue(values[i]), recordNumber);
                    }
                    writeObject(writer, object);
                }
                writer.endArray();
            }
        }
    }

    private void validateHeaders(String[] headers) throws IOException {
        Map<String, Object> paths = new LinkedHashMap<>();
        for (String header : headers) {
            if (header.isEmpty()) {
                throw new IOException("Пустое название столбца CSV");
            }
            insertValue(paths, header, "", 1);
        }
    }

    private Object parseValue(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("[")) {
            return value;
        }
        try (JsonReader reader = new JsonReader(new java.io.StringReader(trimmed))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement element = JsonParser.parseReader(reader);
            if (element.isJsonArray()) {
                return element;
            }
        } catch (IOException | JsonSyntaxException | IllegalStateException e) {
            // Текст, похожий на JSON, оставляем обычной строкой.
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private void insertValue(Map<String, Object> object, String header,
                             Object value, int recordNumber) throws IOException {
        String[] path = header.split("\\.", -1);
        Map<String, Object> current = object;
        for (int i = 0; i < path.length; i++) {
            String key = path[i];
            if (key.isEmpty()) {
                throw new IOException("Некорректный путь столбца: " + header);
            }
            boolean last = i == path.length - 1;
            if (last) {
                if (current.containsKey(key)) {
                    throw new IOException("Конфликт столбцов '" + header
                            + "' в записи " + recordNumber);
                }
                current.put(key, value);
            } else {
                Object next = current.get(key);
                if (next == null && !current.containsKey(key)) {
                    next = new LinkedHashMap<String, Object>();
                    current.put(key, next);
                }
                if (!(next instanceof Map<?, ?>)) {
                    throw new IOException("Конфликт вложенных полей для '" + header
                            + "' в записи " + recordNumber);
                }
                current = (Map<String, Object>) next;
            }
        }
    }

    private void writeObject(JsonWriter writer, Map<String, Object> object)
            throws IOException {
        writer.beginObject();
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            writer.name(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) value;
                writeObject(writer, nested);
            } else if (value instanceof JsonElement) {
                writeJsonElement(writer, (JsonElement) value);
            } else {
                writer.value((String) value);
            }
        }
        writer.endObject();
    }

    private void writeJsonElement(JsonWriter writer, JsonElement element)
            throws IOException {
        if (element.isJsonNull()) {
            writer.nullValue();
        } else if (element.isJsonArray()) {
            writer.beginArray();
            for (JsonElement item : element.getAsJsonArray()) {
                writeJsonElement(writer, item);
            }
            writer.endArray();
        } else if (element.isJsonObject()) {
            writer.beginObject();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                writer.name(entry.getKey());
                writeJsonElement(writer, entry.getValue());
            }
            writer.endObject();
        } else if (element.getAsJsonPrimitive().isBoolean()) {
            writer.value(element.getAsBoolean());
        } else if (element.getAsJsonPrimitive().isNumber()) {
            writer.jsonValue(element.getAsString());
        } else {
            writer.value(element.getAsString());
        }
    }

    // Читает одну логическую CSV-запись, включая переносы строк внутри кавычек.
    private String[] readCsvRecord(PushbackReader reader, int recordNumber)
            throws IOException {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean afterQuotes = false;
        boolean started = false;

        while (true) {
            int next = reader.read();
            if (next == -1) {
                if (!started && values.isEmpty() && current.length() == 0) {
                    return null;
                }
                if (inQuotes) {
                    throw new IOException("Незакрытые кавычки в записи " + recordNumber);
                }
                values.add(current.toString());
                return values.toArray(new String[0]);
            }

            char ch = (char) next;
            started = true;
            if (inQuotes) {
                if (ch == '"') {
                    int following = reader.read();
                    if (following == '"') {
                        current.append('"');
                    } else {
                        inQuotes = false;
                        afterQuotes = true;
                        if (following != -1) {
                            reader.unread(following);
                        }
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == ',') {
                values.add(current.toString());
                current.setLength(0);
                afterQuotes = false;
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r') {
                    int following = reader.read();
                    if (following != '\n' && following != -1) {
                        reader.unread(following);
                    }
                }
                values.add(current.toString());
                return values.toArray(new String[0]);
            } else if (afterQuotes) {
                throw new IOException("Некорректный CSV в записи " + recordNumber);
            } else if (ch == '"' && current.length() == 0) {
                inQuotes = true;
            } else if (ch == '"') {
                throw new IOException("Некорректные кавычки в записи " + recordNumber);
            } else {
                current.append(ch);
            }
        }
    }
}

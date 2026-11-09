
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

        JsonArray rows;

        // 1. Читаем JSON
        try (Reader reader = Files.newBufferedReader(
                source.toPath(), StandardCharsets.UTF_8)) {

            JsonElement root = JsonParser.parseReader(reader);

            if (!root.isJsonArray()) {
                throw new IOException(
                        "JSON root must be an array"
                );
            }

            rows = root.getAsJsonArray();

        } catch (RuntimeException e) {
            throw new IOException("Invalid JSON file", e);
        }

        // 2. Собираем объекты и названия столбцов
        List<JsonObject> objects = new ArrayList<>();
        Set<String> headers = new LinkedHashSet<>();

        for (JsonElement element : rows) {

            if (!element.isJsonObject()) {
                throw new IOException(
                        "Every array element must be an object"
                );
            }

            JsonObject object = element.getAsJsonObject();
            objects.add(object);

            headers.addAll(object.keySet());

            // Поддерживаем только простые значения
            for (String key : object.keySet()) {
                JsonElement value = object.get(key);

                if (!value.isJsonNull()
                        && !value.isJsonPrimitive()) {
                    throw new IOException(
                            "Nested JSON values are not supported"
                    );
                }
            }
        }

        // 3. Записываем CSV
        try (Writer writer = Files.newBufferedWriter(
                target.toPath(), StandardCharsets.UTF_8)) {

            writeCsvRow(writer, new ArrayList<>(headers));

            for (JsonObject object : objects) {

                List<String> values = new ArrayList<>();

                for (String header : headers) {

                    JsonElement value = object.get(header);

                    if (value == null || value.isJsonNull()) {
                        values.add("");
                    } else {
                        values.add(value.getAsString());
                    }
                }

                writeCsvRow(writer, values);
            }
        }
    }

    // Записывает одну строку CSV
    private void writeCsvRow(
            Writer writer, List<String> values)
            throws IOException {

        for (int i = 0; i < values.size(); i++) {

            if (i > 0) {
                writer.write(",");
            }

            writer.write(escapeCsv(values.get(i)));
        }

        writer.write(System.lineSeparator());
    }

    // Экранирует специальные символы CSV
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

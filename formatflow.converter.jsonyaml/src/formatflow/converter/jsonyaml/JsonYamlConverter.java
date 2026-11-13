package formatflow.converter.jsonyaml;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import formatflow.api.FileConverter;

public class JsonYamlConverter implements FileConverter {

    @Override
    public String getSourceFormat() {
        return "json";
    }

    @Override
    public String getTargetFormat() {
        return "yaml";
    }

    @Override
    public void convert(File source, File target)
            throws IOException {

        try {
            // Читаем JSON
            JsonElement json;

            try (Reader reader = Files.newBufferedReader(
                    source.toPath(),
                    StandardCharsets.UTF_8)) {

                json = JsonParser.parseReader(reader);
            }

            // Преобразуем JSON в Java-объекты
            Object data = convertToJava(json);

            // Настраиваем формат YAML
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(
                    DumperOptions.FlowStyle.BLOCK
            );
            options.setPrettyFlow(true);
            options.setIndent(2);

            Yaml yaml = new Yaml(options);

            // Записываем YAML
            try (Writer writer = Files.newBufferedWriter(
                    target.toPath(),
                    StandardCharsets.UTF_8)) {

                yaml.dump(data, writer);
            }

        } catch (Exception e) {
            throw new IOException(
                    "Failed to convert JSON to YAML: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private Object convertToJava(JsonElement json) {

        if (json == null || json.isJsonNull()) {
            return null;
        }

        if (json.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();

            JsonObject object = json.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry
                    : object.entrySet()) {

                map.put(
                        entry.getKey(),
                        convertToJava(entry.getValue())
                );
            }

            return map;
        }

        if (json.isJsonArray()) {
            List<Object> list = new ArrayList<>();

            JsonArray array = json.getAsJsonArray();

            for (JsonElement element : array) {
                list.add(convertToJava(element));
            }

            return list;
        }

        JsonPrimitive primitive = json.getAsJsonPrimitive();

        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }

        if (primitive.isNumber()) {
            BigDecimal number = new BigDecimal(primitive.getAsString());

            try {
                return number.intValueExact();
            } catch (ArithmeticException e) {
                try {
                    return number.longValueExact();
                } catch (ArithmeticException ex) {
                    return number.doubleValue();
                }
            }
        }

        return primitive.getAsString();
    }
}

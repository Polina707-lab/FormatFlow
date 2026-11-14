
package formatflow.converter.yamljson;
import formatflow.api.SafeFileWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import formatflow.api.FileConverter;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class YamlJsonConverter implements FileConverter {

    @Override
    public String getSourceFormat() {
        return "yaml";
    }

    @Override
    public String getTargetFormat() {
        return "json";
    }


@Override
public void convert(File inputFile, File outputFile)
        throws IOException {

    try {
        // Проверяем, что исходный файл не пустой
        try (Reader reader = Files.newBufferedReader(
                inputFile.toPath(),
                StandardCharsets.UTF_8)) {

            int character;
            boolean hasContent = false;

            while ((character = reader.read()) != -1) {
                if (!Character.isWhitespace(character)) {
                    hasContent = true;
                    break;
                }
            }

            if (!hasContent) {
                throw new IOException("Input YAML file is empty");
            }
        }

        // Читаем YAML
        Yaml yaml = new Yaml();

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();

        try (Reader reader = Files.newBufferedReader(
                inputFile.toPath(),
                StandardCharsets.UTF_8)) {

            Object data = yaml.load(reader);

            // Записываем JSON
            SafeFileWriter.write(outputFile.toPath(), tempFile -> {

                try (Writer writer = Files.newBufferedWriter(
                        tempFile,
                        StandardCharsets.UTF_8)) {

                    gson.toJson(data, writer);
                }
            });
        }

    } catch (Exception e) {
        throw new IOException(
                "Failed to convert YAML to JSON: "
                        + e.getMessage(),
                e
        );
    }
}

}

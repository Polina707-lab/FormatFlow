
package formatflow.converter.yamljson;

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
    public void convert(File inputFile, File outputFile) throws IOException {

        Yaml yaml = new Yaml();

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();

        try (Reader reader = Files.newBufferedReader(
                inputFile.toPath(), StandardCharsets.UTF_8)) {

            Object data = yaml.load(reader);

            try (Writer writer = Files.newBufferedWriter(
                    outputFile.toPath(), StandardCharsets.UTF_8)) {

                gson.toJson(data, writer);
            }
        }
    }
}

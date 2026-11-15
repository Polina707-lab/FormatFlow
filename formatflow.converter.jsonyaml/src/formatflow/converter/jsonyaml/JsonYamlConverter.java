
package formatflow.converter.jsonyaml;

import formatflow.api.FileConverter;
import formatflow.api.SafeFileWriter;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.events.*;
import org.yaml.snakeyaml.nodes.Tag;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

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
            SafeFileWriter.write(target.toPath(), tempFile -> {

                try (Reader reader = Files.newBufferedReader(
                            source.toPath(), StandardCharsets.UTF_8);
                     Writer writer = Files.newBufferedWriter(
                            tempFile, StandardCharsets.UTF_8);
                     JsonReader jsonReader = new JsonReader(reader)) {

                    jsonReader.setStrictness(Strictness.STRICT);

                    DumperOptions options = new DumperOptions();
                    options.setDefaultFlowStyle(
                            DumperOptions.FlowStyle.BLOCK
                    );
                    options.setIndent(2);
                    options.setPrettyFlow(true);
                    options.setAllowUnicode(true);

                    Emitter emitter = new Emitter(writer, options);

                    emitter.emit(new StreamStartEvent(null, null));
                    emitter.emit(new DocumentStartEvent(
                            null, null, false, null, null
                    ));

                    writeValue(jsonReader, emitter);

                    if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                        throw new IOException(
                                "Unexpected content after JSON value"
                        );
                    }

                    emitter.emit(new DocumentEndEvent(
                            null, null, false
                    ));
                    emitter.emit(new StreamEndEvent(null, null));

                    writer.flush();
                }
            });

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(
                    "Failed to convert JSON to YAML: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private void writeValue(
            JsonReader reader,
            Emitter emitter
    ) throws IOException {

        JsonToken token = reader.peek();

        switch (token) {

            case BEGIN_OBJECT -> {
                emitter.emit(new MappingStartEvent(
                        null, Tag.MAP.getValue(),
                        true, null, null,
                        DumperOptions.FlowStyle.BLOCK
                ));

                reader.beginObject();

                while (reader.hasNext()) {
                    String name = reader.nextName();

                    writeScalar(
                            emitter,
                            Tag.STR,
                            name,
                            DumperOptions.ScalarStyle.DOUBLE_QUOTED
                    );

                    writeValue(reader, emitter);
                }

                reader.endObject();

                emitter.emit(new MappingEndEvent(null, null));
            }

            case BEGIN_ARRAY -> {
                emitter.emit(new SequenceStartEvent(
                        null, Tag.SEQ.getValue(),
                        true, null, null,
                        DumperOptions.FlowStyle.BLOCK
                ));

                reader.beginArray();

                while (reader.hasNext()) {
                    writeValue(reader, emitter);
                }

                reader.endArray();

                emitter.emit(new SequenceEndEvent(null, null));
            }

            case STRING -> writeScalar(
                    emitter,
                    Tag.STR,
                    reader.nextString(),
                    DumperOptions.ScalarStyle.DOUBLE_QUOTED
            );

            case NUMBER -> {
                String value = reader.nextString();
                BigDecimal number;

                try {
                    number = new BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw new IOException(
                            "Invalid JSON number: " + value, e
                    );
                }

                boolean integer =
                        number.stripTrailingZeros().scale() <= 0;

                writeScalar(
                        emitter,
                        integer ? Tag.INT : Tag.FLOAT,
                        value,
                        DumperOptions.ScalarStyle.PLAIN
                );
            }

            case BOOLEAN -> writeScalar(
                    emitter,
                    Tag.BOOL,
                    Boolean.toString(reader.nextBoolean())
                            .toLowerCase(Locale.ROOT),
                    DumperOptions.ScalarStyle.PLAIN
            );

            case NULL -> {
                reader.nextNull();

                writeScalar(
                        emitter,
                        Tag.NULL,
                        "null",
                        DumperOptions.ScalarStyle.PLAIN
                );
            }

            default -> throw new IOException(
                    "Unexpected JSON token: " + token
            );
        }
    }


private void writeScalar(
        Emitter emitter,
        Tag tag,
        String value,
        DumperOptions.ScalarStyle style
) throws IOException {

    emitter.emit(new ScalarEvent(
            null,
            tag.getValue(),
            new ImplicitTuple(true, true),
            value,
            null,
            null,
            style
    ));
}

}

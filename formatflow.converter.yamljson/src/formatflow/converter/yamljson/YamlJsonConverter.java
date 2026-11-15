
package formatflow.converter.yamljson;

import formatflow.api.FileConverter;
import formatflow.api.SafeFileWriter;

import com.google.gson.stream.JsonWriter;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.events.*;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class YamlJsonConverter implements FileConverter {

    private static final Pattern JSON_NUMBER = Pattern.compile(
            "-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?"
                    + "(?:[eE][+-]?[0-9]+)?"
    );

    private final Resolver resolver = new Resolver();

    @Override
    public String getSourceFormat() {
        return "yaml";
    }

    @Override
    public String getTargetFormat() {
        return "json";
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
                     JsonWriter jsonWriter = new JsonWriter(writer)) {

                    LoaderOptions options = new LoaderOptions();
                    options.setCodePointLimit(100_000_000);

                    Parser parser = new ParserImpl(
                            new StreamReader(reader), options
                    );

                    jsonWriter.setIndent("  ");
                    jsonWriter.setSerializeNulls(true);

                    expect(parser, Event.ID.StreamStart);
                    expect(parser, Event.ID.DocumentStart);

                    if (parser.checkEvent(Event.ID.DocumentEnd)) {
                        throw new IOException("YAML document is empty");
                    }

                    writeValue(parser, jsonWriter);

                    expect(parser, Event.ID.DocumentEnd);

                    if (!parser.checkEvent(Event.ID.StreamEnd)) {
                        throw new IOException(
                                "Multiple YAML documents are not supported"
                        );
                    }

                    expect(parser, Event.ID.StreamEnd);

                    jsonWriter.flush();
                }
            });

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(
                    "Failed to convert YAML to JSON: "
                            + e.getMessage(), e
            );
        }
    }

    private void writeValue(
            Parser parser,
            JsonWriter writer
    ) throws IOException {

        Event event = parser.peekEvent();

        if (event == null) {
            throw new IOException("Unexpected end of YAML");
        }

        switch (event.getEventId()) {

            case MappingStart -> {
                MappingStartEvent start =
                        (MappingStartEvent) parser.getEvent();

                checkAnchor(start);
                checkTag(start.getTag(), Tag.MAP);

                writer.beginObject();

                Set<String> keys = new HashSet<>();

                while (!parser.checkEvent(Event.ID.MappingEnd)) {

                    Event keyEvent = parser.getEvent();

                    if (!(keyEvent instanceof ScalarEvent key)) {
                        throw new IOException(
                                "YAML object keys must be strings"
                        );
                    }

                    checkAnchor(key);

                    Tag keyTag = resolveTag(key);

                    if (!Tag.STR.equals(keyTag)) {
                        throw new IOException(
                                "YAML object keys must be strings"
                        );
                    }

                    String name = key.getValue();

                    if (!keys.add(name)) {
                        throw new IOException(
                                "Duplicate YAML key: " + name
                        );
                    }

                    writer.name(name);
                    writeValue(parser, writer);
                }

                expect(parser, Event.ID.MappingEnd);
                writer.endObject();
            }

            case SequenceStart -> {
                SequenceStartEvent start =
                        (SequenceStartEvent) parser.getEvent();

                checkAnchor(start);
                checkTag(start.getTag(), Tag.SEQ);

                writer.beginArray();

                while (!parser.checkEvent(Event.ID.SequenceEnd)) {
                    writeValue(parser, writer);
                }

                expect(parser, Event.ID.SequenceEnd);
                writer.endArray();
            }

            case Scalar -> {
                ScalarEvent scalar =
                        (ScalarEvent) parser.getEvent();

                checkAnchor(scalar);
                writeScalar(scalar, writer);
            }

            case Alias -> throw new IOException(
                    "YAML aliases are not supported"
            );

            default -> throw new IOException(
                    "Unexpected YAML event: " + event.getEventId()
            );
        }
    }

    private void writeScalar(
            ScalarEvent scalar,
            JsonWriter writer
    ) throws IOException {

        String value = scalar.getValue();
        Tag tag = resolveTag(scalar);

        if (Tag.STR.equals(tag)) {
            writer.value(value);
            return;
        }

        if (Tag.NULL.equals(tag)) {
            writer.nullValue();
            return;
        }

        if (Tag.BOOL.equals(tag)) {
            if ("true".equalsIgnoreCase(value)) {
                writer.value(true);
            } else if ("false".equalsIgnoreCase(value)) {
                writer.value(false);
            } else {
                throw new IOException(
                        "Unsupported YAML boolean: " + value
                );
            }
            return;
        }

        if (Tag.INT.equals(tag) || Tag.FLOAT.equals(tag)) {

            if (!JSON_NUMBER.matcher(value).matches()) {
                throw new IOException(
                        "Unsupported YAML number: " + value
                );
            }

            try {
                writer.value(new BigDecimal(value));
            } catch (NumberFormatException e) {
                throw new IOException(
                        "Invalid YAML number: " + value, e
                );
            }
            return;
        }

        throw new IOException(
                "Unsupported YAML tag: " + tag
        );
    }

    private Tag resolveTag(ScalarEvent scalar)
            throws IOException {

        String explicitTag = scalar.getTag();

        if (explicitTag != null) {
            Tag tag = new Tag(explicitTag);

            if (Tag.STR.equals(tag)
                    || Tag.INT.equals(tag)
                    || Tag.FLOAT.equals(tag)
                    || Tag.BOOL.equals(tag)
                    || Tag.NULL.equals(tag)) {
                return tag;
            }

            throw new IOException(
                    "Unsupported YAML tag: " + explicitTag
            );
        }

        return resolver.resolve(
                NodeId.scalar,
                scalar.getValue(),
                scalar.isPlain()
        );
    }

    private void checkAnchor(NodeEvent event)
            throws IOException {

        if (event.getAnchor() != null) {
            throw new IOException(
                    "YAML anchors are not supported"
            );
        }
    }

    private void checkTag(String actual, Tag expected)
            throws IOException {

        if (actual != null
                && !expected.getValue().equals(actual)) {
            throw new IOException(
                    "Unsupported YAML tag: " + actual
            );
        }
    }

    private void expect(Parser parser, Event.ID expected)
            throws IOException {

        Event event = parser.getEvent();

        if (event == null
                || event.getEventId() != expected) {
            throw new IOException(
                    "Expected YAML event: " + expected
            );
        }
    }
}

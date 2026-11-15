
package formatflow.converter.xmljson;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.gson.stream.JsonWriter;

import formatflow.api.FileConverter;

public class XmlJsonConverter implements FileConverter {

    @Override
    public String getSourceFormat() {
        return "xml";
    }

    @Override
    public String getTargetFormat() {
        return "json";
    }

    @Override
    public void convert(File source, File target) throws IOException {

        XMLStreamReader xmlReader = null;

        try (Reader reader = Files.newBufferedReader(
                    source.toPath(), StandardCharsets.UTF_8);
             Writer writer = Files.newBufferedWriter(
                    target.toPath(), StandardCharsets.UTF_8);
             JsonWriter jsonWriter = new JsonWriter(writer)) {

            XMLInputFactory factory =
                    XMLInputFactory.newInstance();

            factory.setProperty(
                    XMLInputFactory.SUPPORT_DTD, false
            );

            factory.setProperty(
                    XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                    false
            );

            xmlReader = factory.createXMLStreamReader(reader);

            jsonWriter.setIndent("  ");
            jsonWriter.setSerializeNulls(true);

            if (!moveToNextElement(xmlReader)) {
                throw new IOException("Empty XML document");
            }

            if (!"json".equals(xmlReader.getLocalName())) {
                throw new IOException(
                        "Invalid FormatFlow XML: root must be <json>"
                );
            }

            writeJsonValue(xmlReader, jsonWriter);

            while (xmlReader.hasNext()) {
                int event = xmlReader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    throw new IOException(
                            "Unexpected XML element after root"
                    );
                }
            }

            jsonWriter.flush();

        } catch (XMLStreamException e) {
            throw new IOException(
                    "Failed to convert XML to JSON: "
                            + e.getMessage(),
                    e
            );
        } finally {
            if (xmlReader != null) {
                try {
                    xmlReader.close();
                } catch (XMLStreamException ignored) {
                }
            }
        }
    }

    private boolean moveToNextElement(
            XMLStreamReader reader
    ) throws XMLStreamException {

        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                return true;
            }
        }

        return false;
    }

    private void writeJsonValue(
            XMLStreamReader reader,
            JsonWriter writer
    ) throws XMLStreamException, IOException {

        String type = reader.getAttributeValue(null, "type");

        if (type == null) {
            throw new IOException(
                    "XML element is missing type attribute"
            );
        }

        switch (type) {

            case "object" -> writeObject(reader, writer);

            case "array" -> writeArray(reader, writer);

            case "string" ->
                    writer.value(readPrimitiveText(reader));

            case "number" -> {
                String value = readPrimitiveText(reader);

                try {
                    writer.value(new BigDecimal(value));
                } catch (NumberFormatException e) {
                    throw new IOException(
                            "Invalid number: " + value, e
                    );
                }
            }

            case "boolean" -> {
                String value = readPrimitiveText(reader);

                if (!"true".equals(value)
                        && !"false".equals(value)) {
                    throw new IOException(
                            "Invalid boolean value: " + value
                    );
                }

                writer.value(Boolean.parseBoolean(value));
            }

            case "null" -> {
                String value = readPrimitiveText(reader);

                if (!value.isBlank()) {
                    throw new IOException(
                            "Null element must be empty"
                    );
                }

                writer.nullValue();
            }

            default -> throw new IOException(
                    "Unsupported XML element type: " + type
            );
        }
    }

    private void writeObject(
            XMLStreamReader reader,
            JsonWriter writer
    ) throws XMLStreamException, IOException {

        writer.beginObject();

        Set<String> names = new HashSet<>();

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {

                if (!"entry".equals(reader.getLocalName())) {
                    throw new IOException(
                            "Object must contain only <entry> elements"
                    );
                }

                String name =
                        reader.getAttributeValue(null, "name");

                if (name == null) {
                    throw new IOException(
                            "Entry element is missing name attribute"
                    );
                }

                if (!names.add(name)) {
                    throw new IOException(
                            "Duplicate JSON property: " + name
                    );
                }

                writer.name(name);
                writeJsonValue(reader, writer);

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                writer.endObject();
                return;

            } else if (event == XMLStreamConstants.CHARACTERS
                    && !reader.isWhiteSpace()) {

                throw new IOException(
                        "Unexpected text inside object"
                );
            }
        }

        throw new IOException("Unclosed XML object");
    }

    private void writeArray(
            XMLStreamReader reader,
            JsonWriter writer
    ) throws XMLStreamException, IOException {

        writer.beginArray();

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {

                if (!"item".equals(reader.getLocalName())) {
                    throw new IOException(
                            "Array must contain only <item> elements"
                    );
                }

                writeJsonValue(reader, writer);

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                writer.endArray();
                return;

            } else if (event == XMLStreamConstants.CHARACTERS
                    && !reader.isWhiteSpace()) {

                throw new IOException(
                        "Unexpected text inside array"
                );
            }
        }

        throw new IOException("Unclosed XML array");
    }

    private String readPrimitiveText(
            XMLStreamReader reader
    ) throws XMLStreamException, IOException {

        StringBuilder text = new StringBuilder();

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.CHARACTERS
                    || event == XMLStreamConstants.CDATA) {

                text.append(reader.getText());

            } else if (event == XMLStreamConstants.START_ELEMENT) {

                throw new IOException(
                        "Primitive value cannot contain XML elements"
                );

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return text.toString();
            }
        }

        throw new IOException("Unclosed XML primitive");
    }
}

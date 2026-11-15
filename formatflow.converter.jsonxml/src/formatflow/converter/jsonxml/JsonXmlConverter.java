
package formatflow.converter.jsonxml;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import formatflow.api.FileConverter;

public class JsonXmlConverter implements FileConverter {

    @Override
    public String getSourceFormat() {
        return "json";
    }

    @Override
    public String getTargetFormat() {
        return "xml";
    }

    @Override
    public void convert(File source, File target)
            throws IOException {

        try (Reader reader = Files.newBufferedReader(
                source.toPath(), StandardCharsets.UTF_8);
             JsonReader jsonReader = new JsonReader(reader);
             Writer writer = Files.newBufferedWriter(
                     target.toPath(), StandardCharsets.UTF_8)) {

            jsonReader.setStrictness(Strictness.STRICT);

            XMLStreamWriter xmlWriter =
                    XMLOutputFactory.newInstance()
                            .createXMLStreamWriter(writer);

            try {
                xmlWriter.writeStartDocument("UTF-8", "1.0");

                writeElement(jsonReader, xmlWriter, "json", null);

                if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                    throw new IOException(
                            "Unexpected content after JSON value"
                    );
                }

                xmlWriter.writeEndDocument();
                xmlWriter.flush();

            } finally {
                xmlWriter.close();
            }

        } catch (XMLStreamException e) {
            throw new IOException(
                    "Failed to convert JSON to XML", e
            );
        }
    }

    private void writeElement(
            JsonReader jsonReader,
            XMLStreamWriter xmlWriter,
            String elementName,
            String attributeName
    ) throws IOException, XMLStreamException {

        JsonToken token = jsonReader.peek();

        xmlWriter.writeStartElement(elementName);

        if (attributeName != null) {
            xmlWriter.writeAttribute("name", attributeName);
        }

        switch (token) {

            case BEGIN_OBJECT -> {
                xmlWriter.writeAttribute("type", "object");

                jsonReader.beginObject();

                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();

                    writeElement(
                            jsonReader,
                            xmlWriter,
                            "entry",
                            name
                    );
                }

                jsonReader.endObject();
            }

            case BEGIN_ARRAY -> {
                xmlWriter.writeAttribute("type", "array");

                jsonReader.beginArray();

                while (jsonReader.hasNext()) {
                    writeElement(
                            jsonReader,
                            xmlWriter,
                            "item",
                            null
                    );
                }

                jsonReader.endArray();
            }

            case STRING -> {
                xmlWriter.writeAttribute("type", "string");
                xmlWriter.writeCharacters(
                        jsonReader.nextString()
                );
            }

            case NUMBER -> {
                xmlWriter.writeAttribute("type", "number");
                xmlWriter.writeCharacters(
                        jsonReader.nextString()
                );
            }

            case BOOLEAN -> {
                xmlWriter.writeAttribute("type", "boolean");
                xmlWriter.writeCharacters(
                        String.valueOf(jsonReader.nextBoolean())
                );
            }

            case NULL -> {
                xmlWriter.writeAttribute("type", "null");
                jsonReader.nextNull();
            }

            default -> throw new IOException(
                    "Unexpected JSON token: " + token
            );
        }

        xmlWriter.writeEndElement();
    }
}

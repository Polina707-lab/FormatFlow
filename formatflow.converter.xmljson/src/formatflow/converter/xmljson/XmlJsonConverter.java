
package formatflow.converter.xmljson;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import formatflow.api.FileConverter;

public class XmlJsonConverter implements FileConverter {

    private final Gson gson = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    @Override
    public String getSourceFormat() {
        return "xml";
    }

    @Override
    public String getTargetFormat() {
        return "json";
    }

    @Override
    public void convert(File source, File target)
            throws IOException {

        try {
            // 1. Безопасно настраиваем XML-парсер
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );

            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
            );

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
            );

            factory.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING,
                    true
            );

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_DTD, ""
            );
            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""
            );

            // 2. Читаем XML
            Document document = factory
                    .newDocumentBuilder()
                    .parse(source);

            Element root = document.getDocumentElement();

            if (root == null || !"json".equals(root.getTagName())) {
                throw new IOException(
                        "Invalid FormatFlow XML: root must be <json>"
                );
            }

            // 3. Восстанавливаем JSON
            JsonElement json = readElement(root);

            // 4. Записываем результат
            try (Writer writer = Files.newBufferedWriter(
                    target.toPath(),
                    StandardCharsets.UTF_8)) {

                gson.toJson(json, writer);
            }

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(
                    "Failed to convert XML to JSON: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private JsonElement readElement(Element element)
            throws IOException {

        String type = element.getAttribute("type");

        switch (type) {

            case "object":
                return readObject(element);

            case "array":
                return readArray(element);

            case "string":
                requireNoElementChildren(element);
                return new JsonPrimitive(element.getTextContent());

            case "number":
                requireNoElementChildren(element);
                try {
                    return new JsonPrimitive(
                            new BigDecimal(element.getTextContent())
                    );
                } catch (NumberFormatException e) {
                    throw new IOException(
                            "Invalid number: "
                                    + element.getTextContent(),
                            e
                    );
                }

            case "boolean":
                requireNoElementChildren(element);

                String value = element.getTextContent();

                if (!"true".equals(value)
                        && !"false".equals(value)) {
                    throw new IOException(
                            "Invalid boolean value: " + value
                    );
                }

                return new JsonPrimitive(
                        Boolean.parseBoolean(value)
                );

            case "null":
                requireNoElementChildren(element);
                return JsonNull.INSTANCE;

            default:
                throw new IOException(
                        "Unsupported XML element type: " + type
                );
        }
    }

    private JsonObject readObject(Element element)
            throws IOException {

        JsonObject object = new JsonObject();
        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;

            if (!"entry".equals(child.getTagName())) {
                throw new IOException(
                        "Object must contain only <entry> elements"
                );
            }

            if (!child.hasAttribute("name")) {
                throw new IOException(
                        "Entry element is missing name attribute"
                );
            }

            String name = child.getAttribute("name");

            if (object.has(name)) {
                throw new IOException(
                        "Duplicate JSON property: " + name
                );
            }

            object.add(name, readElement(child));
        }

        return object;
    }

    private JsonArray readArray(Element element)
            throws IOException {

        JsonArray array = new JsonArray();
        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;

            if (!"item".equals(child.getTagName())) {
                throw new IOException(
                        "Array must contain only <item> elements"
                );
            }

            array.add(readElement(child));
        }

        return array;
    }

    private void requireNoElementChildren(Element element)
            throws IOException {

        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType()
                    == Node.ELEMENT_NODE) {

                throw new IOException(
                        "Primitive value cannot contain XML elements"
                );
            }
        }
    }
}

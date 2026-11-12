
package formatflow.converter.jsonxml;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

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

        try {
            JsonElement json;

            // 1. Читаем JSON
            try (Reader reader = Files.newBufferedReader(
                    source.toPath(), StandardCharsets.UTF_8)) {

                json = JsonParser.parseReader(reader);
            }

            // 2. Создаём XML-документ
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            Document document = factory
                    .newDocumentBuilder()
                    .newDocument();

            Element root = document.createElement("json");
            document.appendChild(root);

            // 3. Преобразуем JSON в XML
            writeElement(document, root, json);

            // 4. Записываем XML в файл
            Transformer transformer =
                    TransformerFactory.newInstance()
                            .newTransformer();

            transformer.setOutputProperty(
                    OutputKeys.INDENT, "yes"
            );

            transformer.setOutputProperty(
                    OutputKeys.ENCODING, "UTF-8"
            );

            transformer.transform(
                    new DOMSource(document),
                    new StreamResult(target)
            );

        } catch (Exception e) {
            throw new IOException(
                    "Failed to convert JSON to XML: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private void writeElement(
            Document document,
            Element element,
            JsonElement json
    ) {

        // JSON null
        if (json == null || json.isJsonNull()) {
            element.setAttribute("type", "null");
            return;
        }

        // JSON object
        if (json.isJsonObject()) {
            element.setAttribute("type", "object");

            JsonObject object = json.getAsJsonObject();

            for (String key : object.keySet()) {

                Element child = document.createElement("entry");
                child.setAttribute("name", key);

                element.appendChild(child);

                writeElement(
                        document,
                        child,
                        object.get(key)
                );
            }

            return;
        }

        // JSON array
        if (json.isJsonArray()) {
            element.setAttribute("type", "array");

            JsonArray array = json.getAsJsonArray();

            for (JsonElement item : array) {

                Element child = document.createElement("item");
                element.appendChild(child);

                writeElement(document, child, item);
            }

            return;
        }

        // JSON primitive
        JsonPrimitive primitive = json.getAsJsonPrimitive();

        if (primitive.isBoolean()) {
            element.setAttribute("type", "boolean");
        } else if (primitive.isNumber()) {
            element.setAttribute("type", "number");
        } else {
            element.setAttribute("type", "string");
        }

        element.setTextContent(primitive.getAsString());
    }
}

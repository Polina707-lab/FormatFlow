
package formatflow.ui.preview;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilePreviewService {

    private static final int MAX_CHARACTERS = 10_000;

    public String readPreview(Path path) throws IOException {

        if (!Files.isRegularFile(path)) {
            throw new IOException("File does not exist.");
        }

        StringBuilder preview = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(
                path, StandardCharsets.UTF_8)) {

            char[] buffer = new char[1024];
            int read;

            while (preview.length() < MAX_CHARACTERS
                    && (read = reader.read(
                            buffer,
                            0,
                            Math.min(buffer.length,
                                    MAX_CHARACTERS - preview.length() + 1)
                    )) != -1) {

                preview.append(buffer, 0, read);
            }
        }

        if (preview.length() > MAX_CHARACTERS) {
            preview.setLength(MAX_CHARACTERS);
            preview.append("\n\n... Preview truncated ...");
        }

        return preview.toString();
    }
}

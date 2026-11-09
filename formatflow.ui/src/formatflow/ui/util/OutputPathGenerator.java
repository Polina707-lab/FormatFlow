
package formatflow.ui.util;

import java.nio.file.Path;
import java.util.Locale;

public final class OutputPathGenerator {

    private OutputPathGenerator() {
    }

    public static String generate(
            String sourcePath,
            String targetFormat
    ) {
        if (sourcePath == null || sourcePath.isBlank()
                || targetFormat == null || targetFormat.isBlank()) {
            return "";
        }

        Path source;

        try {
            source = Path.of(sourcePath.trim());
        } catch (IllegalArgumentException e) {
            return "";
        }

        Path fileNamePath = source.getFileName();

        if (fileNamePath == null) {
            return "";
        }

        String fileName = fileNamePath.toString();

        int dotIndex = fileName.lastIndexOf('.');

        String baseName = dotIndex > 0
                ? fileName.substring(0, dotIndex)
                : fileName;

        String newFileName = baseName + "."
                + targetFormat.toLowerCase(Locale.ROOT);

        Path parent = source.getParent();

        return parent != null
                ? parent.resolve(newFileName).toString()
                : newFileName;
    }
}

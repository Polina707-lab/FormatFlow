
package formatflow.ui.util;

import java.nio.file.Path;
import java.util.Locale;

public final class FileFormatDetector {

    private FileFormatDetector() {
    }

    public static String detectFormat(String filePath) {

        if (filePath == null || filePath.isBlank()) {
            return "";
        }

        String fileName;

        try {
            fileName = Path.of(filePath.trim())
                    .getFileName()
                    .toString();
        } catch (IllegalArgumentException e) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }
}

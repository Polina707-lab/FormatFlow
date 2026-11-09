package formatflow.ui.validation;

import java.io.File;
import java.io.IOException;

public class ConversionValidator {

    public static void validateFiles(
            String sourcePath,
            String outputPath
    ) throws IOException {

        if (sourcePath.isEmpty() || outputPath.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select source and output files."
            );
        }

        File source = new File(sourcePath);

        if (!source.isFile()) {
            throw new IllegalArgumentException(
                    "The selected source file does not exist."
            );
        }

        File target = new File(outputPath);

        if (source.getCanonicalFile().equals(target.getCanonicalFile())) {
            throw new IllegalArgumentException(
                    "Source and output files must be different."
            );
        }
    }
}
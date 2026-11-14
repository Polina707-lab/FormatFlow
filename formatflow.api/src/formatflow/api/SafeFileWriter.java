
package formatflow.api;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SafeFileWriter {

    private SafeFileWriter() {
    }

    @FunctionalInterface
    public interface FileWriterAction {
        void write(Path path) throws IOException;
    }

    public static void write(
            Path target,
            FileWriterAction action
    ) throws IOException {

        Path absoluteTarget = target.toAbsolutePath();
        Path directory = absoluteTarget.getParent();

        Path tempFile = Files.createTempFile(
                directory,
                ".formatflow-",
                ".tmp"
        );

        try {
            // Записываем результат во временный файл
            action.write(tempFile);

            // Заменяем целевой файл после успешной записи
            try {
                Files.move(
                        tempFile,
                        absoluteTarget,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                        tempFile,
                        absoluteTarget,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } finally {
            // Удаляем временный файл при ошибке
            Files.deleteIfExists(tempFile);
        }
    }
}

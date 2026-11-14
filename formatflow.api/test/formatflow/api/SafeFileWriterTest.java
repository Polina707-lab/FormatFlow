
package formatflow.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SafeFileWriterTest {

    public static void main(String[] args)
            throws IOException {

        Path directory = Files.createTempDirectory(
                "formatflow-test-"
        );

        Path target = directory.resolve("result.txt");

        // Создаём файл с исходным содержимым
        Files.writeString(
                target,
                "Original content",
                StandardCharsets.UTF_8
        );

        // Имитируем ошибку во время записи
        try {
            SafeFileWriter.write(target, tempFile -> {
                Files.writeString(
                        tempFile,
                        "Incomplete content",
                        StandardCharsets.UTF_8
                );

                throw new IOException("Simulated write error");
            });
        } catch (IOException e) {
            System.out.println(
                    "Expected error: " + e.getMessage()
            );
        }

        // Проверяем содержимое целевого файла
        String result = Files.readString(
                target,
                StandardCharsets.UTF_8
        );

        if (!"Original content".equals(result)) {
            throw new AssertionError(
                    "Original file was modified"
            );
        }

        // Проверяем удаление временных файлов
        try (var files = Files.list(directory)) {
            boolean tempExists = files.anyMatch(
                    path -> path.getFileName()
                            .toString()
                            .startsWith(".formatflow-")
            );

            if (tempExists) {
                throw new AssertionError(
                        "Temporary file was not deleted"
                );
            }
        }

        System.out.println("SafeFileWriter test passed");
        
     // Проверяем успешную замену файла
        SafeFileWriter.write(target, tempFile -> {
            Files.writeString(
                    tempFile,
                    "New content",
                    StandardCharsets.UTF_8
            );
        });

        String updatedContent = Files.readString(
                target,
                StandardCharsets.UTF_8
        );

        if (!"New content".equals(updatedContent)) {
            throw new AssertionError(
                    "Target file was not replaced correctly"
            );
        }

        System.out.println("File replacement test passed");

        // Удаляем тестовые файлы
        Files.deleteIfExists(target);
        Files.deleteIfExists(directory);
    }
}

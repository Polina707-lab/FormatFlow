
package formatflow.core;

import java.io.File;
import java.io.IOException;
import java.util.List;

import formatflow.api.ConverterChangeListener;
import formatflow.api.FileConversionService;
import formatflow.api.FileConverter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import formatflow.api.SafeFileWriter;

public class ConversionService implements FileConversionService {

    private final ConverterRegistry registry;

    public ConversionService(ConverterRegistry registry) {
        this.registry = registry;
    }
    
    private List<FileConverter> findConversionPath(
            String sourceFormat,
            String targetFormat
    ) {
        List<FileConverter> converters = registry.getConverters();

        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        Map<String, FileConverter> previous = new HashMap<>();

        String start = sourceFormat.toUpperCase(java.util.Locale.ROOT);
        String end = targetFormat.toUpperCase(java.util.Locale.ROOT);

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String current = queue.remove();

            if (current.equals(end)) {
                break;
            }

            for (FileConverter converter : converters) {

                if (!converter.getSourceFormat().equalsIgnoreCase(current)) {
                    continue;
                }

                String next = converter.getTargetFormat()
                        .toUpperCase(java.util.Locale.ROOT);

                if (visited.add(next)) {
                    previous.put(next, converter);
                    queue.add(next);
                }
            }
        }

        if (!visited.contains(end)) {
            return List.of();
        }

        List<FileConverter> path = new ArrayList<>();

        String current = end;

        while (!current.equals(start)) {
            FileConverter converter = previous.get(current);

            path.add(0, converter);

            current = converter.getSourceFormat()
                    .toUpperCase(java.util.Locale.ROOT);
        }

        return path;
    }

    @Override
    public void convert(
            File source,
            File target,
            String sourceFormat,
            String targetFormat
    ) throws IOException {

        if (source == null || target == null) {
            throw new IllegalArgumentException(
                    "Source and target files must not be null"
            );
        }

        Objects.requireNonNull(sourceFormat, "Source format must not be null");
        Objects.requireNonNull(targetFormat, "Target format must not be null");

        if (!source.isFile()) {
            throw new IOException("Source file does not exist: " + source);
        }

        Path sourcePath = source.toPath().toRealPath();
        Path targetPath = target.toPath().toAbsolutePath().normalize();

        if (sourcePath.equals(targetPath)
                || (Files.exists(targetPath)
                    && Files.isSameFile(sourcePath, targetPath))) {

            throw new IOException(
                    "Source and target must be different files"
            );
        }

        List<FileConverter> path =
                findConversionPath(sourceFormat, targetFormat);

        if (path.isEmpty()) {
            throw new IOException(
                    "No conversion path found for "
                            + sourceFormat + " -> " + targetFormat
            );
        }

      
        Path directory = targetPath.getParent();

        List<Path> temporaryFiles = new ArrayList<>();

        try {
            File currentSource = source;

            // Выполняем все преобразования, кроме последнего.
            for (int i = 0; i < path.size() - 1; i++) {

                FileConverter converter = path.get(i);

                Path temporaryFile = Files.createTempFile(
                        directory,
                        ".formatflow-step-",
                        ".tmp"
                );

                temporaryFiles.add(temporaryFile);

                converter.convert(
                        currentSource,
                        temporaryFile.toFile()
                );

                currentSource = temporaryFile.toFile();
            }

            // Последнее преобразование записываем безопасно.
            FileConverter lastConverter = path.get(path.size() - 1);

            File finalSource = currentSource;

            SafeFileWriter.write(targetPath, temporaryFile -> {
                lastConverter.convert(
                        finalSource,
                        temporaryFile.toFile()
                );
            });

        } finally {

            // Удаляем промежуточные файлы.
            for (Path temporaryFile : temporaryFiles) {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    @Override
    public List<String> getSourceFormats() {

        return registry.getConverters()
                .stream()
                .map(FileConverter::getSourceFormat)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public List<String> getTargetFormats(String sourceFormat) {

        if (sourceFormat == null || sourceFormat.isBlank()) {
            return List.of();
        }

        return registry.getConverters()
                .stream()
                .map(FileConverter::getTargetFormat)
                .distinct()
                .filter(targetFormat ->
                        !targetFormat.equalsIgnoreCase(sourceFormat))
                .filter(targetFormat ->
                        !findConversionPath(sourceFormat, targetFormat).isEmpty())
                .sorted()
                .toList();
    }
    
    @Override
    public void addConverterChangeListener(ConverterChangeListener listener) {
        registry.addListener(listener);
    }

    @Override
    public void removeConverterChangeListener(ConverterChangeListener listener) {
        registry.removeListener(listener);
    }
}

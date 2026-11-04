
package formatflow.api;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface FileConversionService {

    void convert(
            File source,
            File target,
            String sourceFormat,
            String targetFormat
    ) throws IOException;

    List<String> getSourceFormats();

    List<String> getTargetFormats(String sourceFormat);
    
    void addConverterChangeListener(ConverterChangeListener listener);

    void removeConverterChangeListener(ConverterChangeListener listener);
}

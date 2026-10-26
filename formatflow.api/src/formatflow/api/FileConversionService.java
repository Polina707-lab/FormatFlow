package formatflow.api;

import java.io.File;
import java.io.IOException;

public interface FileConversionService {
			void convert(
					File source,
					File target,
					String sourceFromat,
					String targetFormat
					
		
					
					) throws IOException;
			
}

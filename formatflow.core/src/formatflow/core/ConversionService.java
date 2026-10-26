package formatflow.core;

import java.io.File;
import java.io.IOException;

import formatflow.api.FileConversionService;
import formatflow.api.FileConverter;

public class ConversionService  implements FileConversionService{
	private final ConverterRegistry registry;
	
	public ConversionService(ConverterRegistry registry){
		this.registry = registry;
	}
		
	@Override
		public void convert(
				File source,
				File target, 
				String sourceFormat,
				String targetFormat
				) throws IOException {
			
			if(source == null || target == null) {
				
				throw new IllegalArgumentException("Source and target files must not be null");
			}
			
			for(FileConverter converter : registry.getConverters()) {
				
				if(converter.getSourceFormat().equalsIgnoreCase(sourceFormat) 
						&& converter.getTargetFormat().equalsIgnoreCase(targetFormat)) {
					converter.convert(source, target);
					return;
				}
			}
			
		    throw new IOException(
		            "No converter found for "
		                    + sourceFormat + " -> " + targetFormat);
			
		}
}



package formatflow.api;

import java.io.File;
import java.io.IOException;

public interface FileConverter {
	String getSourceFormat(); //возвращает исходный формат
	String getTargetFormat(); //возвращает целевой формат
	
	
	void convert(File source, File target) throws IOException;
	

}


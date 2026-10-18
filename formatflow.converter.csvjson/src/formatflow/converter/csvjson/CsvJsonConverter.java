package formatflow.converter.csvjson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;


import formatflow.api.FileConverter;

public class CsvJsonConverter  implements FileConverter{

	@Override 
	public String getSourceFormat(){
		return "csv";
	}
	
	@Override
	public String getTargetFormat() {
		return  "json";
	}
	
	@Override 
	public void convert(File source, File target) throws IOException {
		List<String> lines  = Files.readAllLines(source.toPath(), StandardCharsets.UTF_8);
		
		if(lines.isEmpty()) throw new IOException("CSV-файл пустой");
		
		//отберем заголовки
		String[] headers = lines.get(0).split(",", -1);
		List<String> objects = new ArrayList<>();
		
		//отберем строки 
		for(int i = 1; i < lines.size(); i++) {
			
			String[] values = lines.get(i).split(",", -1);
			
			if(values.length != headers.length) 
				throw new IOException("Некорректное число столбцов в строке  " + (i + 1));
				
				//формируем JSON-объект
				List<String> fields = new ArrayList<>();
				
				for(int j = 0; j < headers.length; j++) {
					fields.add(
							"\"" + escapeJson(headers[j]) + "\": "  + 
							"\"" + escapeJson(values[j]) + "\""
							);
					
				}
				
				objects.add("  {\n    " +
	                    String.join(",\n    ", fields) + "\n  }");
		
			
		}
		
		 String json = "[\n" + String.join(",\n", objects) + "\n]";
	        
	        Files.writeString(
	                target.toPath(), json, StandardCharsets.UTF_8);
	}
	
	private String escapeJson(String value) {
	    return value
	            .replace("\\", "\\\\")
	            .replace("\"", "\\\"")
	            .replace("\n", "\\n")
	            .replace("\r", "\\r")
	            .replace("\t", "\\t");
	}
	
}

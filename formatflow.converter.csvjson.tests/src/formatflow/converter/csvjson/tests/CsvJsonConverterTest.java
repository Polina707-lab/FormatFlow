package formatflow.converter.csvjson.tests;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import formatflow.converter.csvjson.CsvJsonConverter;

import org.junit.jupiter.api.Test;

class CsvJsonConverterTest {

	@Test
	void shouldReturnCorrectFormats() {
		CsvJsonConverter converter = new CsvJsonConverter();
		
		assertEquals("csv", converter.getSourceFormat());
		assertEquals("json", converter.getTargetFormat());
	}
	
	@Test
	void shouldConvertCsvToJson(@TempDir Path tempDir) throws IOException{
		Path source = tempDir.resolve("Student.csv");
		Path target = tempDir.resolve("students.json");
		
		String csv = "id,name\n1,Anna\n2,Max";
		Files.writeString(source, csv);
		
		CsvJsonConverter converter = new CsvJsonConverter();
		converter.convert(source.toFile(), target.toFile());
		
		String actualJson = Files.readString(target);
		String expectedJson = """
		        [
		          {
		            "id": "1",
		            "name": "Anna"
		          },
		          {
		            "id": "2",
		            "name": "Max"
		          }
		        ]""";
		
		assertEquals (expectedJson, actualJson);
	}
	
	@Test
	void shouldThrowsExceptionForEmptyCsv(@TempDir Path tempDir) throws IOException {
		Path source = tempDir.resolve("empty.csv");
		Path target = tempDir.resolve("result.json");
		
		Files.writeString(source,"");
		CsvJsonConverter converter = new CsvJsonConverter();
		
		assertThrows(IOException.class, () -> {
			converter.convert(source.toFile(), target.toFile());
		});
		
	}
	
	@Test
	void shouldThrowExceptionForInvaludColumnCount(@TempDir Path tempDir) throws IOException {
		Path source = tempDir.resolve("empty.csv");
		Path target = tempDir.resolve("result.csv");
		
		CsvJsonConverter converter = new CsvJsonConverter();
		
		String csv = "id,name,age\n1,Anna,20\n2,Max";
		Files.writeString(source,csv);
		
		assertThrows(IOException.class, () -> {
			converter.convert(source.toFile(),target.toFile());
		});
	}

}

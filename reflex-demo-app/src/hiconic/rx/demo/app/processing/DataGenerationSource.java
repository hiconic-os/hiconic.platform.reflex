package hiconic.rx.demo.app.processing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class DataGenerationSource {
	private List<String> names;
	private List<String> lastNames;
	
	public DataGenerationSource() {
		names = readLines("names.txt");
		lastNames = readLines("last-names.txt");
	}
	
	public List<String> getNames() {
		return names;
	}
	
	public List<String> getLastNames() {
		return lastNames;
	}

	private List<String> readLines(String resource) {
		List<String> elements = new ArrayList<String>();
		
		try (BufferedReader r = new BufferedReader(new InputStreamReader(getClass().getResource(resource).openStream(), "UTF-8"))) {
			String l = null;
			
			while ((l = r.readLine()) != null) {
				elements.add(l);
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return elements;
	}
}

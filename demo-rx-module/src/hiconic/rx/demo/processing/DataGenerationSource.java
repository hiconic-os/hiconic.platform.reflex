// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.demo.processing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class DataGenerationSource {

	private final List<String> names;
	private final List<String> lastNames;
	
	public DataGenerationSource() {
		names = readLines("names.txt");
		lastNames = readLines("last-names.txt");
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
	public List<String> getNames() {
		return names;
	}
	
	public List<String> getLastNames() {
		return lastNames;
	}

}

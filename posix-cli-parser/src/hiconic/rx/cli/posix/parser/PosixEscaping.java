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
package hiconic.rx.cli.posix.parser;

import java.util.HashMap;
import java.util.Map;

public class PosixEscaping {
	private static Map<String, Character> symbols = new HashMap<>();
	
	static {
		symbols.put("apos", '\'');
		symbols.put("at", '@');
		symbols.put("quot", '"');
		symbols.put("semi", ';');
		symbols.put("vert", '|');
		symbols.put("amp", '&');
		symbols.put("hat", '^');
		symbols.put("dollar", '$');
		symbols.put("percnt", '%');
		symbols.put("lpar", '(');
		symbols.put("rpar", ')');
		symbols.put("lcub", '{');
		symbols.put("rcub", '}');
		symbols.put("lsqb", '[');
		symbols.put("rsqb", ']');
		symbols.put("ast", '*');
		symbols.put("gt", '>');
		symbols.put("lt", '<');
		symbols.put("hash", '#');
		symbols.put("quest", '?');
	}
	
	public static String unescape(String rawArg) {
		int length = rawArg.length();
		StringBuilder builder = new StringBuilder();
		

		int escape = -1;
		StringBuilder escapeSymbol = null;
		
		
		for (int i = 0; i < length; i++) {
			char c = rawArg.charAt(i);
			

			if (escape == -1) {
				if (c != '&') {
					builder.append(c);
				}
				else {
					escape = 0;
					escapeSymbol = new StringBuilder();
				}
			}
			else {
				if (c == ';') {
					escape = -1;
					
					if (escapeSymbol.charAt(0) == '#') {
						char symbol = (char)Integer.parseInt(escapeSymbol.substring(1));
						builder.append(symbol);
					}
					else {
						String symbolKey = escapeSymbol.toString();
						Character symbol = symbols.get(symbolKey);
						
						if (symbol == null)
							throw new IllegalStateException("unsupported escape symbol: " + symbolKey);
						
						builder.append(symbol);
					}
				}
				else {
					escapeSymbol.append(c);
					escape++;
				}
			}
			
		}
		
		if (escape != -1) {
			builder.append('&');
			builder.append(escapeSymbol);
		}
		
		return builder.toString();
	}
}

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
package hiconic.rx.servlet.velocity;

import java.util.Date;

import com.braintribe.utils.DateTools;
import com.braintribe.utils.StringTools;

public class VelocityTools {

	public static String escape(String s) {
		int c = s.length();
		if (c == 0)
			return s;

		char[] chars = new char[c];
		s.getChars(0, c, chars, 0);

		StringBuilder b = new StringBuilder(c * 2);

		for (int i = 0; i < chars.length; i++) {
			char ch = chars[i];

			switch (ch) {
			case '&':
				b.append("&amp;");
				break;
			case '<':
				b.append("&lt;");
				break;
			case '>':
				b.append("&gt;");
				break;
			case '"':
				b.append("&quot;");
				break;
			case '\'':
				b.append("&apos;");
				break;
			default:
				if (ch > 0x7F) {
					b.append("&#");
					b.append(Integer.toString(ch, 10));
					b.append(';');
				} else {
					b.append(ch);
				}
			}
		}
		
		return b.toString();
	}
	
	public static String displayUserName(String firstName, String lastName, String userName, String defaultText) {
		
		String displayName = userName;
		if (!StringTools.isEmpty(firstName)) {
			displayName = firstName;
		}
		if (!StringTools.isEmpty(lastName)) {
			displayName = (StringTools.isEmpty(displayName) ? lastName : displayName + " " + lastName);
		}
		if (StringTools.isEmpty(displayName)) {
			displayName = defaultText;
		}
		return displayName;
		
	}

	public static String getDateAsString(Date date) {
		return getDateAsString(date, "dd MMMM yyyy, HH:mm");
	}

	public static String getDateAsString(Date date, String format) {
		return DateTools.getDateString(date, format);
	}
	
}

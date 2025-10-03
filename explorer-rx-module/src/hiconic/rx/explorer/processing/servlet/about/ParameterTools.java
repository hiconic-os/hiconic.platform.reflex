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
package hiconic.rx.explorer.processing.servlet.about;

import java.util.Map;

import com.braintribe.logging.Logger;
import com.braintribe.utils.StringTools;

import jakarta.servlet.http.HttpServletRequest;

public class ParameterTools {

	private static Logger logger = Logger.getLogger(ParameterTools.class);
	
	public static String getParameterMapAsString(HttpServletRequest req) {
		StringBuilder result = new StringBuilder();
		Map<String, String[]> parameterMap = req.getParameterMap();
		if (parameterMap != null) {
			boolean first = true;
			for (Map.Entry<String,String[]> entry : parameterMap.entrySet()) {
				String key = entry.getKey();
				String[] values = entry.getValue();
				String valuesAsString = StringTools.createStringFromArray(values);
				if (!first) {
					result.append(", ");
				} else {
					first = false;
				}
				result.append(key != null ? key : "null");
				result.append("=");
				result.append(valuesAsString != null ? valuesAsString : "null");
			}
		}
		return result.toString();
	}
	public static String getTypeOfRequest(HttpServletRequest req) {
		Map<String, String[]> parameters = req.getParameterMap();
		String type = getSingleParameter(parameters, "type");
		return type;
	}
	public static String getSingleParameter(Map<String, String[]> parameters, String key) {
		String[] values = parameters.get(key);
		if (values == null || values.length == 0) {
			return null;
		}
		return values[0];
	}
	public static String getSingleParameterAsString(HttpServletRequest req, String key) {
		Map<String, String[]> parameters = req.getParameterMap();
		if (parameters == null || parameters.isEmpty()) {
			return null;
		}
		String[] values = parameters.get(key);
		if (values == null || values.length == 0) {
			return null;
		}
		return values[0];
	}
	public static Long getSingleParameterAsLong(HttpServletRequest req, String key) {
		Map<String, String[]> parameters = req.getParameterMap();
		if (parameters == null || parameters.isEmpty()) {
			return null;
		}
		String[] values = parameters.get(key);
		if (values == null || values.length == 0) {
			return null;
		}
		try {
			return Long.parseLong(values[0]);
		} catch (NumberFormatException nfe) {
			logger.error("Invalid long value: " + values[0]);
			return null;
		}
	}
	public static Integer getSingleParameterAsInteger(HttpServletRequest req, String key) {
		Map<String, String[]> parameters = req.getParameterMap();
		if (parameters == null || parameters.isEmpty()) {
			return null;
		}
		String[] values = parameters.get(key);
		if (values == null || values.length == 0) {
			return null;
		}
		try {
			return Integer.parseInt(values[0]);
		} catch (NumberFormatException nfe) {
			logger.error("Invalid integer value: " + values[0]);
			return null;
		}
	}
	public static Boolean getSingleParameterAsBoolean(HttpServletRequest req, String key) {
		Map<String, String[]> parameters = req.getParameterMap();
		if (parameters == null || parameters.isEmpty()) {
			return null;
		}
		String[] values = parameters.get(key);
		if (values == null || values.length == 0) {
			return null;
		}
		return Boolean.parseBoolean(values[0]);
	}

}

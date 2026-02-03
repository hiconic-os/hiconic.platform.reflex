// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.hibernate.processing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.hibernate.dialect.Dialect;

import com.braintribe.cartridge.common.processing.DialectMapping;
import com.braintribe.cfg.Required;
import com.braintribe.exception.Exceptions;
import com.braintribe.persistence.hibernate.dialects.HibernateDialectMapping;
import com.braintribe.util.jdbc.dialect.JdbcDialectAutoSense;
import com.braintribe.utils.StringTools;

/**
 * This class holds the list of {@link DialectMapping}s. It's main purpose is to increase the expressiveness of the configuration (for EM).
 * 
 * @author michael.lafite
 */
public class DialectAutoSense {

	private final Map<Pattern, Class<? extends Dialect>> patternMappings = new HashMap<>();

	@Required
	public void setDialectMappings(List<HibernateDialectMapping> dialectMappings) {
		for (HibernateDialectMapping m: dialectMappings) {
			Pattern pattern = Pattern.compile(m.productRegex);
			patternMappings.put(pattern, m.dialectType);
		}
	}

	public Class<? extends Dialect> senseDialect(DataSource connectionPool) {
		try {
			return autoSenseDialect(JdbcDialectAutoSense.getProductNameAndVersion(connectionPool));
		} catch (RuntimeException e) {
			throw Exceptions.contextualize(e, "Could not autosense dialect.");
		}
	}

	private Class<? extends Dialect> autoSenseDialect(String productNameAndVersion) {
		if (StringTools.isEmpty(productNameAndVersion))
			throw new RuntimeException("The auto-sense feature of the connection pool does not work. Could not determine the database product name.");

		for (Map.Entry<Pattern, Class<? extends Dialect>> entry : patternMappings.entrySet()) {
			Matcher m = entry.getKey().matcher(productNameAndVersion);
			if (m.matches()) {
				return entry.getValue();
			}
		}

		throw new RuntimeException(
				"The auto-sense feature of the connection pool does not work. No pattern for database " + productNameAndVersion + " configured.");
	}

}

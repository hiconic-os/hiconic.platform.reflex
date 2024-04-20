// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
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
import com.braintribe.logging.Logger;
import com.braintribe.persistence.hibernate.dialects.HibernateDialectMapping;
import com.braintribe.util.jdbc.dialect.JdbcDialectAutoSense;
import com.braintribe.utils.StringTools;

/**
 * This class holds the list of {@link DialectMapping}s. It's main purpose is to increase the expressiveness of the configuration (for EM).
 * 
 * @author michael.lafite
 * 
 * @param <D>
 *            The dialect type
 */
public class DialectAutoSense {

	private static Logger logger = Logger.getLogger(DialectAutoSense.class);

	private Map<Pattern, Class<? extends Dialect>> patternMappings = new HashMap<>();

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

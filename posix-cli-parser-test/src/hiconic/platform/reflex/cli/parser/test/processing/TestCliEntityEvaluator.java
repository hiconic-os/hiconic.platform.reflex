package hiconic.platform.reflex.cli.parser.test.processing;

import java.util.LinkedHashMap;

import com.braintribe.gm.cli.posix.parser.api.CliEntityEvaluator;
import com.braintribe.model.generic.GenericEntity;

import hiconic.platform.reflex.cli.parser.test.model.ParseUrlQuery;

public class TestCliEntityEvaluator implements CliEntityEvaluator {

	@Override
	public boolean isEvaluable(GenericEntity entity) {
		return entity instanceof ParseUrlQuery;
	}

	@Override
	public Object evaluate(GenericEntity entity) {
		var parseUrlQuery = (ParseUrlQuery)entity;
		
		String entries[] = parseUrlQuery.getQuery().split("&");
		
		var map = new LinkedHashMap<String, String>();
		
		for (String entry: entries) {
			int index = entry.indexOf("=");
			
			final String key; 
			final String value;
			
			if (index != -1) {
				key = entry.substring(0, index);
				value = entry.substring(index + 1);
			}
			else {
				key = entry;
				value = null;
			}
			
			map.put(key, value);
		}
		
		return map;
	}

}

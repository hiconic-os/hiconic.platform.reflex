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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.MapType;

public class MapAccumulator extends CollectionAccumulator {

	private MapType mapType;
	private GenericModelType keyType;
	private GenericModelType valueType;

	public MapAccumulator(CliArgumentParser parser, Consumer<Object> consumer, MapType mapType) {
		super(parser, consumer, mapType);
		this.mapType = mapType;
		keyType = mapType.getKeyType();
		valueType = mapType.getValueType();
	}

	@Override
	protected GenericModelType inferredType() {
		return index % 2 == 0? keyType: valueType;
	}

	@Override
	protected Object buildValue(List<CliArgument> args) {
		Map<Object, Object> map = mapType.createPlain();
		
		int size = args.size();
		
		for (int i = 0; i < size;) {
			CliArgument keyArg = args.get(i++);
			Object key = keyArg.getValue();

			if (!keyType.isInstance(key))
				throw new ParseException("Error at " + keyArg.getDesc() + ": Map key type mismatch");
			
			if (!(i < size))
				throw new ParseException("Error at " + keyArg.getDesc() + ": Missing value for map key");
			
			CliArgument valueArg = args.get(i++);
			Object value = valueArg.getValue();

			if (!valueType.isInstance(value))
				throw new ParseException("Error at " + valueArg.getDesc() + ": Map value type mismatch");

			map.put(key, value);
		}
		
		return map;
	}
}

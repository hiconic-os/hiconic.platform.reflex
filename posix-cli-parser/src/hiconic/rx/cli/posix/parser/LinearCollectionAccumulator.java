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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;

public class LinearCollectionAccumulator extends CollectionAccumulator {

	private GenericModelType elementType;
	private LinearCollectionType linearCollectionType;

	public LinearCollectionAccumulator(CliArgumentParser parser, Consumer<Object> consumer, LinearCollectionType collectionType) {
		super(parser, consumer, collectionType);
		this.linearCollectionType = collectionType;
		elementType = collectionType.getCollectionElementType();
	}
	
	@Override
	protected GenericModelType inferredType() {
		return elementType;
	}

	@Override
	protected Object buildValue(List<CliArgument> args) {
		Collection<Object> collection = linearCollectionType.createPlain();
		
		for (CliArgument arg: args) {
			Object value = arg.getValue();
			if (!elementType.isInstance(value))
				throw new ParseException("Error at " + arg.getDesc() + ": Collection element type mismatch");
				
			collection.add(value);
		}
		return collection;
	}

}

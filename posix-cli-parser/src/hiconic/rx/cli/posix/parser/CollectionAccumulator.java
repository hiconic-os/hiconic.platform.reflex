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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.braintribe.model.generic.reflection.CollectionType;
import com.braintribe.model.generic.reflection.GenericModelType;

public abstract class CollectionAccumulator {
	private List<CliArgument> arguments = new ArrayList<>();
	
	private int forwardCount = 0;
	private Consumer<Object> consumer;
	protected int index;

	private CollectionType collectionType;

	private CliArgumentParser parser;
	
	protected abstract GenericModelType inferredType();
	protected abstract Object buildValue(List<CliArgument> values);
	
	protected CollectionAccumulator(CliArgumentParser parser, Consumer<Object> consumer, CollectionType collectionType) {
		this.parser = parser;
		this.consumer = consumer;
		this.collectionType = collectionType;
	}
	
	public void parse() {
		while (true) {
			CliArgument argument = parser.parseOptionalArgumentValue(inferredType());
			
			if (argument == null)
				break;
			
			arguments.add(argument);
			
			Object value = argument.getValue();
			
			if (value != null && value instanceof ForwardReference fr) {
				forwardCount++;
				fr.consumers.add(v -> resolve(argument, v));
			}
			
			index++;
		}
		
		buildAndNotifyIfComplete();
	}
	
	private void resolve(CliArgument argument, Object value) {
		argument.setValue(value);
		forwardCount--;
		buildAndNotifyIfComplete();
	}
	
	private void buildAndNotifyIfComplete() {
		if (forwardCount == 0) {
			if (arguments.size() == 1) {
				CliArgument argument = arguments.get(0);
				Object value = argument.getValue();
				if (collectionType.isInstance(value)) {
					consumer.accept(value);
					return;
				}
			}
			
			Object value = buildValue(arguments);
			consumer.accept(value);
		}
	}
	
}

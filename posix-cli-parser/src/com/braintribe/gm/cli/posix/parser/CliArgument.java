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
package com.braintribe.gm.cli.posix.parser;

public class CliArgument {
	
	private int num;
	private String input;
	private Object value;
	
	public CliArgument(int num, String input) {
		super();
		this.num = num;
		this.input = input;
	}
	
	void setValue(Object value) {
		this.value = value;
	}
	
	public int getNum() {
		return num;
	}
	
	public String getInput() {
		return input;
	}
	
	public Object getValue() {
		return value;
	}
	
	public String getDesc() {
		return "argument #" + num + " [" + input + "]";
	}
	
	@Override
	public String toString() {
		return getDesc();
	}
	
}

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
package hiconic.rx.cli.processing;

import java.io.PrintStream;
import java.util.function.Supplier;

import com.braintribe.console.AbstractAnsiConsole;

public class SuppliedPrintStreamConsole extends AbstractAnsiConsole  {
	private Supplier<PrintStream> printStreamSupplier;

	public SuppliedPrintStreamConsole(Supplier<PrintStream> printStreamSupplier) {
		this(printStreamSupplier, true);
	}
	
	public SuppliedPrintStreamConsole(Supplier<PrintStream> printStreamSupplier, boolean ansiConsole) {
		this(printStreamSupplier, ansiConsole, true);
	}
	
	public SuppliedPrintStreamConsole(Supplier<PrintStream> printStreamSupplier, boolean ansiConsole, boolean resetStyles) {
		super(ansiConsole, resetStyles);
		this.printStreamSupplier = printStreamSupplier;
	}

	@Override
	protected void _out(CharSequence text, boolean linebreak) {
		PrintStream ps = printStreamSupplier.get();
		
		if (linebreak)
			ps.println(text);
		else
			ps.print(text);
		
		ps.flush();
	}
}
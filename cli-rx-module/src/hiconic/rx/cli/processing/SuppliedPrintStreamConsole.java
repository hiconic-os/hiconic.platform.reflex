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
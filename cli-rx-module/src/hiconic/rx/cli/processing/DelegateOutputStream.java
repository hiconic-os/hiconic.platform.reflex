// ============================================================================
package hiconic.rx.cli.processing;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;

public class DelegateOutputStream extends OutputStream {
	private Supplier<? extends OutputStream> delegateSupplier;
	private boolean closeDelegate = false;

	public DelegateOutputStream(Supplier<? extends OutputStream> delegateSupplier) {
		super();
		this.delegateSupplier = delegateSupplier;
	}

	@Override
	public void write(int b) throws IOException {
		delegateSupplier.get().write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		delegateSupplier.get().write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		delegateSupplier.get().write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		delegateSupplier.get().flush();
	}

	@Override
	public void close() throws IOException {
		if (closeDelegate)
			delegateSupplier.get().close();
	}
}

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

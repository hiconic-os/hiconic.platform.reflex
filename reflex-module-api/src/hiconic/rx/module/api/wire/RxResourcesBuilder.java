// ============================================================================
package hiconic.rx.module.api.wire;

import java.io.UncheckedIOException;

import com.braintribe.codec.marshaller.api.MarshallException;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.model.resource.api.ResourceHandle;

/**
 * @see ResourceHandle
 */
public interface RxResourcesBuilder extends ResourceHandle {

	/**
	 * <p>
	 * Returns an unmarshalled object from the resource contents.
	 * 
	 * <p>
	 * Assumes the resource is a marshalled representation of a object compatible with the given {@link Marshaller}
	 * instance.
	 * 
	 * @param marshaller
	 *            The {@link Marshaller} to be used for unmarshalling the resource.
	 * @return An unmarshalled object from the resource contents.
	 * @throws UncheckedIOException
	 *             In case of IOException(s) while reading the resource contents.
	 * @throws MarshallException
	 *             Upon failures while unmarshalling the resource.
	 */
	<T> T asAssembly(Marshaller marshaller) throws UncheckedIOException, MarshallException;

	/**
	 * Similar to {@link #asAssembly(Marshaller)}, but returns the default value if the underlying resource (e.g. File)
	 * does not exist.
	 */
	<T> T asAssembly(Marshaller marshaller, T defaultValue);

}

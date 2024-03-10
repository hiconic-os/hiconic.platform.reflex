package hiconic.rx.module.api.wire;

/**
 * Enables modules to export {@link RxExportContract}s, which can then be imported by other modules.
 * 
 * @see RxExportContract
 */
public interface Exports {

	<E extends RxExportContract> void bind(Class<E> contractClass, E spaceInstance);

	<E extends RxExportContract, S extends E> void bind(Class<E> contractClass, Class<S> spaceClass);

}

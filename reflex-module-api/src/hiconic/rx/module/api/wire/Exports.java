package hiconic.rx.module.api.wire;

public interface Exports {

	<E extends RxExportContract> void bind(Class<E> wireSpaceContract, E wireSpaceInstance);

	<E extends RxExportContract, S extends E> void bind(Class<E> wireSpaceContract, Class<S> wireSpaceClass);

}

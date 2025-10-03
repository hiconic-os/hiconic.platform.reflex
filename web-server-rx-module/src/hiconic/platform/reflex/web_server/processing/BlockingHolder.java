package hiconic.platform.reflex.web_server.processing;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BlockingHolder<T> implements Consumer<T>, Supplier<T>  {
    private volatile T value;

    @Override
	public synchronized void accept(T newValue) {
        value = newValue;
        
        if (newValue != null) {
	        	synchronized (this) {
	            notifyAll();
	        }
        }
    }

    @Override
	public T get() {
        T v = value;
        
        if (v != null)
            return v;

        synchronized (this) {
            while (value == null) {
                try {
					wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
            }
            return value;
        }
    }

    public T peek() {
        return value;
    }
}

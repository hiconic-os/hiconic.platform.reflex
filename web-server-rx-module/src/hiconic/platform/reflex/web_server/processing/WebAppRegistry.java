package hiconic.platform.reflex.web_server.processing;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebAppRegistry {
	private final Set<String> paths = ConcurrentHashMap.newKeySet();

	public void register(String path) {
		paths.add(path);
	}

	public boolean isRegistered(String path) {
		return paths.contains(path);
	}
}

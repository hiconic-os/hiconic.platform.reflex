package hiconic.platform.reflex.web_server.processing;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import hiconic.rx.web.server.api.WebAppNavigationEntry;

public class WebAppRegistry {
	private final Set<String> paths = ConcurrentHashMap.newKeySet();
	private final Map<String, WebAppNavigationEntry> navigationEntries = new ConcurrentHashMap<>();

	public void register(String path) {
		paths.add(path);
	}

	public boolean isRegistered(String path) {
		return paths.contains(path);
	}

	public void registerNavigation(WebAppNavigationEntry entry) {
		navigationEntries.put(entry.webAppPath(), entry);
	}

	public List<WebAppNavigationEntry> navigation() {
		return navigationEntries.values().stream() //
				.filter(entry -> paths.contains(entry.webAppPath())) //
				.sorted(Comparator.comparingInt(WebAppNavigationEntry::order)
						.thenComparing(WebAppNavigationEntry::displayName, String.CASE_INSENSITIVE_ORDER)) //
				.toList();
	}
}

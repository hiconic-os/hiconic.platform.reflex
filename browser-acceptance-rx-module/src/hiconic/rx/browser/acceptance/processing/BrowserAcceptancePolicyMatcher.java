package hiconic.rx.browser.acceptance.processing;

import java.util.List;
import java.util.Set;
import hiconic.rx.browser.acceptance.model.configuration.BrowserAcceptancePolicy;

public class BrowserAcceptancePolicyMatcher {
	private final List<BrowserAcceptancePolicy> policies;
	public BrowserAcceptancePolicyMatcher(List<BrowserAcceptancePolicy> policies) { this.policies = policies == null ? List.of() : List.copyOf(policies); }
	public boolean applies(String entryPoint, Set<String> roles) {
		BrowserAcceptancePolicy matching = null;
		for (BrowserAcceptancePolicy policy : policies) {
			Set<String> entryPoints = policy.getEntryPoints();
			if (!entryPoints.isEmpty() && (entryPoint == null || !entryPoints.contains(entryPoint))) continue;
			if (matching != null) throw new IllegalStateException("Ambiguous browser acceptance policies for entry point: " + entryPoint);
			matching = policy;
		}
		if (matching == null) return false;
		if (intersects(roles, matching.getExcludedRoles())) return false;
		return matching.getIncludedRoles().isEmpty() || intersects(roles, matching.getIncludedRoles());
	}
	private static boolean intersects(Set<String> left, Set<String> right) { return left != null && right != null && left.stream().anyMatch(right::contains); }
}

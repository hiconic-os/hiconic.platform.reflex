// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.workbench.api;

import java.util.Objects;
import java.util.regex.Pattern;

import com.braintribe.wire.api.scope.InstanceQualification;

import hiconic.rx.module.api.wire.ModuleReflectionContract;

/** Stable, URL-friendly identity of a Wire-managed workbench initializer. */
public final class WorkbenchInitializerIdentity {

	private static final Pattern ARTIFACT_ID = Pattern.compile("[A-Za-z0-9_.-]+");
	private static final Pattern WIRE_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

	private final String prefix;

	private WorkbenchInitializerIdentity(String prefix) {
		this.prefix = prefix;
	}

	public static WorkbenchInitializerIdentity wire(ModuleReflectionContract module, InstanceQualification qualification) {
		Objects.requireNonNull(module, "module");
		Objects.requireNonNull(qualification, "qualification");

		String artifactId = requireArtifactId(module.artifactId());
		String spaceName = requireWireName("Wire space", qualification.space().getClass().getSimpleName());
		String beanName = requireWireName("Wire bean", qualification.name());
		return new WorkbenchInitializerIdentity(artifactId + "--" + spaceName + "." + beanName);
	}

	public String prefix() {
		return prefix;
	}

	private static String requireArtifactId(String value) {
		if (value == null || !ARTIFACT_ID.matcher(value).matches() || value.contains("--"))
			throw new IllegalArgumentException("Artifact id used for a workbench identity must match " + ARTIFACT_ID + " and not contain '--': "
					+ value);
		return value;
	}

	private static String requireWireName(String kind, String value) {
		if (value == null || !WIRE_NAME.matcher(value).matches())
			throw new IllegalArgumentException(kind + " used for a workbench identity must match " + WIRE_NAME + ": " + value);
		return value;
	}

}

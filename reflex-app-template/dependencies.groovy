import com.braintribe.devrock.templates.model.artifact.CreateBuildSystemConfig;
import com.braintribe.devrock.templates.model.artifact.CreateProjectMetadata;
import com.braintribe.devrock.templates.model.artifact.CreateSourceControlConfig;
import com.braintribe.devrock.templates.model.Dependency;

import hiconic.rx.setup.model.CreateReflexModule;

def buildSystemConfigRequest = CreateBuildSystemConfig.T.create();
support.mapFromTo(request, buildSystemConfigRequest);
buildSystemConfigRequest.artifactType = 'rx-application';

def applicationDep = Dependency.T.create();
applicationDep.groupId = 'hiconic.platform.reflex';
applicationDep.artifactId = 'reflex-platform';

buildSystemConfigRequest.dependencies = support.distinctDependencies([applicationDep] + buildSystemConfigRequest.dependencies);

if (request.isModule) {
	def rxModuleRequest = CreateReflexModule.T.create();
	support.mapFromTo(request, rxModuleRequest);

	return [rxModuleRequest, buildSystemConfigRequest]

} else {
	def projectMetadataRequest = CreateProjectMetadata.T.create();
	support.mapFromTo(request, projectMetadataRequest);
	projectMetadataRequest.projectName = request.artifactId + " - " + request.groupId;
	projectMetadataRequest.sourceDirectory = 'src';
	if ('eclipse'.equals(request.ide)) {
		projectMetadataRequest.classPathEntries = ["org.eclipse.jdt.launching.JRE_CONTAINER"];
		projectMetadataRequest.builders = ['com.braintribe.devrock.arb.builder.ArtifactReflectionBuilder', 'org.eclipse.jdt.core.javabuilder'];
		projectMetadataRequest.builderOutputLibs = ["class-gen"];
		projectMetadataRequest.natures  = ['org.eclipse.jdt.core.javanature'];
	}

	def sourceControlConfigRequest = CreateSourceControlConfig.T.create();
	support.mapFromTo(request, sourceControlConfigRequest);
	sourceControlConfigRequest.ignoredFiles = ['/' + projectMetadataRequest.outputDirectory];

	return [buildSystemConfigRequest, projectMetadataRequest, sourceControlConfigRequest]
}
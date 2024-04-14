import hiconic.rx.setup.model.CreateReflexModule;
import hiconic.rx.setup.model.CreateReflexApp;
import hiconic.rx.setup.model.RxEndpoint;
import hiconic.rx.setup.model.ReflexTemplateLocator
import com.braintribe.devrock.templates.model.Dependency;
import tribefire.cortex.assets.templates.model.CreateModel;

def modelName = request.artifactId + "-model";
def moduleName = request.artifactId + "-rx-module";
def appName = request.artifactId + "-app";

def createModelRequest = CreateModel.T.create();
support.mapFromTo(request, createModelRequest, /*exclude:*/ ['dependencies', 'directoryName']);
createModelRequest.template = ReflexTemplateLocator.template('reflex-model-template');
createModelRequest.artifactId = modelName;

def createModuleRequest = CreateReflexModule.T.create();
support.mapFromTo(request, createModuleRequest, /*exclude:*/ ['dependencies', 'directoryName']);
createModuleRequest.artifactId = moduleName;
createModuleRequest.sampleProcessor = true;
createModuleRequest.dependencies = [assetDependency(request.groupId, modelName)]

// if any dependencies are specified on the CreateReflexProject request, they are added to the App
def createAppRequest = CreateReflexApp.T.create();
support.mapFromTo(request, createAppRequest, /*exclude:*/ ['directoryName']);
createAppRequest.artifactId = appName;
createAppRequest.dependencies += [dependency(request.groupId, moduleName)]

if (request.endpoint == RxEndpoint.web) {
	createAppRequest.dependencies += [dependency('hiconic.platform.reflex', 'web-api-server-rx-module')]

} else if (request.endpoint == RxEndpoint.cli) {
	createAppRequest.dependencies += [dependency('hiconic.platform.reflex', 'cli-rx-module')]
}

support.ensureDependencyVersions( 
	'com.braintribe.gm', '[2.0,2.1)',
	'hiconic.platform.reflex', '[1.0,1.1)',
);

requestContext.eval(createModelRequest).get();
requestContext.eval(createModuleRequest).get();
requestContext.eval(createAppRequest).get();

return []

Dependency assetDependency(String groupId, String artifactId) {
	def dep = dependency(groupId, artifactId);
	dep.tags = ['asset'];
	return dep;
}

Dependency dependency(String groupId, String artifactId) {
	def dep = Dependency.T.create();
	dep.groupId = groupId;
	dep.artifactId = artifactId;
	return dep;
}
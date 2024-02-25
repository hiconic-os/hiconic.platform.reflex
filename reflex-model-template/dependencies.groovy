import tribefire.cortex.assets.templates.model.CreateModel;
import com.braintribe.devrock.templates.model.Dependency;

def createModelRequest = CreateModel.T.create();
support.mapFromTo(request, createModelRequest, /*exclude:*/ ['template']);
createModelRequest.api = true;

return [createModelRequest];
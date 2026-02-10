// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.access.module.processing;

import java.util.List;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GmReflectionTools;
import com.braintribe.model.processing.accessrequest.api.AbstractDispatchingAccessRequestProcessor;
import com.braintribe.model.processing.accessrequest.api.AccessRequestContext;
import com.braintribe.model.processing.accessrequest.api.DispatchConfiguration;
import com.braintribe.model.processing.service.api.ResponseConsumerAspect;
import com.braintribe.model.processing.session.api.common.GmSessions;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.resourceapi.base.ResourceRequest;
import com.braintribe.model.resourceapi.persistence.DeleteResource;
import com.braintribe.model.resourceapi.persistence.DeleteResourceResponse;
import com.braintribe.model.resourceapi.persistence.DeletionScope;
import com.braintribe.model.resourceapi.persistence.UpdateResource;
import com.braintribe.model.resourceapi.persistence.UploadResource;
import com.braintribe.model.resourceapi.persistence.UploadResourceResponse;
import com.braintribe.model.resourceapi.persistence.UploadResources;
import com.braintribe.model.resourceapi.persistence.UploadResourcesResponse;
import com.braintribe.model.resourceapi.stream.BinaryRetrievalResponse;
import com.braintribe.model.resourceapi.stream.DownloadResource;
import com.braintribe.model.resourceapi.stream.GetBinaryResponse;
import com.braintribe.model.resourceapi.stream.GetResource;
import com.braintribe.model.resourceapi.stream.StreamResource;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.access.model.md.PreEnrichResourceWith;
import hiconic.rx.access.module.api.ResourceEnricher;
import hiconic.rx.resource.model.api.DeleteResourcePayload;
import hiconic.rx.resource.model.api.DeleteResourcePayloadResponse;
import hiconic.rx.resource.model.api.DownloadResourcePayload;
import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.resource.model.api.GetResourcePayloadResponse;
import hiconic.rx.resource.model.api.PipeResourcePayload;
import hiconic.rx.resource.model.api.PipeResourcePayloadResponse;
import hiconic.rx.resource.model.api.StoreResourcePayload;
import hiconic.rx.resource.model.api.StoreResourcePayloadResponse;

/**
 * Central processor of {@link ResourceRequest}s.
 */
public class ResourceRequestProcessor extends AbstractDispatchingAccessRequestProcessor<ResourceRequest, BinaryRetrievalResponse> {

	private Evaluator<ServiceRequest> systemEvaluator;

	@Required
	public void setSystemEvaluator(Evaluator<ServiceRequest> systemEvaluator) {
		this.systemEvaluator = systemEvaluator;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration dispatching) {
		dispatching.registerReasoned(GetResource.T, this::get);
		dispatching.registerReasoned(StreamResource.T, this::stream);

		dispatching.register(UploadResources.T, this::bulkUpload);
		dispatching.register(UploadResource.T, this::upload);
		dispatching.register(UpdateResource.T, this::update);
		dispatching.registerReasoned(DeleteResource.T, this::delete);
	}

	// ################################################
	// ## . . . . . . DownloadResource . . . . . . . ##
	// ################################################

	private Maybe<GetBinaryResponse> get(AccessRequestContext<GetResource> context) {
		GetResource request = context.getOriginalRequest();

		// Query resource
		Maybe<Resource> resourceMaybe = queryResource(context, request.getResource());
		if (resourceMaybe.isUnsatisfied())
			return resourceMaybe.propagateReason();

		// Get the payload
		GetResourcePayload getPayload = buildDlRequest(context, GetResourcePayload.T, resourceMaybe.get());

		Maybe<? extends GetResourcePayloadResponse> responseMaybe = getPayload.eval(systemEvaluator).getReasoned();

		if (responseMaybe.isUnsatisfied())
			return responseMaybe.propagateReason();

		// Prepare response
		GetResourcePayloadResponse response = responseMaybe.get();

		GetBinaryResponse result = GetBinaryResponse.T.create();
		result.setCacheControl(response.getCacheControl());
		result.setRanged(response.getRanged());
		result.setResource(response.getResource());

		return Maybe.complete(result);
	}

	private Maybe<PipeResourcePayloadResponse> stream(AccessRequestContext<StreamResource> context) {
		StreamResource request = context.getOriginalRequest();

		// Query resource
		Maybe<Resource> resourceMaybe = queryResource(context, request.getResource());
		if (resourceMaybe.isUnsatisfied())
			return resourceMaybe.propagateReason();

		// Stream the payload
		PipeResourcePayload pipePayload = buildDlRequest(context, PipeResourcePayload.T, resourceMaybe.get());
		pipePayload.setCapture(request.getCapture());

		Maybe<? extends PipeResourcePayloadResponse> responseMaybe = pipePayload.eval(systemEvaluator) //
				.with(ResponseConsumerAspect.class, context::notifyResponse) //
				.getReasoned();

		if (responseMaybe.isUnsatisfied())
			return responseMaybe.propagateReason();

		// Prepare response
		PipeResourcePayloadResponse response = responseMaybe.get();

		PipeResourcePayloadResponse result = PipeResourcePayloadResponse.T.create();
		result.setCacheControl(response.getCacheControl());
		result.setRanged(response.getRanged());
		result.setStreamed(result.getStreamed());

		return Maybe.complete(result);
	}

	private <T extends DownloadResourcePayload> T buildDlRequest( //
			AccessRequestContext<? extends DownloadResource> context, EntityType<T> et, Resource resource) {

		DownloadResource request = context.getOriginalRequest();

		T result = et.create();
		result.setDomainId(context.getDomainId());
		result.setCondition(request.getCondition());
		result.setRange(request.getRange());
		result.setResourceSource(resource.getResourceSource());
		result.setMd5(resource.getMd5());
		result.setCreated(resource.getCreated());

		return result;
	}
	private Maybe<Resource> queryResource(AccessRequestContext<?> context, Resource r) {
		// This also verifies user has the right to access the resource
		Resource result = context.getSession().query().entity(r).find();

		if (result != null)
			return Maybe.complete(result);
		else
			return Reasons.build(NotFound.T).text("Resource not found for id: " + r.getId()).toMaybe();
	}

	// ################################################
	// ## . . . . . . . ManageResource . . . . . . . ##
	// ################################################

	private UploadResourcesResponse bulkUpload(AccessRequestContext<UploadResources> context) {
		UploadResources originalRequest = context.getOriginalRequest();
		Boolean detectMimeType = originalRequest.getDetectMimeType();

		UploadResourcesResponse response = UploadResourcesResponse.T.create();
		List<Resource> responseResources = response.getResources();

		for (Resource resource : originalRequest.getResources()) {
			UploadResource uploadResource = UploadResource.T.create();
			uploadResource.setDomainId(context.getDomainId());
			uploadResource.setResource(resource);
			uploadResource.setSourceType(originalRequest.getSourceType());
			uploadResource.setUseCase(originalRequest.getUseCase());

			UploadResourceResponse uploadResourceResponse = evalUploadResource(context.getSession(), resource, uploadResource, detectMimeType);

			responseResources.add(uploadResourceResponse.getResource());
		}

		return response;
	}

	private UploadResourceResponse evalUploadResource(PersistenceGmSession session, Resource resource, UploadResource uploadResource,
			Boolean detectMimeType) {

		String requestResourceMimeType = resource.getMimeType();

		if (Boolean.TRUE.equals(detectMimeType)) {
			resource.setMimeType(requestResourceMimeType);
		} else if (Boolean.FALSE.equals(detectMimeType) && resource.getMimeType() == null) {
			resource.setMimeType("application/octet-stream");
		}

		UploadResourceResponse uploadResourceResponse = uploadResource.eval(session).get();

		Resource responseResource = uploadResourceResponse.getResource();

		if (Boolean.TRUE.equals(detectMimeType) && responseResource != null && responseResource.getMimeType() == null)
			responseResource.setMimeType(requestResourceMimeType);

		return uploadResourceResponse;
	}

	private Maybe<UploadResourceResponse> upload(AccessRequestContext<UploadResource> context) {
		UploadResource request = context.getOriginalRequest();

		Resource resource = GmReflectionTools.makeShallowCopy(request.getResource());

		Maybe<Void> uploadMaybe = uploadAndSetResourceSource(request, resource, context);
		if (uploadMaybe.isUnsatisfied())
			return uploadMaybe.propagateReason();

		Resource clonedResource = GmSessions.cloneIntoSession(resource, context.getSession());

		UploadResourceResponse response = UploadResourceResponse.T.create();
		response.setResource(clonedResource);

		return Maybe.complete(response);
	}

	private Maybe<UploadResourceResponse> update(AccessRequestContext<UpdateResource> context) {
		PersistenceGmSession session = context.getSession();
		UpdateResource request = context.getOriginalRequest();
		Resource resource = GmReflectionTools.makeShallowCopy(request.getResource());

		// Upload payload

		Maybe<Void> responseMaybe = uploadAndSetResourceSource(request, resource, context);
		if (responseMaybe.isUnsatisfied())
			return responseMaybe.propagateReason();

		// Query existing resource

		Maybe<Resource> resourceMaybe = queryResource(context, request.getResource());
		if (resourceMaybe.isUnsatisfied())
			return resourceMaybe.propagateReason();

		Resource persistedResource = resourceMaybe.get();

		// Delete source of existing resource if needed

		if (request.getDeleteOldResourceSource())
			deleteHelper(persistedResource, DeletionScope.source, context);

		// Transfer MD

		transferMetadata(resource, persistedResource);

		ResourceSource clonedResourceSource = GmSessions.cloneIntoSession(resource.getResourceSource(), session);
		persistedResource.setResourceSource(clonedResourceSource);

		// Return response

		UploadResourceResponse response = UploadResourceResponse.T.create();
		response.setResource(persistedResource);

		return Maybe.complete(response);
	}

	private Maybe<Void> uploadAndSetResourceSource(UploadResource request, Resource resource, AccessRequestContext<?> context) {
		PersistenceGmSession session = context.getSession();

		EntityType<? extends ResourceSource> sourceType = resolveSourceType(request.getSourceType());
		if (sourceType == null)
			return notFoundMaybe("No ResourceSource type found with signature: " + request.getSourceType());

		String requestUseCase = request.getUseCase();

		List<ResourceEnricher> preEnrichers = resovePreEnrichers(session, sourceType, requestUseCase);
		preEnrich(preEnrichers, resource);

		StoreResourcePayload storePayload = StoreResourcePayload.T.create();
		storePayload.setDomainId(context.getDomainId());
		storePayload.setData(resource);
		storePayload.setUseCase(requestUseCase);

		Maybe<? extends StoreResourcePayloadResponse> responseMaybe = storePayload.eval(systemEvaluator).getReasoned();
		if (responseMaybe.isUnsatisfied())
			return responseMaybe.propagateReason();

		ResourceSource resourceSource = responseMaybe.get().getResourceSource();
		if (requestUseCase != null)
			if (resourceSource.getUseCase() == null)
				resourceSource.setUseCase(requestUseCase);

		resource.setResourceSource(resourceSource);

		return Maybe.complete(null);
	}

	private EntityType<? extends ResourceSource> resolveSourceType(String sourceTypeSignature) {
		if (sourceTypeSignature == null)
			return ResourceSource.T;
		else
			return GMF.getTypeReflection().findEntityType(sourceTypeSignature);
	}

	private List<ResourceEnricher> resovePreEnrichers(PersistenceGmSession session, EntityType<? extends ResourceSource> sourceType, String useCase) {
		List<PreEnrichResourceWith> mds = session.getModelAccessory().getCmdResolver().getMetaData() //
				.entityType(sourceType) //
				.useCase(useCase) //
				.meta(PreEnrichResourceWith.T) //
				.list();

		return mds.stream() //
				.map(md -> md.<ResourceEnricher> getResourceEnricher()) //
				.toList();
	}

	private void preEnrich(List<ResourceEnricher> preEnrichers, Resource resource) {
		for (ResourceEnricher enricher : preEnrichers)
			enricher.enrich(resource);
	}

	private Maybe<DeleteResourceResponse> delete(AccessRequestContext<DeleteResource> context) {
		DeleteResource request = context.getOriginalRequest();

		Maybe<Resource> resourceMaybe = queryResource(context, request.getResource());
		if (resourceMaybe.isUnsatisfied())
			return resourceMaybe.propagateReason();

		return deleteHelper(resourceMaybe.get(), request.getDeletionScope(), context);
	}

	private Maybe<DeleteResourceResponse> deleteHelper(Resource resource, DeletionScope deletionScope, AccessRequestContext<?> context) {
		ResourceSource resourceSource = resource.getResourceSource();
		if (resourceSource == null)
			return Reasons.build(NotFound.T).text("Resource has no source: " + resource).toMaybe();

		PersistenceGmSession session = context.getSession();

		DeleteResourcePayload deleteRequest = DeleteResourcePayload.T.create();
		deleteRequest.setDomainId(context.getDomainId());
		deleteRequest.setResourceSource(resourceSource);

		Maybe<? extends DeleteResourcePayloadResponse> responseMaybe = deleteRequest.eval(systemEvaluator).getReasoned();
		if (responseMaybe.isUnsatisfied())
			return responseMaybe.propagateReason();

		if (deletionScope != null) {
			switch (deletionScope) {
				case resource:
					session.deleteEntity(resource);
					//$FALL-THROUGH$
				case source:
					session.deleteEntity(resourceSource);
					break;
				default:
					// Do nothing
			}
		}

		return Maybe.complete(DeleteResourceResponse.T.create());
	}

	private void transferMetadata(Resource source, Resource target) {
		target.setMd5(source.getMd5());
		target.setFileSize(source.getFileSize());
		target.setMimeType(source.getMimeType());

		if (source.getName() != null)
			target.setName(source.getName());

		if (source.getCreated() != null)
			target.setCreated(source.getCreated());

		if (source.getCreator() != null)
			target.setCreator(source.getCreator());

		if (source.getSpecification() != null) {
			PersistenceGmSession targetSession = (PersistenceGmSession) target.session();
			target.setSpecification(GmSessions.cloneIntoSession(source.getSpecification(), targetSession));
		}

		if (!source.getTags().isEmpty())
			target.getTags().addAll(source.getTags());
	}

	private static <T> Maybe<T> notFoundMaybe(String text) {
		return Reasons.build(NotFound.T).text(text).toMaybe();
	}
}

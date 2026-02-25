// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.access.module.processing.resource;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.model.access.HasAccessId;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.session.InputStreamProvider;
import com.braintribe.model.generic.session.OutputStreamProvider;
import com.braintribe.model.generic.session.OutputStreamer;
import com.braintribe.model.generic.stream.StreamProviders;
import com.braintribe.model.processing.service.api.ResponseConsumerAspect;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.resource.ResourceAccess;
import com.braintribe.model.processing.session.api.resource.ResourceCreateBuilder;
import com.braintribe.model.processing.session.api.resource.ResourceDeleteBuilder;
import com.braintribe.model.processing.session.api.resource.ResourceRetrieveBuilder;
import com.braintribe.model.processing.session.api.resource.ResourceUpdateBuilder;
import com.braintribe.model.processing.session.api.resource.ResourceUrlBuilder;
import com.braintribe.model.resource.CallStreamCapture;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.resource.specification.ResourceSpecification;
import com.braintribe.model.resourceapi.persistence.DeleteResource;
import com.braintribe.model.resourceapi.persistence.DeleteResourceResponse;
import com.braintribe.model.resourceapi.persistence.DeletionScope;
import com.braintribe.model.resourceapi.persistence.UpdateResource;
import com.braintribe.model.resourceapi.persistence.UploadResource;
import com.braintribe.model.resourceapi.persistence.UploadResourceResponse;
import com.braintribe.model.resourceapi.stream.BinaryRetrievalResponse;
import com.braintribe.model.resourceapi.stream.GetBinaryResponse;
import com.braintribe.model.resourceapi.stream.GetResource;
import com.braintribe.model.resourceapi.stream.StreamBinaryResponse;
import com.braintribe.model.resourceapi.stream.StreamResource;
import com.braintribe.model.resourceapi.stream.condition.StreamCondition;
import com.braintribe.model.resourceapi.stream.range.StreamRange;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipeFactory;

/**
 * <p>
 * A {@link ServiceRequest}-evaluating {@link ResourceAccess}.
 * 
 */
public class RxResourceAccess implements ResourceAccess {

	protected PersistenceGmSession gmSession;
	protected String accessId;
	protected Function<Resource, ? extends ResourceUrlBuilder> urlBuilderSupplier;
	private StreamPipeFactory streamPipeFactory;
	private boolean shallowifyRequestResource;

	private static final Logger log = Logger.getLogger(RxResourceAccess.class);

	protected RxResourceAccess(PersistenceGmSession gmSession) {

		Objects.requireNonNull(gmSession, "gmSession must not be null");

		this.gmSession = gmSession;

		initialize();

	}

	@Configurable
	@Required
	public void setStreamPipeFactory(StreamPipeFactory streamPipeFactory) {
		this.streamPipeFactory = streamPipeFactory;
	}

	@Configurable
	public void setShallowifyRequestResource(boolean shallowifyRequestResource) {
		this.shallowifyRequestResource = shallowifyRequestResource;
	}

	protected void setUrlBuilderSupplier(Function<Resource, ? extends ResourceUrlBuilder> urlBuilderSupplier) {
		this.urlBuilderSupplier = urlBuilderSupplier;
	}

	protected void initialize() {

		if (gmSession instanceof HasAccessId)
			accessId = ((HasAccessId) gmSession).getAccessId();

		if (accessId == null)
			throw new IllegalStateException("The underlying " + gmSession.getClass().getSimpleName() + " provided no access id.");

		if (log.isTraceEnabled())
			log.trace(getClass().getName() + " initialized with access id [ " + accessId + " ]");
	}

	@Override
	public ResourceUrlBuilder url(Resource resource) {

		if (urlBuilderSupplier == null)
			throw new UnsupportedOperationException("URL building is not supported as no URL builder is configured");

		Objects.requireNonNull(resource, "resource must not be null");

		ResourceUrlBuilder urlBuilder = urlBuilderSupplier.apply(resource);
		urlBuilder.accessId(accessId);

		return urlBuilder;

	}

	@Override
	public ResourceCreateBuilder create() {
		return new ResourceCreateBuilderImpl();

	}

	@Override
	public ResourceUpdateBuilder update(Resource resource) {
		Objects.requireNonNull(resource, "resource must not be null");

		return new ResourceUpdateBuilderImpl(resource);
	}

	@Override
	public ResourceRetrieveBuilder retrieve(final Resource resource) {

		Objects.requireNonNull(resource, "resource must not be null");

		return new ResourceRetrieveBuilder() {

			private StreamCondition condition;
			private Consumer<BinaryRetrievalResponse> consumer;
			private StreamRange range;

			@Override
			public ResourceRetrieveBuilder condition(StreamCondition condition) {
				this.condition = condition;
				return this;
			}

			@Override
			public ResourceRetrieveBuilder onResponse(Consumer<BinaryRetrievalResponse> consumer) {
				this.consumer = consumer;
				return this;
			}

			@Override
			public void stream(OutputStream outputStream) {
				Objects.requireNonNull(outputStream, "outputStream must not be null");
				writeToStream(resource, condition, range, consumer, StreamProviders.from(outputStream));
			}

			@Override
			public void stream(OutputStreamProvider outputStreamProvider) {
				Objects.requireNonNull(outputStreamProvider, "outputStreamProvider must not be null");
				writeToStream(resource, condition, range, consumer, outputStreamProvider);
			}

			@Override
			public InputStream stream() {
				return openStream(resource, condition, range, consumer);
			}

			@Override
			public ResourceRetrieveBuilder range(StreamRange range) {
				this.range = range;
				return this;
			}

		};

	}

	@Override
	public ResourceDeleteBuilder delete(final Resource resource) {

		return new ResourceDeleteBuilder() {

			private String useCase;
			private DeletionScope scope;

			@Override
			public ResourceDeleteBuilder useCase(String useCase) {
				this.useCase = useCase;
				return this;
			}

			@Override
			public ResourceDeleteBuilder scope(DeletionScope scope) {
				this.scope = scope;
				return this;
			}

			@Override
			public void delete() {
				RxResourceAccess.this.delete(resource, scope);
			}

		};

	}

	protected <T> T evaluate(EvalContext<T> evalContext) {
		return evalContext.get();
	}

	protected Resource create(EntityType<? extends ResourceSource> sourceType, String useCase, String mimeType, String md5, Set<String> tags,
			String resourceName, String creator, ResourceSpecification specification, InputStreamProvider inputStreamProvider) {
		return _create(sourceType, useCase, mimeType, md5, tags, resourceName, creator, specification,
				() -> Resource.createTransient(inputStreamProvider));
	}

	protected Resource create(EntityType<? extends ResourceSource> sourceType, String useCase, String mimeType, String md5, Set<String> tags,
			String resourceName, String creator, ResourceSpecification specification, OutputStreamer streamer) {
		return _create(sourceType, useCase, mimeType, md5, tags, resourceName, creator, specification,
				() -> Resource.createTransient(StreamProviders.from(streamer, streamPipeFactory)));
	}

	private Resource _create(EntityType<? extends ResourceSource> sourceType, String useCase, String mimeType, String md5, Set<String> tags,
			String resourceName, String creator, ResourceSpecification specification, Supplier<Resource> resourceFactory) {

		Resource resource = resourceFactory.get();
		resource.setName(resourceName);
		resource.setMimeType(mimeType);
		resource.setMd5(md5);
		resource.setSpecification(specification);
		resource.setTags(tags);
		resource.setCreator(creator);

		UploadResource request = UploadResource.T.create();

		request.setResource(resource);
		request.setUseCase(useCase);

		if (sourceType != null) {
			request.setSourceType(sourceType.getTypeSignature());
		}
		EvalContext<? extends UploadResourceResponse> evalContext = request.eval(gmSession);

		UploadResourceResponse response = evaluate(evalContext);

		return response.getResource();

	}

	private Resource _update(EntityType<? extends ResourceSource> sourceType, String useCase, Resource resource, boolean deleteOldResourceSource) {
		UpdateResource request = UpdateResource.T.create();

		request.setDeleteOldResourceSource(deleteOldResourceSource);
		request.setResource(resource);
		request.setUseCase(useCase);

		if (sourceType != null) {
			request.setSourceType(sourceType.getTypeSignature());
		}

		EvalContext<? extends UploadResourceResponse> evalContext = request.eval(gmSession);

		UploadResourceResponse response = evaluate(evalContext);

		return response.getResource();

	}

	private void delete(Resource resource, DeletionScope scope) {

		DeleteResource request = DeleteResource.T.create();
		request.setResource(shallowClone(resource));
		request.setDeletionScope(scope);

		EvalContext<? extends DeleteResourceResponse> evalContext = request.eval(gmSession);

		evaluate(evalContext);

	}

	private void writeToStream(Resource resource, StreamCondition condition, StreamRange range, Consumer<BinaryRetrievalResponse> consumer,
			OutputStreamProvider outputStreamProvider) {

		CallStreamCapture streamCapture = CallStreamCapture.T.create();
		streamCapture.setGlobalId(UUID.randomUUID().toString());
		streamCapture.setOutputStreamProvider(outputStreamProvider);

		StreamResource request = StreamResource.T.create();
		request.setResource(shallowClone(resource));
		request.setCondition(condition);
		request.setCapture(streamCapture);
		request.setRange(range);

		EvalContext<? extends StreamBinaryResponse> evalContext = request.eval(gmSession);

		if (consumer != null) {
			evalContext.with(ResponseConsumerAspect.class, consumer);
		}

		evaluate(evalContext);

	}

	private InputStream openStream(Resource resource, StreamCondition condition, StreamRange range, Consumer<BinaryRetrievalResponse> consumer) {

		GetResource request = GetResource.T.create();
		request.setResource(shallowClone(resource));
		request.setCondition(condition);
		request.setRange(range);

		EvalContext<? extends GetBinaryResponse> evalContext = request.eval(gmSession);

		if (consumer == null) {

			GetBinaryResponse response = evaluate(evalContext);

			return response.getResource().openStream();

		} else {

			InputStreamConsumer inputConsumer = new InputStreamConsumer();

			evalContext.with(ResponseConsumerAspect.class, consumer.andThen(inputConsumer));

			evaluate(evalContext);

			return inputConsumer.stream();

		}

	}

	private final class ResourceUpdateBuilderImpl implements ResourceUpdateBuilder {
		protected String useCase;
		protected EntityType<? extends ResourceSource> sourceType;
		protected boolean deleteOldResourceSource = true;

		private final Resource resource;

		public ResourceUpdateBuilderImpl(Resource resource) {
			this.resource = Resource.T.create();
			this.resource.setId(resource.getId());
			this.resource.setPartition(resource.getPartition());
			this.resource.setGlobalId(resource.getGlobalId());
		}

		@Override
		public ResourceUpdateBuilder deleteOldResourceSource(boolean keep) {
			deleteOldResourceSource = keep;
			return this;
		}

		@Override
		public ResourceUpdateBuilder useCase(String useCase) {
			this.useCase = useCase;
			return this;
		}

		@Override
		public ResourceUpdateBuilder sourceType(EntityType<? extends ResourceSource> sourceType) {
			this.sourceType = sourceType;
			return this;
		}

		@Override
		public ResourceUpdateBuilder name(String resourceName) {
			resource.setName(resourceName);
			return this;
		}
		@Override
		public ResourceUpdateBuilder mimeType(String mimeType) {
			resource.setMimeType(mimeType);
			return this;
		}
		@Override
		public ResourceUpdateBuilder md5(String md5) {
			resource.setMd5(md5);
			return this;
		}
		@Override
		public ResourceUpdateBuilder tags(Set<String> tags) {
			resource.setTags(tags);
			return this;
		}
		@Override
		public ResourceUpdateBuilder specification(ResourceSpecification specification) {
			resource.setSpecification(specification);
			return this;
		}
		@Override
		public ResourceUpdateBuilder creator(String creator) {
			resource.setCreator(creator);
			return this;
		}
		@Override
		public Resource store(InputStream inputStream) {
			Objects.requireNonNull(inputStream, "inputStream must not be null");
			StreamPipe fileBackedPipe = streamPipeFactory.newPipe(getClass().getSimpleName() + "-" + accessId);
			fileBackedPipe.feedFrom(inputStream);
			resource.assignTransientSource(fileBackedPipe::openInputStream);
			return _update(sourceType, useCase, resource, deleteOldResourceSource);
		}
		@Override
		public Resource store(InputStreamProvider inputStreamProvider) {
			Objects.requireNonNull(inputStreamProvider, "inputStreamProvider must not be null");
			resource.assignTransientSource(inputStreamProvider);
			return _update(sourceType, useCase, resource, deleteOldResourceSource);
		}
		@Override
		public Resource store(OutputStreamer streamer) {
			Objects.requireNonNull(streamer, "streamer must not be null");
			resource.assignTransientSource(StreamProviders.from(streamer, streamPipeFactory));
			return _update(sourceType, useCase, resource, deleteOldResourceSource);
		}
	}

	private class ResourceCreateBuilderImpl implements ResourceCreateBuilder {
		protected String useCase;
		protected EntityType<? extends ResourceSource> sourceType;
		protected String name;
		protected String mimeType;
		protected String md5;
		protected ResourceSpecification specification;
		protected Set<String> tags;
		protected String creator;

		@Override
		public ResourceCreateBuilder useCase(String useCase) {
			this.useCase = useCase;
			return this;
		}
		@Override
		public ResourceCreateBuilder sourceType(EntityType<? extends ResourceSource> sourceType) {
			this.sourceType = sourceType;
			return this;
		}
		@Override
		public ResourceCreateBuilder name(String resourceName) {
			this.name = resourceName;
			return this;
		}
		@Override
		public ResourceCreateBuilder mimeType(String mimeType) {
			this.mimeType = mimeType;
			return this;
		}
		@Override
		public ResourceCreateBuilder md5(String md5) {
			this.md5 = md5;
			return this;
		}
		@Override
		public ResourceCreateBuilder tags(Set<String> tags) {
			this.tags = tags;
			return this;
		}
		@Override
		public ResourceCreateBuilder specification(ResourceSpecification specification) {
			this.specification = specification;
			return this;
		}
		@Override
		public ResourceCreateBuilder creator(String creator) {
			this.creator = creator;
			return this;
		}
		@Override
		public Resource store(InputStream inputStream) {
			Objects.requireNonNull(inputStream, "inputStream must not be null");
			StreamPipe fileBackedPipe = streamPipeFactory.newPipe(getClass().getSimpleName() + "-" + accessId);
			fileBackedPipe.feedFrom(inputStream);
			return create(sourceType, useCase, mimeType, md5, tags, name, creator, specification, fileBackedPipe::openInputStream);
		}
		@Override
		public Resource store(InputStreamProvider inputStreamProvider) {
			Objects.requireNonNull(inputStreamProvider, "inputStreamProvider must not be null");
			return create(sourceType, useCase, mimeType, md5, tags, name, creator, specification, inputStreamProvider);
		}
		@Override
		public Resource store(OutputStreamer streamer) {
			Objects.requireNonNull(streamer, "streamer must not be null");
			return create(sourceType, useCase, mimeType, md5, tags, name, creator, specification, streamer);
		}
	}

	protected static class InputStreamConsumer implements Consumer<BinaryRetrievalResponse> {

		private InputStream input;

		@Override
		public void accept(BinaryRetrievalResponse response) {
			input = ((GetBinaryResponse) response).getResource().openStream();
		}

		private InputStream stream() {
			return input;
		}

	}

	private <T extends GenericEntity> T shallowClone(T original) {
		if (!shallowifyRequestResource)
			return original;

		T clone = (T) original.entityType().create();

		for (Property p : original.entityType().getProperties()) {
			if (p.isIdentifying())
				p.set(clone, p.get(original));
			else
				p.setAbsenceInformation(clone, GMF.absenceInformation());
		}

		return clone;
	}

}

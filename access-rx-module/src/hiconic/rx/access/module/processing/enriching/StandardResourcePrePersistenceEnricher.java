// ============================================================================
package hiconic.rx.access.module.processing.enriching;

// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.braintribe.cfg.Configurable;
import com.braintribe.mimetype.MimeTypeDetector;
import com.braintribe.mimetype.PlatformMimeTypeDetector;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.specification.ResourceSpecification;
import com.braintribe.utils.lcd.NullSafe;
import com.braintribe.utils.stream.CountingInputStream;
import com.braintribe.utils.stream.WriteOnReadInputStream;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipeFactory;

import hiconic.rx.access.module.api.ResourceEnricher;
import hiconic.rx.access.module.api.ResourceSpecificationDetector;

/**
 * A standard {@link ResourceEnricher} for enriching {@link Resource}(s) before they are persisted.
 * <p>
 * The following properties are set by this {@link ResourceEnricher}:
 * 
 * <ul>
 * <li>{@link Resource#setId(Object)}</li>
 * <li>{@link Resource#setMd5(String)}</li>
 * <li>{@link Resource#setMimeType(String)}</li>
 * <li>{@link Resource#setFileSize(Long)}</li>
 * <li>{@link Resource#setCreated(java.util.Date)}</li>
 * <li>{@link Resource#setCreator(String)}</li>
 * </ul>
 *
 * COPIED FROM CORTEX:<br/>
 * Currently the StandardResourcePrePersistenceEnricher supports enriching of
 * {@link Resource#setSpecification(ResourceSpecification)} as well because of optimization reasons. As soon as there is
 * a better way to cache resource streams this feature will be extracted into a separate enricher implementation.
 * 
 */
public class StandardResourcePrePersistenceEnricher implements ResourceEnricher, StandardResourceProcessor {

	private ResourceSpecificationDetector<?> specificationDetector;
	private StreamPipeFactory streamPipeFactory;
	private MimeTypeDetector detector = PlatformMimeTypeDetector.instance;
	private int consumerBufferSize = 8096;
	private String digestAlgorithm = "MD5";

	@Configurable
	public void setSpecificationDetector(ResourceSpecificationDetector<?> specificationDetector) {
		this.specificationDetector = Objects.requireNonNull(specificationDetector, "specificationDetector cannot be set to null");
	}

	@Configurable
	public void setStreamPipeFactory(StreamPipeFactory streamPipeFactory) {
		this.streamPipeFactory = Objects.requireNonNull(streamPipeFactory, "streamPipeFactory cannot be set to null");
	}

	@Configurable
	public void setMimeTypeDetector(MimeTypeDetector mimeTypeDetector) {
		this.detector = NullSafe.nonNull(mimeTypeDetector, "mimeTypeDetector");
	}

	@Configurable
	public void setConsumerBufferSize(int consumerBufferSize) {
		Objects.requireNonNull(consumerBufferSize, "consumerBufferSize cannot be set to null");
		this.consumerBufferSize = consumerBufferSize;
	}

	@Configurable
	public void setDigestAlgorithm(String digestAlgorithm) {
		Objects.requireNonNull(digestAlgorithm, "digestAlgorithm cannot be set to null");
		this.digestAlgorithm = digestAlgorithm;
	}

	@Override
	public boolean enrich(Resource resource, ServiceRequestContext context) {
		boolean enrichId = unset(resource.getId());
		boolean enrichMd5 = unset(resource.getMd5());
		boolean enrichMimeType = unset(resource.getMimeType());
		boolean enrichSize = resource.getFileSize() == null;
		boolean enrichCreated = resource.getCreated() == null;
		boolean enrichCreator = unset(resource.getCreator());
		boolean enrichSpecification = resource.getSpecification() == null && specificationDetector != null;

		if (!(enrichId || enrichMd5 || enrichMimeType || enrichSize || enrichCreated || enrichCreator || enrichSpecification)) {
			return false;
		}

		// Something will be enriched at this point. We set the resource to the response.

		if (enrichId) {
			if (resource.getGlobalId() != null) {
				resource.setId(resource.getGlobalId());
			} else {
				resource.setId(UUID.randomUUID().toString());
			}
		}

		if (enrichCreated) {
			resource.setCreated(new Date());
		}

		if (enrichCreator) {
			String creator = context.getRequestorUserName();
			if (!unset(creator)) {
				resource.setCreator(creator);
			}
		}

		boolean streamForBasicEnrichment = enrichMd5 || enrichMimeType || enrichSize;
		Supplier<InputStream> inputStreamSupplier = resourceStream(resource, streamForBasicEnrichment, enrichSpecification);

		if (streamForBasicEnrichment) {

			MessageDigest md = null;

			if (enrichMd5) {
				md = createMessageDigest(digestAlgorithm);
			}

			// @formatter:off
			try (
				InputStream	rootIn  = inputStreamSupplier.get();
				CountingInputStream in = enrichingInputStream(rootIn, md);
			) {
			// @formatter:on

				if (enrichMimeType) {
					String mimeType = detector.getMimeType(in, resource.getName());
					resource.setMimeType(mimeType);
				}

				consume(in, new byte[consumerBufferSize]);

				if (enrichSize) {
					resource.setFileSize(in.getCount());
				}

				if (md != null) {
					resource.setMd5(digest(md));
				}

			} catch (IOException e) {
				throw unchecked("Failed to open the input stream", e);
			}
		}

		if (enrichSpecification && resource.getMimeType() != null) {
			try (InputStream in = inputStreamSupplier.get()) {
				ResourceSpecification specification = specificationDetector.getSpecification(in, resource.getMimeType(), null);
				if (specification != null) {
					resource.setSpecification(specification);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return true;

	}

	@SuppressWarnings("resource") 
	private Supplier<InputStream> resourceStream(Resource resource, boolean streamForBasicEnrichment, boolean enrichSpecification) {

		// If we will only stream the resource once or there isn't a streamPipeFactory we can just stream the resource
		// normally
		if (!enrichSpecification || !streamForBasicEnrichment || streamPipeFactory == null) {
			return resource::openStream;
		}

		// Otherwise we buffer the resource binary content in a StreamPipe for an eventual performance gain when it is
		// streamed the second time.
		//
		// The performance gain is expected to be noticeable when the specificationDetector inspects a large part of the
		// resource's binaries.
		// If the specification detector doesn't inspect most of the binary content the overhead from the buffering
		// might even be bigger than the
		// performance gain from reading a few (or no) bytes buffered. If that turns out to be a common case, we might
		// want to turn off this optimization.
		StreamPipe pipe = streamPipeFactory.newPipe(this.getClass().getSimpleName());
		Supplier<InputStream> uncheckedPipeStreamSupplier = () -> {
			try {
				return pipe.openInputStream();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		};

		AtomicBoolean wasOpened = new AtomicBoolean();

		return () -> wasOpened.getAndSet(true) ? uncheckedPipeStreamSupplier.get()
				// this riteOnReadInputStream will close the underlying streams, hence we suppress the warnings
				: new WriteOnReadInputStream(resource.openStream(), pipe.openOutputStream(), true, true);
	}

	private CountingInputStream enrichingInputStream(InputStream inputStream, MessageDigest md) {
		if (md != null)
			inputStream = new DigestInputStream(inputStream, md);

		return new CountingInputStream(inputStream, false);

	}

	private void consume(CountingInputStream in, byte[] buffer) {
		try {
			while (in.read(buffer) != -1) {
				// consuming
			}
		} catch (IOException e) {
			throw unchecked("Failed to consume the input stream", e);
		}
	}

}

// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resourceapi.stream.range.StreamRange;
import com.braintribe.utils.stream.RangeInputStream;

import hiconic.rx.module.api.resource.AbstractResourceStorage;
import hiconic.rx.module.api.resource.RxPackagedResourceResolver;
import hiconic.rx.resource.model.api.DeleteResourcePayload;
import hiconic.rx.resource.model.api.DeleteResourcePayloadResponse;
import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.resource.model.api.GetResourcePayloadResponse;
import hiconic.rx.resource.model.api.StoreResourcePayload;
import hiconic.rx.resource.model.api.StoreResourcePayloadResponse;
import hiconic.rx.resource.model.packaged.PackagedResourceNamespace;
import hiconic.rx.resource.model.packaged.PackagedResourceSource;

/** Read-only storage adapter for modeled references into indexed application-package resources. */
public class PackagedResourceStorage extends AbstractResourceStorage<PackagedResourceSource> {

	private final RxPackagedResourceResolver resources;
	private final RxPackagedResourceResolver publicResources;

	public PackagedResourceStorage(RxPackagedResourceResolver resources, RxPackagedResourceResolver publicResources) {
		this.resources = resources;
		this.publicResources = publicResources;
		setStorageId("packaged");
	}

	@Override
	protected Maybe<PackagedResourceSource> resolvePayload(GetResourcePayload request) {
		if (!(request.getResourceSource() instanceof PackagedResourceSource source))
			return error(InvalidArgument.T, "Resource source is not a PackagedResourceSource");

		try {
			resolver(source).resource(source);
		} catch (IllegalArgumentException e) {
			return error(NotFound.T, "Packaged resource not found: " + describe(source));
		}

		return Maybe.complete(source);
	}

	@Override
	protected Maybe<GetResourcePayloadResponse> getPayload(PackagedResourceSource source, GetResourcePayload request,
			GetResourcePayloadResponse response) throws UncheckedIOException {
		Supplier<InputStream> streamSupplier = resolver(source).resource(source).asHandle()::asStream;
		StreamRange range = request.getRange();
		if (range != null) {
			long start = range.getStart();
			long end = range.getEnd();
			streamSupplier = () -> rangedStream(source, start, end);
			response.setRanged(true);
			response.setRangeStart(start);
			response.setRangeEnd(end);
			response.setSize(end == Long.MAX_VALUE ? null : end - start + 1);
		}

		response.setResource(Resource.createTransient(streamSupplier::get));
		return Maybe.complete(response);
	}

	private InputStream rangedStream(PackagedResourceSource source, long start, long end) {
		try {
			InputStream in = resolver(source).resource(source).asHandle().asStream();
			return new RangeInputStream(in, start, end == Long.MAX_VALUE ? end : end + 1);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not apply stream range " + start + "-" + end + " to packaged resource " + source.getPath(), e);
		}
	}

	private RxPackagedResourceResolver resolver(PackagedResourceSource source) {
		return source.getNamespace() == PackagedResourceNamespace.publicResources ? publicResources : resources;
	}

	private static String describe(PackagedResourceSource source) {
		String artifact = source.getArtifact();
		return artifact == null || artifact.isBlank()
				? source.getNamespace() + ":" + source.getPath()
				: artifact + ":" + source.getPath();
	}

	@Override
	protected Maybe<StoreResourcePayloadResponse> storePayload(StoreResourcePayload request) throws UncheckedIOException {
		return error(InvalidArgument.T, "Packaged resources are immutable and cannot be stored at runtime");
	}

	@Override
	protected Maybe<DeleteResourcePayloadResponse> deletePayload(DeleteResourcePayload request) throws UncheckedIOException {
		return error(InvalidArgument.T, "Packaged resources are immutable and cannot be deleted at runtime");
	}
}

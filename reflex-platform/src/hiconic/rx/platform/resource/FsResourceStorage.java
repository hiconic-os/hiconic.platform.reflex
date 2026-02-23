package hiconic.rx.platform.resource;

import static com.braintribe.exception.Exceptions.unchecked;
import static com.braintribe.utils.lcd.CollectionTools2.isEmpty;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.FileSystemSource;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.resourceapi.stream.range.StreamRange;
import com.braintribe.utils.DateTools;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.stream.RangeInputStream;

import hiconic.rx.module.api.resource.AbstractResourceStorage;
import hiconic.rx.resource.model.api.DeleteResourcePayload;
import hiconic.rx.resource.model.api.DeleteResourcePayloadResponse;
import hiconic.rx.resource.model.api.DownloadResourcePayload;
import hiconic.rx.resource.model.api.ExistingResourcePayloadRequest;
import hiconic.rx.resource.model.api.GetResourcePayload;
import hiconic.rx.resource.model.api.GetResourcePayloadResponse;
import hiconic.rx.resource.model.api.StoreResourcePayload;
import hiconic.rx.resource.model.api.StoreResourcePayloadResponse;

/**
 * @author peter.gazdik
 */
public class FsResourceStorage extends AbstractResourceStorage<Path> {

	private File baseDir;
	private Path basePath;

	private static DateTimeFormatter timestampPathFormat = DateTimeFormatter.ofPattern("yyMM/ddHH/mmss").withLocale(Locale.US);

	@Required
	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
		this.basePath = baseDir.toPath().normalize();
	}

	@Override
	protected Maybe<Path> resolvePayload(GetResourcePayload  request) {
		return resolvePayloadInternal(request);
	}

	private Maybe<Path> resolvePayloadInternal(ExistingResourcePayloadRequest  request) {
		ResourceSource source = request.getResourceSource();
		if (!(source instanceof FileSystemSource))
			return error(InvalidArgument.T, "Resource source is not a FileSystemSource, but: " + source.entityType().getTypeSignature());

		String path = ((FileSystemSource) source).getPath();
		if (path == null)
			return error(InvalidArgument.T, "Path is null for resource source: " + source);

		Path fullPath = basePath.resolve(path).normalize();
		if (!fullPath.startsWith(basePath))
			return error(InvalidArgument.T, "Path points outside of the root directory for resource source: " + source);

		return Maybe.complete(fullPath);
	}

	// ###############################################
	// ## . . . . . . . Get Payload . . . . . . . . ##
	// ###############################################

	@Override
	protected Maybe<GetResourcePayloadResponse> getPayload(Path payloadPath, GetResourcePayload request, GetResourcePayloadResponse response) {
		File payloadFile = payloadPath.toFile();
		if (!payloadFile.exists())
			return error(NotFound.T, "File does not exist: " + basePath.relativize(payloadPath));
		if (payloadFile.isDirectory())
			return error(InvalidArgument.T, "File is a directory: " + basePath.relativize(payloadPath));

		Supplier<InputStream> inputStreamSupplier = rangifyStream(payloadPath, request, response);
		Resource responseResource = Resource.createTransient(() -> inputStreamSupplier.get());

		Long streamSize = response.getRanged() //
				? (Long) (response.getRangeEnd() - response.getRangeStart() + 1) //
				: response.getSize();

		responseResource.setName(payloadPath.getFileName().toString());
		responseResource.setFileSize(streamSize);
		response.setResource(responseResource);

		return Maybe.complete(response);
	}

	private Supplier<InputStream> rangifyStream(Path path, DownloadResourcePayload request, GetResourcePayloadResponse response) {
		StreamRange streamRange = request.getRange();
		if (streamRange == null)
			return () -> openInputStream(path);

		Long requestStart = streamRange.getStart();
		if (requestStart == null || requestStart < 0)
			requestStart = 0L;

		Long requestedEnd = streamRange.getEnd();
		if (requestedEnd == null || requestedEnd < 0)
			requestedEnd = Long.MAX_VALUE;

		long fileSize = fileSize(path);

		Long start = requestStart;
		Long end = Math.min(requestedEnd, fileSize - 1);

		response.setRanged(true);
		response.setRangeStart(start);
		response.setRangeEnd(end);
		response.setSize(fileSize);

		return () -> {
			try {
				return new RangeInputStream(openInputStream(path), start, end + 1);
			} catch (Exception e) {
				throw unchecked(e, "Could not wrap input stream with range: " + start + "-" + end);
			}
		};
	}

	private InputStream openInputStream(Path path) {
		try {
			return new BufferedInputStream(Files.newInputStream(path));
		} catch (IOException e) {
			log.error("Could not open input stream for file: " + path.toAbsolutePath(), e);
			throw new UncheckedIOException("Could not open input stream for file: " + path, e);
		}
	}

	// ###############################################
	// ## . . . . . . . Store Payload . . . . . . . ##
	// ###############################################

	@Override
	protected Maybe<StoreResourcePayloadResponse> storePayload(StoreResourcePayload request) {
		Resource resource = request.getData();

		Path path = createNewEmptyFileForUpload();

		FileSystemSource resourceSource = storePayload(resource, path);

		// notifyListenersOnStore(context, request, managedResource);

		StoreResourcePayloadResponse response = StoreResourcePayloadResponse.T.create();
		response.setResourceSource(resourceSource);

		return Maybe.complete(response);
	}

	private Path createNewEmptyFileForUpload() {
		String dirPathName = currentRelativeDirPath();
		Path dirPath = basePath.resolve(dirPathName);

		while (true) {
			String fileName = UUID.randomUUID().toString();
			Path path = dirPath.resolve(fileName);
			if (createNewFile(path))
				return path;
		}
	}

	private boolean createNewFile(Path path) {
		File parentFile = path.toFile().getParentFile();

		if (parentFile != null)
			parentFile.mkdirs();

		try {
			return path.toFile().createNewFile();
		} catch (IOException e) {
			log.error("Could not create new file: " + path.toAbsolutePath(), e);
			throw new UncheckedIOException("Could not create new file: " + path, e);
		}
	}

	private FileSystemSource storePayload(Resource inResource, Path path) {
		FileSystemSource resourceSource = FileSystemSource.T.create();
		resourceSource.setPath(basePath.relativize(path).toString());

		// @formatter:off
		try (
			OutputStream out = newOutputStream(path);
			InputStream in = inResource.openStream()
		) {
			IOTools.pump(in, out);

		} catch (IOException e) {
			log.error("Could not store payload to file: " + path.toAbsolutePath(), e);			
			throw new UncheckedIOException("Could not store payload to file: " + path, e);
		}
		// @formatter:on

		return resourceSource;
	}

	private OutputStream newOutputStream(Path path) {
		try {
			Files.createDirectories(path.getParent());

			// @formatter:off
			return Files.newOutputStream(path, 
					StandardOpenOption.CREATE, 
					StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.WRITE);
			// @formatter:on

		} catch (IOException e) {
			log.error("Could not create file: " + path.toAbsolutePath(), e);
			throw new UncheckedIOException("Could not create file: " + path, e);
		}
	}

	private long fileSize(Path path) {
		try {
			return Files.size(path);
		} catch (IOException e) {
			throw new UncheckedIOException("Can't resolve size of file: " + path, e);
		}
	}

	// ###############################################
	// ## . . . . . . . Delete Payload . . . . . . .##
	// ###############################################

	@Override
	protected Maybe<DeleteResourcePayloadResponse> deletePayload(DeleteResourcePayload request) {
		Maybe<Path> payloadMaybe = resolvePayloadInternal(request);
		if (payloadMaybe.isUnsatisfied())
			return payloadMaybe.propagateReason();

		Path path = payloadMaybe.get();
		try {
			log.trace(() -> "Deleting file: " + path);
			boolean deleted = Files.deleteIfExists(path);

			cleanupEmptyDirs(path);

			// notifyListenersOnDelete(context, request);
			DeleteResourcePayloadResponse response = DeleteResourcePayloadResponse.T.create();
			response.setDeleted(deleted);
			return Maybe.complete(response);

		} catch (IOException e) {
			throw unchecked(e, "Failed to delete " + path);
		}
	}

	private void cleanupEmptyDirs(Path path) {
		List<Path> unusedFolders = collectUnusedAndEmptyParentFolders(path);

		for (Path unusedFolder : unusedFolders) {
			try {
				log.trace(() -> "Deleting unused folder: " + path);
				Files.deleteIfExists(unusedFolder);

			} catch (Exception e) {
				log.debug(() -> "Could not delete presumably empty folder: " + unusedFolder, e);
			}
		}
	}

	private List<Path> collectUnusedAndEmptyParentFolders(Path filePath) {
		String nowRelativePath = currentRelativeDirPath();

		List<Path> result = new ArrayList<>();

		File parentDir = filePath.getParent().toFile();

		List<File> emptySuperFolders = FileTools.collectEmptySuperFolders(parentDir, baseDir);
		log.trace(() -> "Got empty super folders: " + emptySuperFolders);

		if (isEmpty(emptySuperFolders))
			return Collections.EMPTY_LIST;

		log.trace(() -> "Current relative path is: " + nowRelativePath);

		String[] nowFolderParts = nowRelativePath.split("/");
		Path nowPath = basePath.resolve(nowRelativePath);

		for (int i = 0; i < nowFolderParts.length && i < emptySuperFolders.size(); ++i) {
			Path emptyPath = emptySuperFolders.get(i).toPath();

			if (log.isTraceEnabled())
				log.trace("Current empty path is: " + emptyPath + ", check path is: " + nowPath);

			if (emptyPath.equals(nowPath)) {
				if (log.isTraceEnabled())
					log.trace("Current empty path " + emptyPath + " equals the check path " + nowPath + ". Stopping here.");
				break;
			} else {
				log.trace(() -> "Adding empty path " + emptyPath);
				result.add(emptyPath);
			}
			nowPath = nowPath.getParent();
		}
		return result;
	}

	private String currentRelativeDirPath() {
		// UnsupportedTemporalTypeException: Unsupported field: HourOfDay
		// return LocalDate.now().format(timestampPathFormat);

		Date now = new Date();
		return DateTools.encode(now, timestampPathFormat);
	}

}

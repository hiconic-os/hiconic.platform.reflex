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
package hiconic.rx.logs.processing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.text.StringEscapeUtils;

import com.braintribe.common.lcd.Numbers;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.resource.Resource;
import com.braintribe.utils.DateTools;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.collection.api.MultiMap;

import hiconic.rx.logs.model.api.GetLogContent;
import hiconic.rx.logs.model.api.GetLogFiles;
import hiconic.rx.logs.model.api.GetLogs;
import hiconic.rx.logs.model.api.LogContent;
import hiconic.rx.logs.model.api.LogFileBundle;
import hiconic.rx.logs.model.api.LogFiles;
import hiconic.rx.logs.model.api.Logs;
import hiconic.rx.logs.model.api.LogsRequest;
import hiconic.rx.logs.model.api.LogsResponse;
import hiconic.rx.logs.model.api.SetLogLevel;
import hiconic.rx.logs.model.api.SetLogLevelResponse;

// TODO mostly just copied from old Cortex, probably doesn't work at all...
public class LogsRxProcessor extends AbstractDispatchingServiceProcessor<LogsRequest, LogsResponse> {

	private static Logger logger = Logger.getLogger(LogsRxProcessor.class);

	// protected File logFolder;
	private Supplier<String> userNameProvider;

	private static final int newLineLength = "\r\n".length();

	@SuppressWarnings("unused")
	private final ReentrantLock logFileCacheLock = new ReentrantLock();

	private final Map<String, File> knownLogFiles = new HashMap<>();
	private final MultiMap<String, File> knownLogFilesPerKey = null;
	private long logFileCacheLastRefresh = -1L;

	@Override
	protected void configureDispatching(DispatchConfiguration<LogsRequest, LogsResponse> dispatching) {
		dispatching.register(SetLogLevel.T, (c, r) -> setLogLevel(r));
		dispatching.register(GetLogs.T, (c, r) -> getLog(r));
		dispatching.register(GetLogFiles.T, (c, r) -> getLogFiles(r));
		dispatching.register(GetLogContent.T, (c, r) -> getLogContent(r));
	}

	@SuppressWarnings("unused")
	private SetLogLevelResponse setLogLevel(SetLogLevel request) {
		// TODO implement SetLogLevel somehow

		SetLogLevelResponse response = SetLogLevelResponse.T.create();
		return response;
	}

	private Logs getLog(GetLogs request) {

		loadLogFiles();

		// TODO implement getLog
		if (knownLogFilesPerKey == null)
			return Logs.T.create();

		Map<String, File> knownLogFilesRef = knownLogFiles;

		Logs logs = Logs.T.create();

		String fileName = request.getFilename();
		if (fileName == null || fileName.trim().length() == 0) {
			fileName = "*";
		}

		StringTokenizer tokenizer = new StringTokenizer(fileName, ".*", true);
		StringBuilder builder = new StringBuilder();
		boolean foundJoker = false;

		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (token.equals("*")) {
				foundJoker = true;
				builder.append(".*");
			} else if (!token.equals(".")) {
				String quoted = Pattern.quote(token);
				builder.append(quoted);
			}
		}

		if (!foundJoker) {
			if (fileName.contains("/") || fileName.contains("\\")) {
				throw new RuntimeException("no paths allowed");
			} else {
				File file = knownLogFilesRef.get(fileName);
				if (file != null && file.exists()) {
					this.setFile(logs, file, "text/plain");
				} else {
					throw new IllegalArgumentException("Could not find file " + fileName);
				}
			}
		} else {
			Date from = request.getFromDate();
			Date to = request.getToDate();

			Collection<File> logFiles = filterFiles(from, to, builder.toString());
			int logFilesCount = logFiles.size();
			int top = request.getTop();
			logFilesCount = Math.min(logFilesCount, top);
			if (logFilesCount <= 0) {
				logFilesCount = Integer.MAX_VALUE;
			}

			String dateStr = DateTools.encode(new Date(), DateTools.TERSE_DATETIME_FORMAT_2);

			String downloadFilenamePrefix = fileName;
			if (downloadFilenamePrefix.endsWith(".*")) {
				downloadFilenamePrefix = downloadFilenamePrefix.substring(0, downloadFilenamePrefix.length() - 2);
			}
			if (downloadFilenamePrefix.equals("*")) {
				downloadFilenamePrefix = "all";
			}
			downloadFilenamePrefix = FileTools.replaceIllegalCharactersInFileName(downloadFilenamePrefix, "");

			String name = String.format("%s-logs-%s.zip", downloadFilenamePrefix, dateStr);

			this.setZippedFile(logs, name, logFiles, logFilesCount);
		}

		return logs;
	}

	private Collection<File> filterFiles(Date from, Date to, String filenamePattern) {
		Pattern pattern = !StringTools.isBlank(filenamePattern) ? Pattern.compile(filenamePattern) : null;
		List<File> result = new ArrayList<>();
		MultiMap<String, File> knownLogFilesPerKeyRef = knownLogFilesPerKey;

		for (String key : knownLogFilesPerKeyRef.keySet()) {
			boolean acceptKey = true;
			if (pattern != null) {
				Matcher matcher = pattern.matcher(key);
				acceptKey = matcher.matches();
			}
			if (acceptKey) {
				Collection<File> filesPerKey = knownLogFilesPerKeyRef.getAll(key);
				for (File file : filesPerKey) {

					if (!file.exists()) {
						continue;
					}

					boolean acceptDate = true;
					if (from != null || to != null) {
						Date fileDate = new Date(file.lastModified());
						if (from != null && from.compareTo(fileDate) > 0) {
							acceptDate = false;
						}
						if (to != null && to.compareTo(fileDate) < 0) {
							acceptDate = false;
						}
					}

					if (acceptDate) {
						result.add(file);
					}
				}

			}
		}
		return result;
	}

	private void setFile(Logs logs, File file, String mimeType) {
		Resource callResource = Resource.createTransient(() -> new FileInputStream(file));

		callResource.setName(file.getName());
		callResource.setMimeType(mimeType);
		callResource.setFileSize(file.length());

		{
			BasicFileAttributes attrs = readAttributes(file, BasicFileAttributes.class);
			if (attrs != null) {
				FileTime creationTime = attrs.creationTime();
				if (creationTime != null) {
					GregorianCalendar atime = new GregorianCalendar();
					atime.setTimeInMillis(creationTime.toMillis());
					callResource.setCreated(atime.getTime());
				}
			}
		}
		{
			PosixFileAttributes attrs = readAttributes(file, PosixFileAttributes.class);
			if (attrs != null) {
				UserPrincipal owner = attrs.owner();
				if (owner != null) {
					callResource.setCreator(owner.getName());
				}
			}
		}

		logs.setLog(callResource);
	}

	private void setZippedFile(Logs logs, String name, Collection<File> logFiles, int logFilesCount) {

		Resource callResource = Resource.createTransient(new ZippingInputStreamProvider(name, logFiles, logFilesCount));

		callResource.setName(name);
		callResource.setMimeType("application/zip");
		callResource.setCreated(new Date());
		try {
			callResource.setCreator(this.userNameProvider.get());
		} catch (RuntimeException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not get the current user name.", e);
			}
		}

		logs.setLog(callResource);
	}

	private LogFiles getLogFiles(GetLogFiles request) {
		loadLogFiles();
		MultiMap<String, File> knownLogFilesPerKeyRef = knownLogFilesPerKey;

		Date from = request.getFrom();
		Date to = request.getTo();

		LogFiles logFiles = LogFiles.T.create();

		for (String key : knownLogFilesPerKeyRef.keySet()) {
			LogFileBundle bundle = LogFileBundle.T.create();
			bundle.setBundleName(key);

			for (File f : knownLogFilesPerKeyRef.getAll(key)) {

				boolean acceptDate = true;
				if (from != null || to != null) {
					Date fileDate = new Date(f.lastModified());
					if (from != null && from.compareTo(fileDate) > 0) {
						acceptDate = false;
					}
					if (to != null && to.compareTo(fileDate) < 0) {
						acceptDate = false;
					}
				}
				if (acceptDate) {
					bundle.getFileNames().add(f.getName());
				}
			}
			logFiles.getLogBundles().add(bundle);
		}
		return logFiles;
	}

	private LogContent getLogContent(GetLogContent request) {

		loadLogFiles();
		Map<String, File> knownLogFilesRef = knownLogFiles;

		String logFileName = request.getLogFile();
		long logMark = request.getMark();
		int logLines = request.getLines();

		LogContent logContentResult = LogContent.T.create();

		List<String> logContent = logContentResult.getContent();

		File logFile = knownLogFilesRef.get(logFileName);
		if (logFile == null || logFile.exists())
			return logContentResult;

		BasicFileAttributes attr = readAttributes(logFile, BasicFileAttributes.class);

		logContentResult.setCreationDate(new Date(attr.creationTime().toMillis()));

		// Read result variables
		try {

			if (logMark < 0) {
				// Mark end of file
				logMark = logFile.length();

				// Read log file from bottom to top
				try (ReversedLinesFileReader logFileReader = //
						ReversedLinesFileReader.builder().setBufferSize(1024).setCharset(StandardCharsets.UTF_8).setFile(logFile).get()) {

					String logLine = null;
					int readLogLines = 0;

					// Read logLine until start is reached or logLines are reached
					while ((logLine = logFileReader.readLine()) != null && readLogLines < logLines) {
						// Add logLine to output
						logContent.add(0, escapeHtml(logLine));
						readLogLines++;
					}
				}
			} else {
				// Read log file from top starting at mark to bottom
				try (BufferedReader logFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
					logFileReader.skip(logMark);
					String logLine = null;
					int readLogLines = 0;

					// Read logLine until end is reached or logLines are reached
					while ((logLine = logFileReader.readLine()) != null && readLogLines < logLines) {
						// Add logLine to output
						logContent.add(escapeHtml(logLine));
						readLogLines++;

						// Add logLine to logMark
						logMark += logLine.length() + newLineLength;
					}
				}
			}

			logContentResult.setMark(logMark);

		} catch (Exception e) {
			logger.debug(() -> "Error while trying to read content of log file: " + logFileName, e);
		}

		return logContentResult;
	}

	private <A extends BasicFileAttributes> A readAttributes(File file, Class<A> type) {
		try {
			return Files.readAttributes(file.toPath(), type);
		} catch (IOException e) {
			throw new UncheckedIOException("Error while reading log file: " + file, e);
		}
	}

	private String escapeHtml(String text) {
		return StringEscapeUtils.builder(StringEscapeUtils.ESCAPE_HTML4).escape(text).toString();
	}

	private void loadLogFiles() {
		long now = System.currentTimeMillis();
		if ((now - logFileCacheLastRefresh) < Numbers.MILLISECONDS_PER_SECOND * 10) {
			return;
		}
		logFileCacheLastRefresh = now;
		// TODO implement loadLogFiles

		// logFileCacheLock.lock();
		// try {
		// String catalinaBase = TribefireRuntime.getContainerRoot().replaceAll("\\\\", "/");
		// String logConfDir = catalinaBase + "/conf";
		//
		// MultiMap<String, File> logFilesPerKey = new ComparatorBasedNavigableMultiMap<>(String::compareTo, new FileComparator());
		// Map<String, File> logFiles = newMap();
		//
		// Set<String> logDirs = newSet();
		// try {
		// File logConfFolder = new File(logConfDir);
		// for (File logConf : logConfFolder.listFiles(f -> f.getName().contains("logging.properties"))) {
		// Properties logProps = new Properties();
		// try (InputStream in = new BufferedInputStream(new FileInputStream(logConf))) {
		// logProps.load(in);
		// }
		// String logLocationPath = logProps.getProperty(LOG_LOCATION);
		// logLocationPath = logLocationPath.replaceAll("\\$\\{catalina.base\\}", catalinaBase);
		// logDirs.add(logLocationPath);
		// }
		//
		// Pattern keyPattern = Pattern.compile("^[A-Za-z]+([-_][A-Za-z]+)*");
		// logDirs.stream().forEach(dir -> {
		// Arrays.asList(new File(dir).listFiles(f -> f.isFile())).stream().forEach(file -> {
		// String key = file.getName();
		// if (key.equals(".DS_Store")) {
		// // We're on a Mac
		// return;
		// }
		// Matcher keyMatcher = keyPattern.matcher(key);
		// if (keyMatcher.find()) {
		// key = keyMatcher.group();
		// }
		// logFilesPerKey.put(key, file);
		// logFiles.put(file.getName(), file);
		// });
		// });
		//
		// } catch (Exception e) {
		// logger.error("Error while reading logging configuration files. " + e.getMessage(), e);
		// }
		//
		// knownLogFilesPerKey = logFilesPerKey;
		// knownLogFiles = logFiles;
		//
		// } finally {
		// logFileCacheLock.unlock();
		// }
	}
}

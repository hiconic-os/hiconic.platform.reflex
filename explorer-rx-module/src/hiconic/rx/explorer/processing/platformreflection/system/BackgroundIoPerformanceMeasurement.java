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
package hiconic.rx.explorer.processing.platformreflection.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.LifecycleAware;
import com.braintribe.cfg.Required;
import com.braintribe.common.lcd.Numbers;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.bootstrapping.TribefireRuntime;
import com.braintribe.transport.http.HttpClientProvider;
import com.braintribe.transport.http.ResponseEntityInputStream;
import com.braintribe.transport.http.util.HttpTools;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;

/**
 * This class is a Runnable that checks the performance of the underlying I/O system.
 * This is done by writing data to a file and reading this data again. Furthermore, an image
 * is downloaded from the Internet to measure (very crudely, though) the Internet connection speed.
 */
public class BackgroundIoPerformanceMeasurement implements Runnable, LifecycleAware {

	private static Logger logger = Logger.getLogger(BackgroundIoPerformanceMeasurement.class);

	public static final int SIZE_GB = 8;
	private static final int BLOCK_SIZE = 64 * 1024;
	private static final int BLOCK_COUNT = (int) (((long) SIZE_GB << 30) / BLOCK_SIZE);

	public static final String ENVIRONMENT_ABOUT_PERIODIC_PERFORMANCE_MEASUREMENT = "TRIBEFIRE_ABOUT_PERIODIC_PERFORMANCE_MEASUREMENT";
	public static final String ENVIRONMENT_ABOUT_PERIODIC_PERFORMANCE_MEASUREMENT_INTERVALMS = "TRIBEFIRE_ABOUT_PERIODIC_PERFORMANCE_MEASUREMENT_INTERVALMS";

	public static Double diskWriteSpeedGBPerSecond = null;
	public static Double diskReadSpeedGBPerSecond = null;
	public static String diskForTesting = null;
	public static Double httpDownloadSpeedKBPerSecond = null;
	public static Long   httpDownloadSize = null;
	
	private HttpClientProvider httpClientProvider;
	private CloseableHttpClient httpClient = null;
	private String downloadUrl;

	protected boolean performMeasurement = false;
	protected long intervalInMs = Numbers.MILLISECONDS_PER_HOUR * 6L; //Every 6 hours

	@Override
	public void postConstruct() {
		String performMeasurementString = TribefireRuntime.getProperty(ENVIRONMENT_ABOUT_PERIODIC_PERFORMANCE_MEASUREMENT);
		if (performMeasurementString != null && performMeasurementString.equalsIgnoreCase("true")) {
			performMeasurement = true;
		} else {
			performMeasurement = false;
		}
	}

	@Override
	public void preDestroy() {
		performMeasurement = false;
	}

	@Override
	public void run() {
		if (performMeasurement) {
			this.doDiskMeasurement();
			this.doInternetMeasurement();
		}
	}

	private void doInternetMeasurement() {
		
		if (StringTools.isBlank(downloadUrl)) {
			logger.info(() -> "The download URL is not set.");
			return;
		}
		
		CloseableHttpClient client = null;
		try {
			client = this.getClient();
		} catch(Exception e) {
			logger.debug(() -> "Could not get an HTTP Client.", e);
			return;
		}
		
		CloseableHttpResponse response = null;
		File targetFile = null;
		try {
			targetFile = File.createTempFile("download-measurement", ".bin");
			
			HttpGet get = new HttpGet(downloadUrl);
			
			long start = System.nanoTime();
			long end = -1;
			
			response = client.execute(get);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				final String responseString = response.toString();
				logger.debug(() -> "Got a non-200 response from "+downloadUrl+": "+responseString);
			} else {
				try (InputStream is = new ResponseEntityInputStream(response); FileOutputStream fos = new FileOutputStream(targetFile)) {
					IOTools.pump(is, fos);
					
					end = System.nanoTime();
				} catch(Exception e) {
					logger.debug(() -> "Error while downloading banner from "+downloadUrl, e);
				}
			}
			
			if (end != -1) {
				httpDownloadSize = targetFile.length();
				httpDownloadSpeedKBPerSecond = ((double) (httpDownloadSize*Numbers.NANOSECONDS_PER_MILLISECOND)/(end-start));
				
				logger.debug(() -> String.format("Download speed %.1f kB/s", httpDownloadSpeedKBPerSecond));
			}
			
		} catch(Exception e) {
			final String message = "Could not get a valid result from "+downloadUrl;
			if (logger.isTraceEnabled()) {
				logger.trace(message, e);
			}
			logger.debug(() -> message+": "+e.getMessage());
		} finally {
			HttpTools.consumeResponse(downloadUrl, response);
			HttpTools.closeResponseQuietly(downloadUrl, response);
			FileTools.deleteFileSilently(targetFile);
		}
	}

	private void doDiskMeasurement() {

		final File tempFile;
		try {
			tempFile = File.createTempFile("measurement", ".bin");
		} catch (Exception e) {
			logger.debug(() -> "Could not create a temporary file.", e);
			return;
		}

		try {

			Path tempPath = tempFile.toPath();
			Path rootPath = tempPath.getRoot();
			diskForTesting = rootPath.toString();
			
			long freeSpace = tempFile.getFreeSpace();
			if ((Numbers.BILLION * SIZE_GB * 10) > freeSpace) {
				logger.debug(() -> "Skipping file system performance measurement as there is only "+StringTools.prettyPrintBytesBinary(freeSpace)+" free disk space.");
				return;
			} else {
				logger.debug(() -> "Performing the system performance measurement as there is "+StringTools.prettyPrintBytesBinary(freeSpace)+" free disk space.");
			}
			
			final byte[] buffer = new byte[BLOCK_SIZE];
			final byte[] acceptBuffer = new byte[555];
			
			long start = System.nanoTime();
			
			try (FileOutputStream out = new FileOutputStream(tempFile)) {
				for (int i = 0; i < BLOCK_COUNT; i++) {
					out.write(buffer);
				}
			} catch(Exception e) {
				logger.debug(() -> "Error while writing "+SIZE_GB+" GB to the temporary file: "+tempFile.getAbsolutePath(), e);
				return;
			}

			long mid = System.nanoTime();
			
			try (FileInputStream in = new FileInputStream(tempFile)) {
				for (int i=0; i<BLOCK_COUNT; ++i) {
					for (int remaining = acceptBuffer.length, read; (read = in.read(buffer)) != -1 && (remaining -= read) > 0; ) {
						in.read(acceptBuffer, acceptBuffer.length - remaining, remaining);
					}
				}
			} catch(Exception e) {
				logger.debug(() -> "Error while reading "+SIZE_GB+" GB from the temporary file: "+tempFile.getAbsolutePath(), e);
				return;
			}

	        long end = System.nanoTime();

	        long size = tempFile.length();
	        
	        diskWriteSpeedGBPerSecond = ((double) size/(mid-start));
	        diskReadSpeedGBPerSecond = ((double) size/(end-mid));
			
	        logger.debug(() -> String.format("Write speed %.1f GB/s, Read speed %.1f GB/s", diskWriteSpeedGBPerSecond, diskReadSpeedGBPerSecond));
	        
		} finally {
			FileTools.deleteFileSilently(tempFile);
		}
	}

	protected CloseableHttpClient getClient() throws Exception {
		if (this.httpClient == null) {
			this.httpClient = this.httpClientProvider.provideHttpClient();
		}
		return this.httpClient;
	}
	
	@Configurable
	public void setIntervalInMs(long intervalInMs) {
		if (intervalInMs > 0) {
			this.intervalInMs = intervalInMs;
		}
	}
	public long getIntervalInMs() {
		if (!performMeasurement) {
			return -1;
		}
		return intervalInMs;
	}
	@Configurable
	@Required
	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}
	@Configurable
	@Required
	public void setHttpClientProvider(HttpClientProvider httpClientProvider) {
		this.httpClientProvider = httpClientProvider;
	}

}

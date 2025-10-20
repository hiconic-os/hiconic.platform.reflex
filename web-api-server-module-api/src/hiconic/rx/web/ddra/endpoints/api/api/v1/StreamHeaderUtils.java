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
package hiconic.rx.web.ddra.endpoints.api.api.v1;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.braintribe.model.cache.CacheControl;
import com.braintribe.model.resourceapi.stream.BinaryRetrievalResponse;
import com.braintribe.model.resourceapi.stream.HasStreamCondition;
import com.braintribe.model.resourceapi.stream.HasStreamRange;
import com.braintribe.model.resourceapi.stream.condition.FingerprintMismatch;
import com.braintribe.model.resourceapi.stream.condition.ModifiedSince;
import com.braintribe.model.resourceapi.stream.range.StreamRange;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.lcd.StringTools;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface StreamHeaderUtils {

	static void ensureContentDispositionHeader(String responseFilename, boolean saveLocally, HttpServletResponse response) {
		if (responseFilename == null && !saveLocally)
			return;

		String disposition = saveLocally ? "attachment" : "inline";

		if (responseFilename != null) {
			String nkfd = Normalizer.normalize(FileTools.normalizeFilename(responseFilename, '_'), Normalizer.Form.NFKD);
			StringBuilder sb = new StringBuilder(nkfd.length());
			for (char c : nkfd.toCharArray()) {
				if (c <= '\u007F')
					sb.append(c);
			}
			final String normalizedFilename = sb.toString();

			// Because the Content-Disposition header is encoded in ISO-8859-1 we need to handle characters outside this encoding
			// specially
			boolean needsToBeEncoded = !Charset.forName("ISO-8859-1").newEncoder().canEncode(responseFilename);

			// Sometimes the 'filename*' attribute is not supported by a client. That's why we always send a normalized
			// simple 'filename' as a backup.
			// Only if the normalization changes the value we supply a 'filename*' variant which always provides the
			// original filename. If 'filename*' is supported, it should always have prevalence over the simple 'filename'.
			// See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition#Directives
			if (!normalizedFilename.equals(responseFilename) || needsToBeEncoded) {
				try {
					String rfc5987EncodedFilename = rfc5987Encode(responseFilename);
					disposition += "; filename*=UTF-8''" + rfc5987EncodedFilename;
				} catch (UnsupportedEncodingException e) {
					throw new IllegalStateException("Could not UrlEncode filename for response header.");
				}
			}

			disposition += "; filename=\"" + normalizedFilename + "\"";
		}
		response.setHeader("Content-Disposition", disposition);

	}

	public static String rfc5987Encode(final String s) throws UnsupportedEncodingException {
		final byte[] utf8Bytes = s.getBytes("UTF-8");
		final int len = utf8Bytes.length;
		final StringBuilder sb = new StringBuilder(len << 1);
		//@formatter:off
	    final char[] digits = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	    final byte[] attr_char = {'!','#','$','&','+','-','.','0','1','2','3','4','5','6','7','8','9', 
	    		'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','^','_','`',
	    		'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','|', '~'};
	    //@formatter:on

		for (int i = 0; i < len; ++i) {
			final byte b = utf8Bytes[i];
			if (Arrays.binarySearch(attr_char, b) >= 0)
				sb.append((char) b);
			else {
				sb.append('%');
				sb.append(digits[0x0f & (b >>> 4)]);
				sb.append(digits[b & 0x0f]);
			}
		}

		return sb.toString();
	}

	static void setContentRangeHeaders(HttpServletResponse response, BinaryRetrievalResponse streamBinaryResponse) {
		if (streamBinaryResponse.getRanged()) {
			Long size = streamBinaryResponse.getSize();
			String sizeString = (size != null) ? String.valueOf(size) : "*";
			String rangeStart = String.valueOf(streamBinaryResponse.getRangeStart());
			String rangeEnd = String.valueOf(streamBinaryResponse.getRangeEnd());
			String contentRange = "bytes ".concat(rangeStart).concat("-").concat(rangeEnd).concat("/").concat(sizeString);
			response.setHeader("Content-Range", contentRange);
			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
		}

		response.setHeader("Accept-Ranges", "bytes");
	}

	static void setDefaultCacheControlHeaders(HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-store, no-cache");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
	}

	static void setCacheControlHeaders(HttpServletResponse response, CacheControl cacheControl) {
		if (cacheControl == null) {
			setDefaultCacheControlHeaders(response);
			return;
		}

		List<String> ccParts = new ArrayList<>();

		if (cacheControl.getType() != null) {
			switch (cacheControl.getType()) {
				case noCache:
					ccParts.add("no-cache");
					response.setHeader("Pragma", "no-cache");
					break;
				case noStore:
					ccParts.add("no-store");
					break;
				case privateCache:
					ccParts.add("private");
					break;
				case publicCache:
					ccParts.add("public");
					break;
			}
		}

		if (cacheControl.getMaxAge() != null) {
			ccParts.add("max-age=" + cacheControl.getMaxAge());
		}

		if (cacheControl.getMustRevalidate()) {
			ccParts.add("must-revalidate");
		}

		if (ccParts.isEmpty()) {
			setDefaultCacheControlHeaders(response);
		} else {
			response.setHeader("Cache-Control", String.join(", ", ccParts));
		}

		if (cacheControl.getLastModified() != null) {
			response.setDateHeader("Last-Modified", cacheControl.getLastModified().getTime());
		}

		if (cacheControl.getFingerprint() != null) {
			response.setHeader("ETag", cacheControl.getFingerprint());
		}
	}

	static void processStreamHeaders(HttpServletRequest request, ServiceRequest service) {
		if (service instanceof HasStreamCondition) {
			HasStreamCondition hasStreamCondition = (HasStreamCondition) service;

			String ifNonMatchHeader = request.getHeader("If-None-Match");
			long ifModifiedSinceHeader = request.getDateHeader("If-Modified-Since");

			if (ifNonMatchHeader != null) {
				FingerprintMismatch streamCondition = FingerprintMismatch.T.create();
				streamCondition.setFingerprint(ifNonMatchHeader);
				hasStreamCondition.setCondition(streamCondition);
			} else if (ifModifiedSinceHeader != -1) {
				ModifiedSince streamCondition = ModifiedSince.T.create();
				streamCondition.setDate(new Date(ifModifiedSinceHeader));
				hasStreamCondition.setCondition(streamCondition);
			}

		}

		if (service instanceof HasStreamRange) {
			HasStreamRange hasStreamRange = (HasStreamRange) service;

			parseRangeHeader(request, hasStreamRange);
		}
	}

	static void parseRangeHeader(HttpServletRequest request, HasStreamRange hasStreamRange) {

		String rangeHeader = request.getHeader("Range");
		if (StringTools.isBlank(rangeHeader)) {
			return;
		}

		try {

			int index = rangeHeader.indexOf('=');
			if (index == -1) {
				throw new IllegalStateException("There is no '=' sign.");
			}
			String unit = rangeHeader.substring(0, index).trim();
			if (StringTools.isBlank(unit) || !unit.equalsIgnoreCase("bytes")) {
				throw new IllegalStateException("Only unit 'bytes' is supported.");
			}
			String rangeSpec = rangeHeader.substring(index + 1).trim();
			index = rangeSpec.indexOf('-');
			if (index == -1) {
				throw new IllegalStateException("The range value " + rangeSpec + " does not contain '-'");
			}
			String start = rangeSpec.substring(0, index).trim();
			String end = null;
			if (index < rangeSpec.length() - 1) {
				end = rangeSpec.substring(index + 1).trim();
			}
			long startLong = Long.parseLong(start);
			Long endLong = null;
			if (!StringTools.isBlank(end)) {
				endLong = Long.parseLong(end);
			}

			StreamRange streamRange = StreamRange.T.create();

			streamRange.setStart(startLong);
			streamRange.setEnd(endLong);

			hasStreamRange.setRange(streamRange);
		} catch (Exception e) {
			throw new IllegalStateException("Unable to parse Range header \"" + rangeHeader + "\".", e);
		}

	}
}

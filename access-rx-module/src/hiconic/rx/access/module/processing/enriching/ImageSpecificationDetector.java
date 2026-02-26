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
package hiconic.rx.access.module.processing.enriching;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.session.GmSession;
import com.braintribe.model.resource.specification.ImageSpecification;
import com.braintribe.model.resource.specification.RasterImageSpecification;
import com.braintribe.utils.IOTools;

import hiconic.rx.access.module.api.ResourceSpecificationDetector;

public class ImageSpecificationDetector implements ResourceSpecificationDetector<ImageSpecification> {

	private final static Logger logger = Logger.getLogger(ImageSpecificationDetector.class);

	@Override
	public ImageSpecification getSpecification(InputStream in, String mimeType, GmSession session) throws IOException {

		ImageInputStream iis = null;
		ImageReader imageReader = null;

		try {
			iis = ImageIO.createImageInputStream(in);
			if (iis == null)
				return null;

			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
			if (!readers.hasNext())
				return null;

			imageReader = readers.next();
			imageReader.setInput(iis);

			return createSpecificationIsPossible(session, imageReader);

		} catch (Exception e) {
			logger.error("Error while detecting image specification.", e);
			return null;

		} finally {
			if (imageReader != null) {
				try {
					imageReader.dispose();
				} catch (Exception e) {
					logger.error("Error while disposing the image reader", e);
				}
			}
			IOTools.closeCloseable(iis, logger);

		}
	}

	private ImageSpecification createSpecificationIsPossible(GmSession session, ImageReader imageReader) {
		Integer height = readHeight(imageReader);
		Integer width = readWidth(imageReader);
		Integer numberImages = readNumImages(imageReader);

		boolean hasDimensions = height != null && width != null;
		boolean hasNumImages = numberImages != null;

		if (!hasDimensions && !hasNumImages)
			return null;

		ImageSpecification result;
		if (hasDimensions) {
			RasterImageSpecification rasterSpecification = contextualCreate(RasterImageSpecification.T, session);
			rasterSpecification.setPixelWidth(width);
			rasterSpecification.setPixelHeight(height);

			result = rasterSpecification;

		} else {
			result = contextualCreate(ImageSpecification.T, session);
		}

		if (hasNumImages)
			result.setPageCount(numberImages);

		return result;
	}

	private Integer readHeight(ImageReader imageReader) {
		try {
			return imageReader.getHeight(0);
		} catch (IOException e) {
			return null;
		}
	}

	private Integer readWidth(ImageReader imageReader) {
		try {
			return imageReader.getWidth(0);
		} catch (IOException e) {
			return null;
		}
	}

	private Integer readNumImages(ImageReader imageReader) {
		try {
			return imageReader.getNumImages(true);
		} catch (IOException e) {
			return null;
		}
	}

	private <T extends GenericEntity> T contextualCreate(EntityType<T> type, GmSession session) {
		return session != null ? session.create(type) : type.create();
	}
}

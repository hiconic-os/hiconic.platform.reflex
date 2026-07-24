package hiconic.platform.reflex.web_streaming.processing;

import java.io.IOException;

import com.braintribe.cfg.Required;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.resource.Resource;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Read compatibility for the historic {@code /streaming} URL contract.
 * The actual payload retrieval is delegated to the RX session resource API,
 * i.e. to the same canonical resource processors used by modeled requests.
 */
public class LegacyResourceStreamingServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private PersistenceGmSessionFactory sessionFactory;

	@Required
	public void setSessionFactory(PersistenceGmSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String accessId = request.getParameter("accessId");
		String resourceId = request.getParameter("resourceId");
		if (isBlank(accessId) || isBlank(resourceId)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Both accessId and resourceId are required");
			return;
		}

		PersistenceGmSession session = sessionFactory.newSession(accessId);
		Resource resource = session.query().entities(EntityQueryBuilder.from(Resource.T) //
				.where().property(Resource.id).eq(resourceId).done()).first();
		if (resource == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (resource.getMimeType() != null)
			response.setContentType(resource.getMimeType());
		if (resource.getFileSize() != null)
			response.setContentLengthLong(resource.getFileSize());

		String fileName = request.getParameter("fileName");
		boolean download = Boolean.parseBoolean(request.getParameter("download"));
		response.setHeader("Content-Disposition", contentDisposition(download, fileName != null ? fileName : resource.getName()));
		session.resources().writeToStream(resource, response.getOutputStream());
	}

	private static String contentDisposition(boolean download, String fileName) {
		String disposition = download ? "attachment" : "inline";
		if (isBlank(fileName))
			return disposition;
		return disposition + "; filename=\"" + fileName.replace("\"", "") + "\"";
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

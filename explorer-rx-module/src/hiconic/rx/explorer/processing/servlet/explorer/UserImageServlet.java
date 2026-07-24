// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.explorer.processing.servlet.explorer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import com.braintribe.cfg.Required;
import com.braintribe.common.lcd.Numbers;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.resource.Icon;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.StringTools;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Streams the current or explicitly named user's picture, with a packaged fallback image. */
public class UserImageServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final long CACHE_SECONDS = Numbers.SECONDS_PER_HOUR;

	private PersistenceGmSessionFactory sessionFactory;
	private String defaultUserImageUrl;

	@Required
	public void setSessionFactory(PersistenceGmSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Required
	public void setDefaultUserImageUrl(String defaultUserImageUrl) {
		this.defaultUserImageUrl = defaultUserImageUrl;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		User user = resolveUser(request.getParameter("name"));
		Icon picture = user == null ? null : user.getPicture();
		Resource image = picture == null ? null : picture.image();

		if (image == null) {
			response.sendRedirect(defaultUserImageUrl);
			return;
		}

		long expiry = new Date().getTime() + CACHE_SECONDS * Numbers.MILLISECONDS_PER_SECOND;
		response.setDateHeader("Expires", expiry);
		response.setHeader("Cache-Control", "max-age=" + CACHE_SECONDS);
		if (image.getMimeType() != null)
			response.setContentType(image.getMimeType());

		try (InputStream in = image.openStream()) {
			in.transferTo(response.getOutputStream());
		}
	}

	private User resolveUser(String requestedName) {
		UserSession userSession = AttributeContexts.peek().findOrNull(UserSessionAspect.class);
		String userName = !StringTools.isBlank(requestedName) ? requestedName
				: userSession == null || userSession.getUser() == null ? null : userSession.getUser().getName();

		if (!StringTools.isBlank(userName)) {
			PersistenceGmSession authSession = sessionFactory.newSession("auth");
			User persistentUser = authSession.query().entities(EntityQueryBuilder.from(User.T) //
					.where().property(User.name).eq(userName).done()).first();
			if (persistentUser != null)
				return persistentUser;
		}

		return userSession == null ? null : userSession.getUser();
	}
}

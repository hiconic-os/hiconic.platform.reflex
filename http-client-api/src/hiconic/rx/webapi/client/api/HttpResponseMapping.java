package hiconic.rx.webapi.client.api;

import com.braintribe.model.generic.reflection.GenericModelType;

/** Runtime representation of an HTTP reasoning rule and, after resolution, its root/detail plan. */
public final class HttpResponseMapping {
	public enum Kind { STATUS, BODY, BODY_DETAIL }
	private final String statusExpression;
	private final Kind kind;
	private final GenericModelType reasonType;
	private final GenericModelType detailType;
	private final String bodyMimeType;
	private final boolean useOriginalStatusCode;
	private final boolean defaultRule;
	private final int metadataPrecedence;

	public HttpResponseMapping(String statusExpression, Kind kind, GenericModelType reasonType, boolean useOriginalStatusCode, boolean defaultRule) {
		this(statusExpression, kind, reasonType, null, useOriginalStatusCode, defaultRule, 0);
	}
	public HttpResponseMapping(String statusExpression, Kind kind, GenericModelType reasonType, boolean useOriginalStatusCode, boolean defaultRule,
			int metadataPrecedence) {
		this(statusExpression, kind, reasonType, null, useOriginalStatusCode, defaultRule, metadataPrecedence);
	}
	public HttpResponseMapping(String statusExpression, Kind kind, GenericModelType reasonType, String bodyMimeType,
			boolean useOriginalStatusCode, boolean defaultRule, int metadataPrecedence) {
		this(statusExpression, kind, reasonType, null, bodyMimeType, useOriginalStatusCode, defaultRule, metadataPrecedence);
	}
	private HttpResponseMapping(String statusExpression, Kind kind, GenericModelType reasonType, GenericModelType detailType, String bodyMimeType,
			boolean useOriginalStatusCode, boolean defaultRule, int metadataPrecedence) {
		this.statusExpression = statusExpression;
		this.kind = kind;
		this.reasonType = reasonType;
		this.detailType = detailType;
		this.bodyMimeType = bodyMimeType;
		this.useOriginalStatusCode = useOriginalStatusCode;
		this.defaultRule = defaultRule;
		this.metadataPrecedence = metadataPrecedence;
	}
	public static HttpResponseMapping plan(HttpResponseMapping root, HttpResponseMapping detail) {
		return new HttpResponseMapping(root.statusExpression, root.kind, root.reasonType,
				root.kind == Kind.BODY || detail == null ? null : detail.reasonType,
				root.kind == Kind.BODY ? root.bodyMimeType : detail == null ? null : detail.bodyMimeType,
				root.useOriginalStatusCode, root.defaultRule, root.metadataPrecedence);
	}
	public boolean matches(int statusCode) {
		boolean included = false;
		boolean hasInclusion = false;
		for (String rawToken : statusExpression.split(",")) {
			String token = rawToken.trim();
			if (token.isEmpty()) continue;
			boolean excluded = token.charAt(0) == '!';
			if (excluded) token = token.substring(1).trim(); else hasInclusion = true;
			if (matchesToken(token, statusCode)) {
				if (excluded) return false;
				included = true;
			}
		}
		return included || !hasInclusion;
	}
	public int specificity(int statusCode) {
		if (!matches(statusCode)) return -1;
		int count = 0;
		for (int code = 100; code <= 599; code++) if (matches(code)) count++;
		return 501 - count;
	}
	private static boolean matchesToken(String token, int statusCode) {
		if ("*".equals(token)) return true;
		if (token.length() == 3 && token.substring(1).equalsIgnoreCase("xx") && Character.isDigit(token.charAt(0)))
			return statusCode / 100 == Character.digit(token.charAt(0), 10);
		int separator = token.indexOf('-');
		try {
			if (separator > 0) {
				int from = Integer.parseInt(token.substring(0, separator).trim());
				int to = Integer.parseInt(token.substring(separator + 1).trim());
				return statusCode >= from && statusCode <= to;
			}
			return statusCode == Integer.parseInt(token);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid HTTP status expression token: '" + token + "'", e);
		}
	}
	public Kind kind() { return kind; }
	public GenericModelType reasonType() { return reasonType; }
	public GenericModelType detailType() { return detailType; }
	public GenericModelType bodyType() { return kind == Kind.BODY || kind == Kind.BODY_DETAIL ? reasonType : detailType; }
	public String bodyMimeType() { return bodyMimeType; }
	public boolean useOriginalStatusCode() { return useOriginalStatusCode; }
	public boolean defaultRule() { return defaultRule; }
	public int metadataPrecedence() { return metadataPrecedence; }
	public int semanticRank() { return kind == Kind.BODY ? 2 : 1; }
}

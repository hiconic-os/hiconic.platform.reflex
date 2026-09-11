package hiconic.rx.browser.acceptance.processing;

/** Informational HTTP client data. None of these values participate in browser-context authorization. */
public record BrowserRequestInformation(String directAddress, String userAgent, String clientHintsUserAgent,
		String clientHintsPlatform, String clientHintsMobile) {
}

package dev.hiconic.servlet.decoder;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import dev.hiconic.servlet.decoder.api.HttpExceptions;

/** Guards the established decoder API while CX and RX artifacts can coexist on migration classpaths. */
public class HttpExceptionsCompatibilityTest {

	@Test
	public void exposesEstablishedExceptionFactories() throws Exception {
		assertFactory("methodNotAllowed");
		assertFactory("notAcceptable");
		assertFactory("preConditionFaild");
		assertFactory("expectationFailed");
		assertFactory("unauthotized");
		assertFactory("notFound");
		assertFactory("badRequest");
		assertFactory("notImplemented");
		assertFactory("internalServerError");
		assertNotNull(HttpExceptions.class.getMethod("httpException", int.class, String.class, Object[].class));
	}

	private void assertFactory(String name) throws Exception {
		assertNotNull(HttpExceptions.class.getMethod(name, String.class, Object[].class));
	}
}

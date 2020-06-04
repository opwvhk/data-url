package opwvhk.data_url;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.UnsupportedCharsetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataURLStreamHandlerTest {
	private static final String TEST_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAQAAAD9CzEMAAAAAmJLR0QAAKqNIzIAAAAJcEhZcwAAAEgAAABIAEbJaz4AAABpSURBVFjD7di7DgAQDAVQlf7/L9dmYOigJVevSUScqNRLmrXU0nOH/wHQWZPgke3SDPAB3VpO82JZS/wQESgAqNvD26OcvMEPEQECBAiUAPzz4PCehB8iAgWAPQ+C32r4IUoHhH8Vz4EBtYQGZ1X1QVgAAAAwdEVYdGNvbW1lbnQARWRpdGVkIHdpdGggTHVuYVBpYzogaHR0cDovL2x1bmFwaWMuY29tL70KMtwAAAAldEVYdGRhdGU6Y3JlYXRlADIwMTMtMDYtMDVUMDY6MjE6MjktMDc6MDAv/LCiAAAAJXRFWHRkYXRlOm1vZGlmeQAyMDEzLTA2LTA1VDA2OjIxOjI5LTA3OjAwXqEIHgAAAABJRU5ErkJggg==";

	private opwvhk.data_url.DataURLStreamHandler streamHandler;
	private URL testURL;

	@BeforeEach
	void setUp() throws MalformedURLException {
		streamHandler = new DataURLStreamHandler();
		testURL = dataURL(TEST_IMAGE.substring(5));
	}

	private URL dataURL(String mimeTypeAndContent) throws MalformedURLException {
		return new URL("data", null, -1, mimeTypeAndContent, streamHandler);
	}

	private byte[] getContent(URL url) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (InputStream inputStream = url.openStream()) {
			inputStream.transferTo(buffer);
		}
		return buffer.toByteArray();
	}

	@Test
	void validateMimeTypePattern() {
		final Pattern pattern = DataURLStreamHandler.STARTS_WITH_MIME_TYPE_PATTERN;

		assertPatternMatchesFirstChars(pattern, 0, "");
		assertPatternMatchesFirstChars(pattern, 0, ",image/png");
		assertPatternMatchesFirstChars(pattern, 12, ";charset=bla");
		assertPatternMatchesFirstChars(pattern, 12, ";charset=bla,image/png");
		assertPatternMatchesFirstChars(pattern, 17, ";charset=\"b ;,la\",image/png");
		assertPatternMatchesFirstChars(pattern, 9, "image/png");
		assertPatternMatchesFirstChars(pattern, 9, "image/png,aksfyirweu");
		assertPatternMatchesFirstChars(pattern, 9, "image/png;base64,aksfyirweu");
		assertPatternMatchesFirstChars(pattern, 26, "image/png;foo=\"b ;\\\\,\\\"ar\",aksfyirweu");
		assertPatternMatchesFirstChars(pattern, 26, "image/png;foo=\"b ;\\\\,\\\"ar\";base64,aksfyirweu");
	}

	private void assertPatternMatchesFirstChars(final Pattern pattern, final int matchLength, final String input) {
		Matcher matcher = pattern.matcher(input);
		try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
			softly.assertThat(matcher.find()).isTrue();
			softly.assertThat(matcher.start()).isEqualTo(0);
			softly.assertThat(matcher.end()).isEqualTo(matchLength);
		}
	}

	@Test
	void checkOpenConnectionIgnoresProxies() throws MalformedURLException {
		final DataURLStreamHandler spy = Mockito.spy(streamHandler);

		final URL url = new URL(TEST_IMAGE);
		final Proxy proxy = Mockito.mock(Proxy.class);
		spy.openConnection(url, proxy);

		Mockito.verify(spy).openConnection(url);
	}

	@Test
	void checkErrorsYieldExceptions() throws MalformedURLException {
		final URL notADataURL = new URL("something-else", null, -1, "whatever", streamHandler);
		assertThatThrownBy(notADataURL::openConnection).
			isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> dataURL("whatever").openConnection()).
			isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> dataURL("noMimeType,whatever").openConnection()).
			isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> dataURL("also/no;mimeType,whatever").openConnection()).
			isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void validateDataURLMimeTypeDetection() throws IOException {
		assertThat(dataURL(",foo").openConnection().getContentType()).
			isEqualTo("text/plain;charset=US-ASCII");
		assertThat(dataURL(";charset=utf8,foo").openConnection().getContentType()).
			isEqualTo("text/plain;charset=utf8");
		assertThat(dataURL("text/plain,foo").openConnection().getContentType()).
			isEqualTo("text/plain");
		assertThat(testURL.openConnection().getContentType()).
			isEqualTo("image/png");

		assertThatThrownBy(() -> dataURL(";charset=bla,foo").openConnection().getContentType()).
			isInstanceOf(UnsupportedCharsetException.class).hasMessage("bla");
	}

	@Test
	void validateContentDecoding() throws IOException {
		// SGVsbG8gV29ybGQh are the base64 encoded US-ASCII bytes of "Hello World!"
		assertThat(getContent(dataURL("text/plain;base64,SGVsbG8gV29ybGQh"))).
			isEqualTo("Hello World!".getBytes(US_ASCII));

		// No charset parameter and no base64? Use US-ASCII
		assertThat(getContent(dataURL("text/plain,Hello%20World%21"))).
			isEqualTo("Hello World!".getBytes(US_ASCII));
		assertThat(getContent(dataURL("text/plain;foo=bar,Hello%20World%21"))).
			isEqualTo("Hello World!".getBytes(US_ASCII));

		// Any parameter can be the charset
		assertThat(getContent(dataURL("text/plain;foo=bar;charset=utf8,Hello%20World%20%E2%98%BA"))).
			isEqualTo("Hello World ☺".getBytes(UTF_8));
		assertThat(getContent(dataURL("text/plain;charset=utf8;foo=bar,Hello%20World%20%E2%98%BA"))).
			isEqualTo("Hello World ☺".getBytes(UTF_8));
	}
}

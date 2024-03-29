package opwvhk.data_url;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.UnsupportedCharsetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("deprecation")
class DataURLStreamHandlerTest {
	@SuppressWarnings("SpellCheckingInspection")
	private static final String TEST_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==";

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

	@SuppressWarnings("SpellCheckingInspection")
	@Test
	void validateMimeTypePattern() {
		final Pattern pattern = DataURLStreamHandler.DATA_URL_PATTERN;

		assertPatternMatchesFirstChars(pattern, 6,  "data:,");
		assertPatternMatchesFirstChars(pattern, 6,  "data:,image/png,");
		assertPatternMatchesFirstChars(pattern, 18, "data:;charset=bla,");
		assertPatternMatchesFirstChars(pattern, 18, "data:;charset=bla,image/png,");
		assertPatternMatchesFirstChars(pattern, 23, "data:;charset=\"b ;,la\",image/png,");
		assertPatternMatchesFirstChars(pattern, 15,  "data:image/png,");
		assertPatternMatchesFirstChars(pattern, 15,  "data:image/png,aksfyirweu");
		assertPatternMatchesFirstChars(pattern, 22,  "data:image/png;base64,aksfyirweu");
		assertPatternMatchesFirstChars(pattern, 32, "data:image/png;foo=\"b ;\\\\,\\\"ar\",aksfyirweu");
		assertPatternMatchesFirstChars(pattern, 39, "data:image/png;foo=\"b ;\\\\,\\\"ar\";base64,aksfyirweu");
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

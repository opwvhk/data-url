package opwvhk.data_url;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataURLStreamHandlerProviderTest {

	public static final String RFC2397_DEFAULT_CONTENT_TYPE = "text/plain;charset=US-ASCII";

	@Test
	void validateMinimalDataURL() throws IOException {
		// This happy flow succeeds if:
		// - DataURLStreamHandlerProvider is a registered service
		// - DataURLStreamHandlerProvider provides a URLStreamHandler that can handle data URLs (DataURLStreamHandler)
		// - DataURLStreamHandler creates a URLConnection (DataURLConnection)
		// - DataURLConnection reads no data (type:

		final URL url = new URL("data:,");
		final URLConnection connection = url.openConnection();

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try (InputStream inputStream = connection.getInputStream()) {
			inputStream.transferTo(buffer);
		}

		assertThat(buffer.toByteArray()).isEmpty();
		assertThat(connection.getContentLength()).isEqualTo(0);
		assertThat(connection.getContentType()).isEqualTo(RFC2397_DEFAULT_CONTENT_TYPE);
	}

	@Test
	void checkProviderDoesNotClaimTooMuch() {
		// This test assumes our provider is used, so this test is only valid if validateOneHappyFlow succeeds.
		assertThatThrownBy(() -> new URL("unknown://host:12/whatever")).isInstanceOf(MalformedURLException.class);
	}
}

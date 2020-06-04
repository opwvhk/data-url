package opwvhk.data_url;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.*;

class DataURLConnectionTest {

	private String mimeType;
	private byte[] content;
	private DataURLConnection connection;

	@BeforeEach
	void setUp() throws IOException {
		final URL url = new URL("data:,anything");
		mimeType = "application/octet-stream";
		content = "Hello World!".getBytes(US_ASCII);
		connection = new DataURLConnection(url, mimeType, content);
	}

	@Test
	void validateSimulatedResponseHeaders() {
		assertThat(connection.getHeaderFields()).containsOnly(
			Assertions.entry("content-type", List.of(mimeType)),
			Assertions.entry("content-length", List.of(String.valueOf(content.length)))
		);

		assertThat(connection.getHeaderField("missing")).isNull();
		assertThat(connection.getHeaderField("content-type")).isEqualTo(mimeType);
		assertThat(connection.getHeaderFieldInt("content-length", 0)).isEqualTo(content.length);

		Set<String> names = new HashSet<>();
		names.add(connection.getHeaderFieldKey(0));
		names.add(connection.getHeaderFieldKey(1));
		assertThat(connection.getHeaderFieldKey(2)).isNull();
		assertThat(names).containsOnly("content-type", "content-length");

		Set<String> values = new HashSet<>();
		values.add(connection.getHeaderField(0));
		values.add(connection.getHeaderField(1));
		assertThat(connection.getHeaderField(2)).isNull();
		assertThat(values).containsOnly(mimeType, String.valueOf(content.length));
	}

	@Test
	void checkDataUrlsAreReadOnly() {
		// data: URLs can be read, unless you don't allow it
		connection.setDoInput(false);
		assertThatThrownBy(connection::getInputStream).isInstanceOf(ProtocolException.class);

		// data: URLs can NOT be written to
		assertThatThrownBy(connection::getOutputStream).isInstanceOf(UnknownServiceException.class);
	}

	@Test
	void checkConnectingIsIdempotent() throws IOException {
		InputStream inputStream1 = connection.getInputStream();
		connection.connect();
		InputStream inputStream2 = connection.getInputStream();

		assertThat(inputStream1).isSameAs(inputStream2);
	}
}

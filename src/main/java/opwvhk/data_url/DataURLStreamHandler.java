package opwvhk.data_url;

import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A stream protocol handler for the {@code data:} protocol.
 *
 * @see <a href="https://www.ietf.org/rfc/rfc2397.txt">RFC 2397 - The "data" URL scheme</a>
 */
public class DataURLStreamHandler extends URLStreamHandler {
	public static final String PROTOCOL = "data";
	public static final String DEFAULT_MIMETYPE = "text/plain";
	public static final String CHARSET_PARAMETER_PREFIX = ";charset=";
	public static final String DEFAULT_FULL_MIMETYPE = DEFAULT_MIMETYPE + CHARSET_PARAMETER_PREFIX + "US-ASCII";
	/**
	 * Regex pattern to match MIME types at the start of a String. The entire match is the MIME type.
	 */
	static final Pattern STARTS_WITH_MIME_TYPE_PATTERN;
	/**
	 * Regex pattern to match MIME type parameters. Matching must start at the first semicolon, and will subsequently
	 * match all parameters. Each match results in two named groups, {@code name} and {@code value}.
	 */
	static final Pattern MIME_TYPE_PARAMETER;
	private static final String BASE64_PARAMETER = ";base64";

	static {
		final String token = "[-!#$%&'*+.^_`|~\\da-zA-Z]+";
		final String quotedString = "\"(?:\\\\.|[^\"\\\\])*\"";
		final String parameter = token + "=(?:" + token + "|" + quotedString + ")";

		STARTS_WITH_MIME_TYPE_PATTERN = Pattern.compile(
			"^(?:" + token + "/" + token + "(?:;" + parameter + ")*|;charset=(?:" + token + "|" + quotedString + ")|)");
		MIME_TYPE_PARAMETER = Pattern.compile(
			"\\G;(?<name>" + token + ")=(?<value>" + token + "|" + quotedString + ")");
	}

	@Override
	protected DataURLConnection openConnection(final URL url) {
		if (!PROTOCOL.equals(url.getProtocol())) {
			throw new IllegalArgumentException("Unsupported protocol: " + url.getProtocol());
		}

		final String fullUrl = url.toExternalForm();
		final String dataUrlSpecific = fullUrl.substring(PROTOCOL.length() + 1);
		final Matcher mimeTypeMatcher = STARTS_WITH_MIME_TYPE_PATTERN.matcher(dataUrlSpecific);
		//noinspection ResultOfMethodCallIgnored: the pattern is guaranteed to match the beginning of any string
		mimeTypeMatcher.find();
		final String rawMimeType = mimeTypeMatcher.group(0);
		final String mimeType;
		if (rawMimeType.isEmpty()) {
			mimeType = DEFAULT_FULL_MIMETYPE;
		} else if (rawMimeType.startsWith(CHARSET_PARAMETER_PREFIX)) {
			mimeType = DEFAULT_MIMETYPE + rawMimeType;
		} else {
			mimeType = rawMimeType;
		}

		final String pastMimeType = dataUrlSpecific.substring(mimeTypeMatcher.end());
		final boolean usesBase64 = pastMimeType.startsWith(BASE64_PARAMETER);
		final String pastEncoding = usesBase64 ? pastMimeType.substring(BASE64_PARAMETER.length()) : pastMimeType;
		if (!pastEncoding.startsWith(",")) {
			throw new IllegalArgumentException("Malformed data URL (expected a comma after the optional MIME type): " + fullUrl);
		}

		final String dataPart = pastEncoding.substring(1);
		final byte[] contents;
		if (usesBase64) {
			contents = Base64.getDecoder().decode(dataPart);
		} else {
			final Charset charset = getCharset(mimeType);
			final String urlDecoded = URLDecoder.decode(dataPart, charset);
			contents = urlDecoded.getBytes(charset);
		}

		return new DataURLConnection(url, mimeType, contents);
	}

	Charset getCharset(final String mimeType) {
		final int firstSemicolon = mimeType.indexOf(';');
		if (firstSemicolon >= 0) {
			final Matcher parameterMatcher = MIME_TYPE_PARAMETER.matcher(mimeType.substring(firstSemicolon));
			while (parameterMatcher.find()) {
				if ("charset".equals(parameterMatcher.group("name"))) {
					return Charset.forName(parameterMatcher.group("value"));
				}
			}
		}
		// If we get here, no charset was found. Return the default.
		return StandardCharsets.US_ASCII;
	}

	@Override
	protected DataURLConnection openConnection(final URL url, final Proxy ignored) {
		return openConnection(url);
	}
}

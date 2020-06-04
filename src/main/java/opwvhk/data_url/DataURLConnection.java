package opwvhk.data_url;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataURLConnection extends URLConnection {
	private final String mimeType;
	private final byte[] contents;

	private InputStream inputStream;
	private Map<String, List<String>> fields;
	private List<String> fieldNames;


	DataURLConnection(final URL url, final String mimeType, final byte[] contents) {
		super(url);
		this.mimeType = mimeType;
		this.contents = contents;
	}

	public void connect() {
		if (!connected) {
			inputStream = new ByteArrayInputStream(contents);
			fields = new LinkedHashMap<>();
			fields.put("content-length", List.of(Integer.toString(contents.length)));
			fields.put("content-type", List.of(mimeType));
			fieldNames = new ArrayList<>(fields.keySet());

			connected = true;
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (!doInput) {
			throw new ProtocolException("Cannot read from URLConnection if doInput=false (call setDoInput(true))");
		}
		connect();
		return inputStream;
	}

	@Override
	public String getHeaderField(String name) {
		connect();
		return fields.getOrDefault(name.toLowerCase(), List.of()).stream().findFirst().orElse(null);
	}

	@Override
	public Map<String, List<String>> getHeaderFields() {
		connect();
		return fields;
	}

	@Override
	public String getHeaderFieldKey(int n) {
		connect();
		return n < fieldNames.size() ? fieldNames.get(n) : null;
	}

	@Override
	public String getHeaderField(int n) {
		String name = getHeaderFieldKey(n);
		return name != null ? getHeaderField(name) : null;
	}
}

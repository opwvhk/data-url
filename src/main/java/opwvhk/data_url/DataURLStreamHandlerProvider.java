package opwvhk.data_url;

import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

public class DataURLStreamHandlerProvider extends URLStreamHandlerProvider {
	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		return DataURLStreamHandler.PROTOCOL.equals(protocol) ? new DataURLStreamHandler() : null;
	}
}

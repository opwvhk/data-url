![Build Status](https://github.com/opwvhk/data-url/workflows/Java%20CI%20with%20Maven/badge.svg)

# data-url

By default, the Java class `java.net.URL` does not support [`data:` URLs](https://www.ietf.org/rfc/rfc2397.txt).

This library hooks into the Java ServiceLoader architecture for URL stream handlers and provides support for `data:` URLs.
To use it, simply add it to the classpath.

For e.g. Maven, this means adding this dependency:

	<dependency>
		<groupId>net.fs.opk</groupId>
		<artifactId>data-url</artifactId>
		<version>1.0</version>
	</dependency>

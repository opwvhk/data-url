![Build Status](https://github.com/opwvhk/data-url/workflows/Java%20CI%20with%20Maven/badge.svg)
[![license](doc/license-APACHE-2.0-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

# data-url

Support for `data:` URLs for JVM languages.


## Rationale

By default, the Java class `java.net.URL` does not support `data:` URLs (specified in [RFC 2397](https://www.ietf.org/rfc/rfc2397.txt)).

As a result, code snippets like this throw a `MalformedURLException` (unknown protocol) on the first line:

```java
java.net.URL url = new java.net.URL("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==");
java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(URL);
```

## Usage

This library hooks into the Java ServiceLoader architecture for URL stream handlers and provides support for `data:` URLs.
To use it, simply add it to the classpath.

For e.g. Maven, this means adding this dependency:

```xml
<dependency>
	<groupId>net.fs.opk</groupId>
	<artifactId>data-url</artifactId>
	<version>1.0</version>
</dependency>
```

With this dependency on the classpath, the code snippet above succeeds, and reads an image: ![red dot](doc/red-dot-5px.png "red dot")

<span style="font-size: 0.6em">(note that an actual image file is used in this readme because GitHub does not allow `data:`URLs)</span>
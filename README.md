# bb4-xml

[![CI](https://github.com/bb4/bb4-xml/actions/workflows/ci.yml/badge.svg)](https://github.com/bb4/bb4-xml/actions/workflows/ci.yml)

📊 [Build status for all bb4 projects](https://github.com/bb4/.github)

DOM helpers used across bb4 projects: parse, query, transform, and write XML documents.

## Using it

```groovy
implementation 'com.barrybecker4:bb4-xml:3.0-SNAPSHOT'
```

Catalog consumers can write `implementation bb4Libs.xml`.

See the [releases page](https://github.com/bb4/bb4-xml/releases) or [Maven Central](https://central.sonatype.com/artifact/com.barrybecker4/bb4-xml) for newer versions.

## What's inside

- **`DomUtil`** — convenience facade for common DOM operations
- **`DomBuilder`** — create empty / rooted documents
- **`DomParser`** — parse from URL or file (skips external DTD loading)
- **`DomQuery`** — attributes and string dumps of the tree
- **`DomWriter`** — serialize a document to a file
- **`DomTransformer`** — whitespace cleanup and `<use ref>` deep-copy substitution
- **`XmlErrorHandler`** — SAX error handler with stdout/stderr logging

### Demo (not published)

**`DomEcho`** is a Swing DOM tree viewer kept under `scala-test` so it is not part of the library JAR.

```bash
./gradlew runDomEcho
./gradlew runDomEcho --args=path/to/file.xml
```

## Building from source

See the [Building bb4 Projects wiki](https://github.com/bb4/bb4-common/wiki/Building-bb4-Projects).

## License

MIT — see [LICENSE](LICENSE).

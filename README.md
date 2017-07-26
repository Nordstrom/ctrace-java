# ctrace-go
[![Build Status][ci-img]][ci] [![Coverage Status][cov-img]][cov] [![OpenTracing 1.0 Enabled][ot-img]][ot-url]

[Canonical OpenTracing](https://github.com/Nordstrom/ctrace) for Java

## Why
[OpenTracing](http://opentracing.io) is a young specification and for most (if not all) SDK implementations, output format and wire protocol are specific to the backend platform implementation.  ctrace attempts to decouple the format and wire protocol from the backend tracer implementation.

## What
ctrace specifies a canonical format for trace logs.  By default the logs are output to stdout but you can configure them to go to any WritableStream.

## Required Reading
To fully understand this platform API, it's helpful to be familiar with the [OpenTracing project](http://opentracing.io) project, [terminology](http://opentracing.io/documentation/pages/spec.html), and [ctrace specification](https://github.com/Nordstrom/ctrace) more specifically.


## Contributing
Please see the [contributing guide](CONTRIBUTING.md) for details on contributing to ctrace-go.

## License
[Apache License 2.0](LICENSE)

[ci-img]: https://travis-ci.org/Nordstrom/ctrace-java.svg
[ci]: https://travis-ci.org/Nordstrom/ctrace-java
[cov-img]: https://coveralls.io/repos/github/Nordstrom/ctrace-java/badge.svg
[cov]: https://coveralls.io/github/Nordstrom/ctrace-java
[ot-img]: https://img.shields.io/badge/OpenTracing--1.0-enabled-blue.svg
[ot-url]: http://opentracing.io

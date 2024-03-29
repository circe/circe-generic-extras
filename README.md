# circe-generic-extras

[![Build](https://github.com/circe/circe-generic-extras/workflows/Continuous%20Integration/badge.svg)](https://github.com/circe/circe-generic-extras/actions)
[![Coverage status](https://img.shields.io/codecov/c/github/circe/circe-generic-extras/master.svg)](https://codecov.io/github/circe/circe-generic-extras)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/circe/circe)
[![Maven Central](https://img.shields.io/maven-central/v/io.circe/circe-generic-extras_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-generic-extras_2.12)

This library provides configurable generic derivation and other functionality extending the generic
derivation mechanisms provided in [Circe][circe]'s circe-generic module.
The circe-generic-extras module used to be included in the core Circe repository, but it was moved
to this separate repository after 0.12.1, in preparation for Circe's 1.0.0 release.

This library is experimental and its development is primarily community-supported.

## Usage

This library as functionality to parse JSON in different cased formats such as `snake_case`, `kebab-case`, `PascalCase` or `SCREAMING_SNAKE_CASE`.

You can enable different case modes by making a custom configuration.

```json
{
  "foo-bar": "foobar"
}
```

```scala
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration

case class Foo(fooBar: String)

implicit val customConfig: Configuration = Configuration.default.withKebabCaseMemberNames

implicit val fooEncoder: Encoder[Foo] = deriveConfiguredEncoder
implicit val fooDecoder: Decoder[Foo] = deriveConfiguredDecoder
```

## Versioning

This library releases at a different cadence than [Circe], any apparent relationship between the [Circe] version numbers and generic-extras version numbers are a coincidence and NOT intentional.  Please use the following table for determining capability with the corresponding [Circe] versions.

| Circe Generic Extras Version | Circe  | Scala 2 | Scala 3 | Scala JVM | Scala.JS | Scala Native |
|------------------------------|--------|---------|---------|-----------|----------|--------------|
| 0.14.3 and later             | 0.14.x | ✅      | ❌      | ✅        | ✅       | ✅           |
| 0.14.2                       | 0.14.x | ✅      | ❌      | ✅        | ✅       | ❌️           |
| 0.14.1                       | 0.14.x | ✅      | ❌      | ✅        | ✅       | ❌️           |
| 0.14.0                       | 0.14.x | ✅      | ❌      | ✅        | ✅       | ❌️           |

## Contributors and participation

All Circe projects support the [Scala code of conduct][code-of-conduct] and we want
all of their channels (Gitter, GitHub, etc.) to be welcoming environments for everyone.

Please see the [Circe contributors' guide][contributing] for details on how to submit a pull
request.

## License

circe-generic-extras is licensed under the **[Apache License, Version 2.0][apache]**
(the "License"); you may not use this software except in compliance with the
License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[apache]: http://www.apache.org/licenses/LICENSE-2.0
[circe]: https://github.com/circe/circe
[code-of-conduct]: https://www.scala-lang.org/conduct/
[contributing]: https://circe.github.io/circe/contributing.html

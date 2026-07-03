# outline-parser - Changelog

## 2.0.0 - 2026-07-03

### General

- **(BREAKING)** Renamed Scala packages from `com.financialforce.*` to `io.github.apexdevtools.*` to align with the Maven group ID `io.github.apex-dev-tools`.
  - `com.financialforce.oparser` → `io.github.apexdevtools.oparser`
  - `com.financialforce.types` → `io.github.apexdevtools.types`
  - `com.financialforce.types.base` → `io.github.apexdevtools.types.base`
  - Consumers must update all imports.

- **(BREAKING)** Merged the `apex-types` library into this artifact. The previously separate `apex-types` dependency is no longer required and should be removed from consuming projects.
  - All type interfaces (`io.github.apexdevtools.types.*`), base types (`Modifier`, `Annotation`, `Location`), the `io.github.apexdevtools.api.*` Issue API, and the `io.github.apexdevtools.spi.AnalysisProvider` SPI are now provided directly by `outline-parser`.

- **(BREAKING)** `Location` semantics changed to match ANTLR conventions:
  - `lineOffset` is now 0-based (previously 1-based).
  - End positions are now exclusive — locations represent the half-open interval `[start, end)` for both columns and byte offsets.

- **(BREAKING)** Removed `intern()` methods and static caches on `Annotation` and `Modifier` companion objects (and corresponding `intern()` calls in `Types.scala` factory methods). The caches ignored the `location` field when comparing equality, returning cached instances with wrong locations under parallel use. Consumers relying on `intern()` should construct instances directly.

- Added optional `location` field to `Modifier` and `Annotation`, populated during parsing:
  - Compound modifiers (e.g. `with sharing`) get a spanned location covering both tokens.
  - Annotations get a span from `@` through any parameter list.

- Fixed multi-line string tokenization (Salesforce Summer '26 `'''…'''` literals) to correctly handle multi-byte characters and avoid mis-tokenizing trailing content.

### Build & dependencies

- Removed the `apex-ls` test dependency. The JVM comparison test suite now uses `apex-parser 5.1.0` directly via `ApexParserFactory`, eliminating the outline-parser ↔ apex-ls cycle. JS-side sample comparison is dropped; the JS test suite is reduced to `SmokeTest`.
- Upgraded Scala.js to `1.18.2`.
- Updated sbt and plugin dependencies to latest stable versions.

### JS / NPM

- Bumped dev dep `@apexdevtools/apex-parser` to `^5.1.0` (was `^4.3.1`).
- Raised minimum Node version to `^20.19.0 || ^22.13.0 || >=24` (was `>=14.0.0`), matching the requirements of `apex-parser 5.x`.
- Moved `@apexdevtools/apex-parser` to `devDependencies`.

## 1.3.0

Last release of the pre-merge line. See git history for prior changes; this changelog begins with the 2.0.0 line.

# outline-parser - Changelog

## Unreleased

### API

- Added `Rule.id()` as a stable machine-readable rule identifier on JVM and Scala.js. Existing implementations remain compatible and default to `name()`; new rules should override it with a stable lowercase kebab-case ID. Existing `Issue` string output remains unchanged.

- **(BREAKING)** `Annotation.parameters`, the unparsed parameter string, is removed and replaced by `Annotation.parameterList`, a located, structured view of the parameter list.
  - Each `AnnotationParameter` carries its name (where one was written), its value, the separator written before it, and locations for the name, the value and the parameter as a whole.
  - The separator is recorded as `AnnotationParameterSeparator.Whitespace` or `AnnotationParameterSeparator.Comma`. Apex only accepts whitespace, but a comma is retained rather than rejected so that consumers can diagnose it.
  - `parameterList` is empty when the annotation was written without parentheses and an empty sequence when they were written but hold nothing, matching what `parameters` distinguished with `None` and `Some("")`.
  - Nothing is validated here. Names and values are left uninterpreted and forms Apex does not accept still parse.
  - `parameters` was a concatenation of token contents, so it could not represent the separator between two parameters. It is removed rather than deprecated so that no published version presents it as the way to read parameters.

- **(BREAKING)** `Annotation` equality, hashing and rendering are now over `parameterList`.
  - Equality is over the name and the parameters in the order and form they were written, so two annotations whose flattened text coincides but whose structure differs — `@Dummy(a=1 b=2)` and `@Dummy(a=1b=2)` — are no longer equal, and neither are lists that differ only in their separator. `location` is still excluded, as it is on `Modifier`, and `AnnotationParameter` excludes its own locations for the same reason.
  - `AnnotationParameter` compares names and values case-insensitively, as the platform treats them and as `parameters` was already compared.
  - `toString` is rebuilt from `parameterList` and now renders the separator that was written, so `@AuraEnabled(cacheable=true scope='global')` renders as written rather than as the whitespace-stripped `@AuraEnabled(cacheable=truescope='global')`. Whitespace is normalised to a single space, so the rendering is faithful but not verbatim. This also changes `IBodyDeclaration` output, which includes annotations.

### Build & dependencies

- Updated the `apex-parser` test dependency to 5.2.0, which tightens the annotation grammar. The ANTLR reference parser used by the sample comparison now builds a full `parameterList`, so the structured form is compared against a second parser across the sample corpus rather than only being produced.

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

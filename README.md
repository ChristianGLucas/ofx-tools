# ofx-tools

Deterministic parsing and inspection of **OFX** (Open Financial Exchange) and
**QFX** financial-statement files — the format banks, credit-card issuers, and
brokerages export for download into accounting software (Quicken, GnuCash,
etc.). Built for the [Axiom](https://axiomide.com) marketplace, handle
`christiangeorgelucas`.

OFX ships in two incompatible syntaxes and this package handles both:

- **OFX 1.x — SGML.** Tag-based, with an HTTP-header-like preamble
  (`OFXHEADER:100`, `VERSION:102`, ...) and **unclosed tags** — a leaf
  element's value runs until the next tag starts, there is no `</TAG>`.
- **OFX 2.x — XML.** Well-formed XML with an `<?OFX ...?>` processing
  instruction in place of the SGML header block.

`DetectFormat` and `ParseDocument` auto-detect which syntax a document uses
from its header and parse either one into the same normalized structure.

## Library

Wraps [`ofx4j`](https://github.com/stoicflame/ofx4j) (`com.webcohesion.ofx4j`,
Apache-2.0), the Java reference OFX client/parser library. Its
`NanoXMLOFXReader` implements the SGML implied-close-tag grammar directly and
auto-detects OFX 1 vs OFX 2 from the document header.

Full runtime dependency tree (verified via `mvn dependency:tree`, all
permissive): `commons-logging` 1.2 (Apache-2.0), `nanoxml` 2.2.3
(zlib/libpng), `jakarta.xml.bind-api` 4.0.0 + `jakarta.activation-api` 2.1.0
(BSD-3-Clause / EDL 1.0), `reflections` (WTFPL / New BSD), `javassist`
(triple-licensed MPL-1.1 / LGPL-2.1 / Apache-2.0 — used here under the
Apache-2.0 option).

**Note:** ofx4j's own POM transitively requests `org.reflections:reflections:
0.9.10`, which in turn pulls in `com.google.code.findbugs:annotations:2.0.1`
— an **LGPL-only** dependency. This package's `axiom.yaml` explicitly pins
`org.reflections:reflections:0.9.12` (a same-line, API-compatible version —
verified with the full test suite) to override that transitive request via
Maven's nearest-wins dependency mediation; 0.9.12 drops the
findbugs-annotations dependency entirely. Axiom's `build.maven_deps` does not
yet support Maven `<exclusions>`, so this direct-pin override is the
mechanism used instead — see the retrospective for the full reasoning.

This package only ever *parses a document the caller already has* — it never
implements OFX's separate bank-connection network protocol, never makes a
network call, and never reads the wall clock.

## Security posture

- Input is bounded to 5 MiB.
- Any input containing a `<!DOCTYPE` declaration is rejected outright before
  it ever reaches ofx4j's XML parser. ofx4j's OFX-2/XML reader uses a bare
  `org.xml.sax.helpers.XMLReaderFactory.createXMLReader()` with no
  external-entity/DTD hardening of its own, so this package supplies its own
  XXE guard rather than relying on the library.
- A malformed document — including adversarially deep SGML aggregate nesting,
  which can trigger a `StackOverflowError` in the library's recursive-descent
  reader — is converted into a structured `OfxError`, never an uncaught crash.

## Nodes

`DetectFormat` · `ParseDocument` · `ExtractAccounts` ·
`ExtractStatementPeriods` · `ExtractTransactions` · `ExtractBalances` ·
`ComputeTransactionTotals` · `FilterTransactionsByType` ·
`FilterTransactionsByDateRange` · `FilterTransactionsByAmountThreshold` ·
`ExtractCreditCardStatements` · `ExtractInvestmentTransactions` ·
`ExtractCurrency` · `NormalizeOfxDate` · `SummarizeTransactions` ·
`DetectDuplicateTransactions` · `ValidateStructure`

`ParseDocument` produces the canonical `OfxDocument` envelope; every
extraction/filter/summary node downstream consumes that envelope (or a
`TransactionList` derived from it) rather than re-parsing raw text.

## License

MIT — Copyright (c) 2026 Christian George Lucas.

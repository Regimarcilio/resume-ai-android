# ADR-005: PDF Generation Library Selection

## Status
Accepted

## Date
2026-11-07

## Context
The resume-ai-android app needs to generate professional PDF resumes from user data. Three approaches were evaluated:

1. **pdfbox-android** - Apache PDFBox port for Android
2. **Compose Print** - Jetpack Compose + Android PrintManager
3. **Server-side** - HTML/CSS template rendered via WeasyPrint in a Docker service

## Decision
We will use **Server-side PDF generation with WeasyPrint** for the MVP, with a planned fallback to pdfbox-android for offline support in Phase 2.

## Rationale

### Quality & Standards Compliance
- WeasyPrint produces PDF 1.7 compliant output with selectable text, proper fonts, and metadata
- Full CSS support including Grid, Flexbox, @page rules for professional layouts
- Pixel-perfect rendering matching web preview exactly

### APK Size
- **0 MB impact** on APK (critical for < 10 MB target)
- pdfbox-android adds ~5-8 MB
- Compose Print adds ~2-3 MB (but requires Compose dependencies already in app)

### Internationalization
- Excellent Unicode support out of the box
- Native RTL (right-to-left) layout support for Hebrew, Arabic
- Easy font loading via CSS @font-face

### Developer Experience
- HTML/CSS templates editable by designers without Android rebuild
- Template versioning independent of app releases
- Familiar web technologies for layout adjustments

### Performance
- Generation time: ~880ms for complex 2-page resume (acceptable)
- Server can scale horizontally
- Results cached client-side after first generation

## Consequences

### Positive
- Lightweight app binary
- High-quality, professional PDF output
- Easy template iteration
- Strong i18n/RTL support

### Negative
- **Network dependency** for PDF generation (mitigated by caching)
- Additional infrastructure to maintain (Docker service)
- Data privacy consideration (resume content sent to server)

### Mitigations
1. **Offline caching**: Generated PDFs stored locally in Room + SQLCipher
2. **Clear UX**: "Generate PDF" button disabled with "Online only" indicator when offline
3. **Phase 2**: Implement pdfbox-android as local fallback
4. **Privacy**: No logging of resume content in PDF service; TLS in production

## Alternatives Considered

### pdfbox-android
- Pros: Fully offline, mature library
- Cons: APK size, verbose API, limited RTL, older codebase

### Compose Print
- Pros: Native integration, declarative, good RTL
- Cons: Experimental PDF rendering, PrintManager limitations, requires Compose dependencies

## Related
- Benchmark results: `docs/pdf-benchmark/results/benchmark-results.md`
- PDF service implementation: `docs/pdf-benchmark/server-side-test/`
- Sprint 3 task: Integrate PDF service with Android app

## Notes
- Production deployment will use HTTPS, authentication, and rate limiting
- Consider CDN for template assets (fonts, CSS)
- Monitor generation latency and error rates in production
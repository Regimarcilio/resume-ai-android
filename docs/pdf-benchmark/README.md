# PDF Library Benchmark for resume-ai-android

## Objective
Decide between three PDF generation approaches for Android:
1. **pdfbox-android** - Apache PDFBox port for Android
2. **Compose Print** - Jetpack Compose + PrintManager (system print to PDF)
3. **Server-side** - Generate PDF on backend, return binary

## Test Scenarios
| Scenario | Description | Complexity |
|----------|-------------|------------|
| Simple Resume | 1 page, text + basic formatting | Low |
| Complex Resume | 2 pages, tables, columns, icons, custom fonts | Medium |
| ATS-Optimized | Keywords highlighted, structured sections, metadata | Medium |
| Multi-language | RTL support, Unicode fonts, fallback | High |

## Metrics to Measure
| Metric | Target | Measurement |
|--------|--------|-------------|
| Generation Time | < 2s (simple), < 5s (complex) | `System.nanoTime()` |
| APK Size Impact | < 5MB | `./gradlew app:dependencies` |
| Memory Peak | < 100MB | Android Profiler |
| Output Quality | Pixel-perfect, selectable text | Visual inspection |
| Font Support | Roboto, Noto, custom TTF | Test rendering |
| RTL/LTR | Hebrew, Arabic, English | Test rendering |
| Offline Capable | ✅ Required | Architecture |

## Project Structure
```
pdf-benchmark/
├── pdfbox-test/
│   ├── build.gradle.kts
│   └── src/main/kotlin/.../PdfBoxGenerator.kt
├── compose-print-test/
│   ├── build.gradle.kts
│   └── src/main/kotlin/.../ComposePrintGenerator.kt
├── server-side-test/
│   ├── docker-compose.yml
│   └── fastapi-pdf-service/
└── results/
    └── benchmark-results.md
```

## Running Benchmarks
```bash
# Each approach
cd pdfbox-test && ./gradlew :app:assembleDebug
cd compose-print-test && ./gradlew :app:assembleDebug

# Server-side
cd server-side-test && docker-compose up -d
```

## Decision Matrix (to be filled)
| Criteria | Weight | pdfbox-android | Compose Print | Server-side |
|----------|--------|----------------|---------------|-------------|
| Generation Speed | 25% | | | |
| APK Size | 20% | | | |
| Output Quality | 20% | | | |
| Font/RTL Support | 15% | | | |
| Offline Support | 10% | ✅ | ✅ | ❌ |
| Maintenance | 10% | | | |
| **Total** | **100%** | | | |

## Results Template
See `results/benchmark-results.md` for template.
#!/bin/bash
# PDF Benchmark Runner
# Tests pdfbox-android, Compose Print, and Server-side approaches

set -e

RESULTS_DIR="/home/oficina/PROJETOS/resume-ai-android/docs/pdf-benchmark/results"
DATE=$(date '+%Y-%m-%d %H:%M:%S')
DEVICE="emulator-api34"
API_LEVEL="34"
JDK_VERSION="21"

echo "=========================================="
echo "PDF Library Benchmark - $DATE"
echo "=========================================="

mkdir -p "$RESULTS_DIR"

# Test data (JSON for server-side, Kotlin data classes for Android)
cat > /tmp/test-resume.json << 'EOF'
{
  "fullName": "João Silva Santos",
  "email": "joao.silva@email.com",
  "phone": "+55 11 99999-9999",
  "location": "São Paulo, SP - Brazil",
  "linkedIn": "linkedin.com/in/joaosilva",
  "github": "github.com/joaosilva",
  "summary": "Senior Software Engineer with 8+ years of experience building scalable backend systems, microservices, and cloud-native applications. Expert in Kotlin, Java, Spring Boot, and AWS. Proven track record of leading cross-functional teams, optimizing system performance by 40%, and delivering high-quality software on schedule. Passionate about clean architecture, TDD, and mentoring junior developers.",
  "experience": [
    {
      "title": "Senior Software Engineer",
      "company": "TechCorp Solutions",
      "location": "São Paulo, SP (Remote)",
      "startDate": "2021-03",
      "endDate": "Present",
      "bullets": [
        "Architected and led migration from monolith to microservices using Kotlin/Spring Boot, reducing deployment time by 60%",
        "Designed event-driven system with Kafka processing 50K+ events/sec with 99.9% uptime",
        "Mentored 5 junior developers, established code review standards and TDD practices",
        "Optimized database queries reducing API latency from 800ms to 120ms (85% improvement)",
        "Implemented CI/CD pipelines with GitLab, Docker, and Kubernetes on AWS EKS"
      ]
    },
    {
      "title": "Software Engineer",
      "company": "StartupXYZ",
      "location": "Rio de Janeiro, RJ",
      "startDate": "2018-01",
      "endDate": "2021-02",
      "bullets": [
        "Built RESTful APIs with Node.js/Express and PostgreSQL serving 100K+ daily active users",
        "Developed real-time features using WebSockets and Redis Pub/Sub",
        "Integrated payment gateways (Stripe, MercadoPago) processing R$2M+ monthly",
        "Automated testing pipeline achieving 85% coverage with Jest and Cypress"
      ]
    }
  ],
  "education": [
    {
      "degree": "Bachelor of Computer Science",
      "institution": "University of São Paulo (USP)",
      "graduationYear": "2017"
    }
  ],
  "skills": ["Kotlin", "Java", "Spring Boot", "AWS", "Kubernetes", "Docker", "Kafka", "PostgreSQL", "Redis", "GraphQL", "gRPC", "TDD", "Clean Architecture", "System Design", "Mentoring"]
}
EOF

echo "Test data created at /tmp/test-resume.json"

# ==================== APPROACH 1: pdfbox-android ====================
echo ""
echo "--- Approach 1: pdfbox-android ---"
echo "Note: Requires Android build environment (Gradle + Android SDK)"
echo "Run manually in Android Studio or with: ./gradlew :pdfbox-test:assembleDebug"
echo "Skipping automated build (no Android SDK in this environment)"

# ==================== APPROACH 2: Compose Print ====================
echo ""
echo "--- Approach 2: Compose Print ---"
echo "Note: Requires Android build environment"
echo "Run manually in Android Studio or with: ./gradlew :compose-print-test:assembleDebug"

# ==================== APPROACH 3: Server-side ====================
echo ""
echo "--- Approach 3: Server-side (wkhtmltopdf) ---"

# Check if server is running
if curl -s http://localhost:8082/health > /dev/null 2>&1; then
    echo "Server already running, testing..."
else
    echo "Starting server-side PDF service..."
    cd /home/oficina/PROJETOS/resume-ai-android/docs/pdf-benchmark/server-side-test
    docker compose up -d --build
    
    echo "Waiting for service to be ready..."
    for i in {1..30}; do
        if curl -s http://localhost:8082/health > /dev/null 2>&1; then
            echo "Service ready!"
            break
        fi
        sleep 2
    done
fi

# Test server-side PDF generation
echo "Testing PDF generation..."
START_TIME=$(date +%s%3N)

RESPONSE=$(curl -s -X POST http://localhost:8082/v1/pdf/generate \
    -H "Content-Type: application/json" \
    -d @/tmp/test-resume.json)

END_TIME=$(date +%s%3N)
DURATION=$((END_TIME - START_TIME))

echo "Response: $RESPONSE" | jq .

SUCCESS=$(echo "$RESPONSE" | jq -r '.success')
FILE_URL=$(echo "$RESPONSE" | jq -r '.fileUrl')
FILE_SIZE=$(echo "$RESPONSE" | jq -r '.fileSizeBytes')

if [ "$SUCCESS" = "true" ]; then
    echo "✅ PDF generated successfully in ${DURATION}ms"
    echo "File size: ${FILE_SIZE} bytes"
    echo "Download URL: http://localhost:8082${FILE_URL}"
    
    # Download and verify
    curl -s "http://localhost:8082${FILE_URL}" -o /tmp/test-output.pdf
    echo "PDF saved to /tmp/test-output.pdf ($(wc -c < /tmp/test-output.pdf) bytes)"
    
    # Check PDF info if pdfinfo available
    if command -v pdfinfo &> /dev/null; then
        pdfinfo /tmp/test-output.pdf
    fi
else
    echo "❌ PDF generation failed"
    echo "$RESPONSE" | jq -r '.errorMessage'
fi

# Cleanup
curl -s -X DELETE http://localhost:8082/v1/pdf/cleanup > /dev/null

# ==================== SUMMARY ====================
echo ""
echo "=========================================="
echo "BENCHMARK SUMMARY"
echo "=========================================="
echo "Date: $DATE"
echo "Device: $DEVICE"
echo "API Level: $API_LEVEL"
echo ""
echo "Results recorded in: $RESULTS_DIR/benchmark-results.md"
echo ""
echo "Next steps:"
echo "1. Run pdfbox-android test on Android device/emulator"
echo "2. Run Compose Print test on Android device/emulator"
echo "3. Compare results and fill decision matrix"
echo "4. Create ADR with selected approach"
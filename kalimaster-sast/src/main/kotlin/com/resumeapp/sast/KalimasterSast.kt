package com.resumeapp.sast

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

/**
 * Kalimaster SAST - Static Application Security Testing
 * OWASP 2025 compliant security scanner for Kotlin/Android projects
 * 
 * Scans for:
 * - Hardcoded secrets (API keys, passwords, tokens)
 * - Insecure crypto usage
 * - SQL injection vulnerabilities
 * - Path traversal
 * - Insecure network communication
 * - Logging of sensitive data
 * - Weak random number generation
 * - Insecure file permissions
 * - Unsafe deserialization
 * - Hardcoded IPs/URLs
 */
class KalimasterSast {

    private val gson = Gson()
    private val findings = mutableListOf<Finding>()
    private val filesScanned = AtomicInteger(0)
    private val linesScanned = AtomicInteger(0)
    
    // Patterns for common security issues
    private val patterns = mapOf(
        "HARDCODED_API_KEY" to """(?i)(api[_-]?key|apikey|access[_-]?token|secret[_-]?key)\s*[:=]\s*["'][^"']+["']""".toRegex(),
        "HARDCODED_PASSWORD" to """(?i)(password|passwd|pwd)\s*[:=]\s*["'][^"']+["']""".toRegex(),
        "HARDCODED_SECRET" to """(?i)(secret|private[_-]?key)\s*[:=]\s*["'][^"']+["']""".toRegex(),
        "INSECURE_CRYPTO_MD5" to """MessageDigest\.getInstance\s*\(\s*["']MD5["']""".toRegex(),
        "INSECURE_CRYPTO_SHA1" to """MessageDigest\.getInstance\s*\(\s*["']SHA1["']""".toRegex(),
        "INSECURE_RANDOM" to """java\.util\.Random""".toRegex(),
        "SQL_INJECTION" to """rawQuery\s*\(\s*["'].*\+.*["']""".toRegex(),
        "PATH_TRAVERSAL" to """File\s*\(\s*.*\.\./""".toRegex(),
        "INSECURE_HTTP" to """http://[^"\s]+""".toRegex(),
        "LOGGING_SENSITIVE" to """Log\.(d|i|w|e|v)\s*\([^)]*(?i)(password|token|key|secret|credit|cpf|cnpj)[^)]*\)""".toRegex(),
        "HARDCODED_IP" to """\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b""".toRegex(),
        "UNSAFE_DESERIALIZATION" to """ObjectInputStream|readObject|Gson.*fromJson\s*\([^)]*Object""".toRegex(),
        "WORLD_WRITABLE_FILE" to """MODE_WORLD_WRITEABLE|MODE_WORLD_READABLE""".toRegex(),
        "DEBUG_LOG" to """BuildConfig\.DEBUG|Log\.(d|v)""".toRegex(),
    )

    data class Finding(
        val ruleId: String,
        val severity: Severity,
        val file: String,
        val line: Int,
        val column: Int,
        val message: String,
        val codeSnippet: String,
        val remediation: String
    ) {
        fun toJson(): String = Gson().toJson(this)
    }

    enum class Severity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }

    fun scan(projectRoot: String): ScanResult {
        val rootPath = Paths.get(projectRoot)
        val ktFiles = findFiles(rootPath, ".kt")
        val javaFiles = findFiles(rootPath, ".java")
        val xmlFiles = findFiles(rootPath, ".xml")
        val gradleFiles = findFiles(rootPath, ".gradle")
        val allFiles = ktFiles + javaFiles + xmlFiles + gradleFiles

        allFiles.forEach { file ->
            scanFile(file)
            filesScanned.incrementAndGet()
        }

        return ScanResult(
            totalFiles = filesScanned.get(),
            totalLines = linesScanned.get(),
            findings = findings.toList(),
            criticalCount = findings.count { it.severity == Severity.CRITICAL },
            highCount = findings.count { it.severity == Severity.HIGH },
            mediumCount = findings.count { it.severity == Severity.MEDIUM },
            lowCount = findings.count { it.severity == Severity.LOW },
            infoCount = findings.count { it.severity == Severity.INFO }
        )
    }

    private fun findFiles(root: Path, extension: String): List<File> {
        val files = mutableListOf<File>()
        Files.walk(root)
            .filter { it.toString().endsWith(extension) && !it.toString().contains("/build/") && !it.toString().contains("/.gradle/") }
            .forEach { files.add(it.toFile()) }
        return files
    }

    private fun scanFile(file: File) {
        val content = file.readText()
        val lines = content.lines().toList()
        linesScanned.addAndGet(lines.size)

        lines.forEachIndexed { index, line ->
            val lineNum = index + 1
            patterns.forEach { (ruleId, pattern) ->
                val matches = pattern.findAll(line)
                matches.forEach { match ->
                    val severity = when (ruleId) {
                        "HARDCODED_API_KEY", "HARDCODED_PASSWORD", "HARDCODED_SECRET" -> Severity.CRITICAL
                        "INSECURE_CRYPTO_MD5", "INSECURE_CRYPTO_SHA1", "SQL_INJECTION", "PATH_TRAVERSAL" -> Severity.HIGH
                        "INSECURE_RANDOM", "INSECURE_HTTP", "LOGGING_SENSITIVE", "UNSAFE_DESERIALIZATION" -> Severity.HIGH
                        "HARDCODED_IP", "WORLD_WRITABLE_FILE" -> Severity.MEDIUM
                        "DEBUG_LOG", "INSECURE_RANDOM" -> Severity.LOW
                        else -> Severity.INFO
                    }

                    val column = match.range.first
                    val codeSnippet = line.trim()
                    val remediation = getRemediation(ruleId)

                    findings.add(Finding(
                        ruleId = ruleId,
                        severity = severity,
                        file = file.absolutePath,
                        line = index + 1,
                        column = column,
                        message = getMessage(ruleId),
                        codeSnippet = codeSnippet,
                        remediation = remediation
                    ))
                }
            }
        }
    }

    private fun getMessage(ruleId: String): String = when (ruleId) {
        "HARDCODED_API_KEY" -> "Hardcoded API key detected"
        "HARDCODED_PASSWORD" -> "Hardcoded password detected"
        "HARDCODED_SECRET" -> "Hardcoded secret detected"
        "INSECURE_CRYPTO_MD5" -> "Insecure MD5 hash algorithm usage"
        "INSECURE_CRYPTO_SHA1" -> "Insecure SHA1 hash algorithm usage"
        "INSECURE_RANDOM" -> "Insecure random number generator usage"
        "SQL_INJECTION" -> "Potential SQL injection vulnerability"
        "PATH_TRAVERSAL" -> "Potential path traversal vulnerability"
        "INSECURE_HTTP" -> "Insecure HTTP communication (use HTTPS)"
        "LOGGING_SENSITIVE" -> "Sensitive data logged"
        "HARDCODED_IP" -> "Hardcoded IP address detected"
        "UNSAFE_DESERIALIZATION" -> "Unsafe deserialization detected"
        "WORLD_WRITABLE_FILE" -> "World-writable file permission"
        "DEBUG_LOG" -> "Debug logging enabled in production code"
        else -> "Security issue detected"
    }

    private fun getRemediation(ruleId: String): String = when (ruleId) {
        "HARDCODED_API_KEY" -> "Use Android Keystore or environment variables for API keys"
        "HARDCODED_PASSWORD" -> "Use Android Keystore or encrypted preferences for passwords"
        "HARDCODED_SECRET" -> "Use Android Keystore for secrets"
        "INSECURE_CRYPTO_MD5" -> "Use SHA-256 or SHA-3 instead of MD5"
        "INSECURE_CRYPTO_SHA1" -> "Use SHA-256 or SHA-3 instead of SHA1"
        "INSECURE_RANDOM" -> "Use SecureRandom for cryptographic operations"
        "SQL_INJECTION" -> "Use parameterized queries with Room DAOs"
        "PATH_TRAVERSAL" -> "Validate and sanitize file paths"
        "INSECURE_HTTP" -> "Use HTTPS for all network communication"
        "LOGGING_SENSITIVE" -> "Sanitize logs - never log passwords, tokens, PII"
        "HARDCODED_IP" -> "Use domain names with DNS resolution"
        "UNSAFE_DESERIALIZATION" -> "Validate input before deserialization; use safe libraries"
        "WORLD_WRITABLE_FILE" -> "Use MODE_PRIVATE for file creation"
        "DEBUG_LOG" -> "Remove debug logs in release builds; use Timber with release tree"
        else -> "Review and remediate"
    }

    fun generateReport(result: ScanResult, outputFile: String) {
        val report = JsonObject()
        report.addProperty("timestamp", System.currentTimeMillis())
        report.addProperty("totalFiles", result.totalFiles)
        report.addProperty("totalLines", result.totalLines)
        report.addProperty("criticalCount", result.criticalCount)
        report.addProperty("highCount", result.highCount)
        report.addProperty("mediumCount", result.mediumCount)
        report.addProperty("lowCount", result.lowCount)
        report.addProperty("infoCount", result.infoCount)

        val findingsJson = Gson().toJsonTree(result.findings)
        report.add("findings", findingsJson)

        File(outputFile).writeText(Gson().toJson(report))
    }

    data class ScanResult(
        val totalFiles: Int,
        val totalLines: Int,
        val findings: List<Finding>,
        val criticalCount: Int,
        val highCount: Int,
        val mediumCount: Int,
        val lowCount: Int,
        val infoCount: Int
    ) {
        fun hasBlockingFindings(): Boolean = criticalCount > 0 || highCount > 0
    }
}

fun main(args: Array<String>) {
    val projectRoot = if (args.isNotEmpty()) args[0] else "."
    val outputFile = if (args.size > 1) args[1] else "kalimaster-sast-report.json"

    println("🔍 Kalimaster SAST Starting...")
    println("📁 Scanning: $projectRoot")
    println("📄 Output: $outputFile")

    val scanner = KalimasterSast()
    val result = scanner.scan(projectRoot)

    println("\n📊 Scan Results:")
    println("   Files scanned: ${result.totalFiles}")
    println("   Lines scanned: ${result.totalLines}")
    println("   Critical: ${result.criticalCount}")
    println("   High: ${result.highCount}")
    println("   Medium: ${result.mediumCount}")
    println("   Low: ${result.lowCount}")
    println("   Info: ${result.infoCount}")

    scanner.generateReport(result, outputFile)
    println("\n✅ Report saved to: $outputFile")

    if (result.hasBlockingFindings()) {
        println("\n❌ BLOCKING FINDINGS DETECTED - Build should fail")
        System.exit(1)
    } else {
        println("\n✅ No blocking findings - Build can proceed")
        System.exit(0)
    }
}
# LGPD Legal Compliance Checklist

## Project: resume-ai-android
**Date**: 2026-11-07
**Status**: 🔴 Pending Legal Review
**Reviewer**: [Legal Team Name]

---

## Article-by-Article Compliance

### Art. 7º - Legal Bases for Processing
| Basis | Applied? | Justification | Document Reference |
|-------|----------|---------------|-------------------|
| Consent (I) | ✅ | Explicit opt-in for AI processing, analytics, marketing | `ConsentRepository`, Onboarding flow |
| Contract (V) | ✅ | Resume creation, PDF generation, subscription | Terms of Use §3 |
| Legitimate Interest (IX) | ⚠️ | Fraud prevention, security monitoring | Privacy Policy §4.2 |
| Legal Obligation (II) | ✅ | Tax records for subscriptions (5 years) | Terms of Use §7 |

### Art. 8º - Consent Requirements
- [ ] **Free**: No dark patterns, equal prominence accept/reject
- [ ] **Informed**: Plain language, specific purposes listed
- [ ] **Specific**: Separate consents for AI, analytics, marketing
- [ ] **Unambiguous**: Active opt-in (checkbox, not pre-ticked)
- [ ] **Revocable**: Easy withdrawal in Settings > LGPD
- [ ] **Recorded**: Timestamp, version, IP stored in `ConsentRepository`

### Art. 9º - Sensitive Data
| Data Category | Collected? | Legal Basis | Protection |
|---------------|------------|-------------|------------|
| Race/ethnicity | ❌ | N/A | N/A |
| Political opinion | ❌ | N/A | N/A |
| Religious belief | ❌ | N/A | N/A |
| Health data | ❌ | N/A | N/A |
| Biometric | ❌ | N/A | N/A |
| **Professional history** | ✅ | Contract + Consent | SQLCipher encryption |

### Art. 11º - Data Processing Principles
| Principle | Implementation |
|-----------|----------------|
| Purpose limitation | Only resume building, ATS scoring, subscription |
| Adequacy | Minimal data: name, email, phone, professional info |
| Necessity | No unnecessary fields (no SSN, no photo required) |
| Free access | "Meus Dados" screen exports all data |
| Data quality | User-editable, validation on input |
| Transparency | Privacy Policy accessible in-app and web |
| Security | SQLCipher AES-256, TLS 1.3, no plaintext logs |
| Prevention | Guardrail, rate limiting, SAST in CI |
| Non-discrimination | No automated decisions with legal effects |
| Accountability | DPO contact, ROPA maintained, incident response |

### Art. 12º - Security Measures
| Measure | Status | Evidence |
|---------|--------|----------|
| Encryption at rest | ✅ | SQLCipher (AES-256-GCM) |
| Encryption in transit | ✅ | TLS 1.3, certificate pinning |
| Pseudonymization | ✅ | User IDs not PII in analytics |
| Access control | ✅ | Hilt DI, no direct DB access |
| Incident response | ✅ | Runbook in `docs/incident-response.md` |
| Regular testing | ✅ | kalimaster-sast in CI, DAST quarterly |

### Art. 18º - Data Subject Rights
| Right | Implementation | API/Endpoint |
|-------|----------------|--------------|
| Confirmation & Access | ✅ "Meus Dados" screen + JSON export | `GET /user/data` |
| Correction | ✅ Editable profile, experience, education | `PUT /user/profile` |
| Anonymization/Blocking | ✅ "Anonimizar" button (Art. 16) | `POST /user/anonymize` |
| Portability | ✅ JSON/PDF export | `GET /user/export` |
| Deletion | ✅ "Excluir conta" with 30-day grace | `DELETE /user/account` |
| Information on sharing | ✅ Privacy Policy §5 lists subprocessors | In-app link |
| Revocation of consent | ✅ Granular toggles in Settings | `PUT /user/consent` |
| Opposition | ✅ Opt-out from analytics/marketing | `PUT /user/preferences` |
| Review of automated decisions | ✅ ATS score explanation + human review | ATS screen details |

### Art. 20º - International Transfer
- [ ] **Standard Contractual Clauses** with AI Gateway provider (OpenRouter)
- [ ] **Adequacy Decision** countries only (EU, US under EU-US DPF)
- [ ] **Binding Corporate Rules** not applicable
- [ ] **Specific consent** for transfer (included in AI consent)

### Art. 22º - Automated Decisions
| Decision | Human Review? | Explanation Provided? |
|----------|---------------|----------------------|
| ATS Score | ✅ Informational only | ✅ Breakdown + suggestions |
| AI Suggestions | ✅ User accepts/rejects | ✅ Inline with source text |
| Template Recommendation | ✅ User chooses | ✅ Reason shown |

**No solely automated decisions with legal effects** (Art. 20 LGPD)

### Art. 37-39 - DPO (Data Protection Officer)
- [ ] **DPO Appointed**: [Name], [Email: dpo@resumeapp.com]
- [ ] **Contact Published**: Privacy Policy §8, Settings > LGPD
- [ ] **Independence**: Reports to highest management
- [ ] **Resources**: Budget allocated for privacy program

### Art. 46-47 - Communication & Notification
| Event | Timeline | Method |
|-------|----------|--------|
| Security incident (risk to rights) | ≤ 72h to ANPD | Email + in-app banner |
| High-risk incident to users | ≤ 72h | Email + push notification |
| DPIA completion | Before processing | Internal record |
| Policy changes | 30 days notice | Email + in-app modal |

---

## Technical Implementation Checklist

### Data Minimization
- [x] Only required fields in registration (name, email)
- [x] Optional fields clearly marked (phone, LinkedIn, GitHub)
- [x] No sensitive data requested
- [x] AI processing uses minimal context (section-only)

### Encryption
- [x] **SQLCipher**: AES-256-GCM for Room database
- [x] **EncryptedSharedPreferences**: MasterKey AES-256 for keys
- [x] **DataStore**: Encrypted preferences for consent flags
- [x] **Network**: TLS 1.3, OkHttp certificate pinning
- [x] **PDF Service**: HTTPS only, no resume logging

### Access Control
- [x] Repository pattern - no direct DAO access from UI
- [x] Hilt DI - scoped dependencies
- [x] No debug logs with PII (guardrail strips)
- [x] kalimaster-sast scans for secrets in CI

### Retention & Deletion
| Data Type | Retention | Deletion Trigger |
|-----------|-----------|------------------|
| Resume content | Until user deletion | `DELETE /user/account` |
| AI cache | 30 days (TTL) | Automatic cleanup |
| Traces | 90 days | Automatic cleanup |
| Subscription records | 5 years (tax law) | Legal hold |
| Consent logs | 5 years | Legal hold |
| Analytics (pseudonymized) | 2 years | Automatic cleanup |

### Consent Management
- [x] **Granular consents**: AI, Analytics, Marketing, Crash Reporting
- [x] **Versioned**: `ConsentRecord` with version, timestamp, IP hash
- [x] **Revocable**: Settings screen toggles update `ConsentRepository`
- [x] **Proof**: Export includes consent history

### Privacy by Design
- [x] **Default privacy**: Analytics off, AI off, Crash reporting on
- [x] **Data minimization**: Section-level AI processing
- [x] **Purpose limitation**: Separate repositories per feature
- [x] **Storage limitation**: TTL on caches, user-controlled deletion

---

## Documentation Status

| Document | Status | Location | Last Review |
|----------|--------|----------|-------------|
| Privacy Policy | 🟡 Draft | `docs/legal-templates/privacy-policy.md` | - |
| Terms of Use | 🟡 Draft | `docs/legal-templates/terms-of-use.md` | - |
| Cookie/Tracking Policy | 🟡 Draft | `docs/legal-templates/cookie-policy.md` | - |
| ROPA (Record of Processing) | 🟡 Draft | `docs/legal-templates/ropa.md` | - |
| DPIA (Impact Assessment) | 🟡 Draft | `docs/legal-templates/dpia.md` | - |
| Incident Response Plan | ✅ Done | `docs/incident-response.md` | 2026-11-07 |
| Data Processing Agreements | 🔴 Pending | With vendors | - |
| DPO Appointment Letter | 🔴 Pending | Legal file | - |

---

## Vendor Assessment (Subprocessors)

| Vendor | Purpose | Data Shared | DPA Signed | Location |
|--------|---------|-------------|------------|----------|
| **Google Play Billing** | Payments | Transaction ID, email | ✅ Standard | US/EU |
| **OpenRouter (AI Gateway)** | AI processing | Resume sections (no PII) | 🔴 Pending | US |
| **Firebase Crashlytics** | Crash reports | Device info, stack traces | ✅ Standard | US |
| **Google Analytics (GA4)** | Analytics | Pseudonymized events | ✅ Standard | US |
| **PDF Service (self-hosted)** | PDF generation | Full resume HTML | ✅ Internal | BR |

---

## Sign-off

| Role | Name | Signature | Date |
|------|------|-----------|------|
| **Product Owner** | | | |
| **Lead Developer** | | | |
| **Legal Counsel** | | | |
| **DPO** | | | |
| **Security Lead** | | | |

---

## Next Actions
1. [ ] Legal review of Privacy Policy & Terms of Use drafts
2. [ ] Execute DPAs with OpenRouter, PDF service
3. [ ] Appoint DPO formally
4. [ ] Conduct DPIA for AI processing
5. [ ] Register with ANPD (if required - large scale)
6. [ ] Update ADR-004 with legal sign-off
7. [ ] Publish Privacy Policy on website and in-app
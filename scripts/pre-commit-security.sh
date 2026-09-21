#!/usr/bin/env bash
# ==============================================================================
# KernelCraft Local Git Pre-Commit Security Hook (OWASP MASVS / MASTG Gate)
#
# Intercepts git commit operations to enforce zero-tolerance security standards:
# 1. Blocks committed binary keystores, certificates, and private key files.
# 2. Blocks plaintext API keys, cloud credentials, and private key blocks.
# 3. Blocks regression to android:allowBackup="true" in XML manifests.
# 4. Enforces strict cleartext HTTP checks across staged code.
# ==============================================================================

set -e

RED="\033[1;31m"
GREEN="\033[1;32m"
YELLOW="\033[1;33m"
CYAN="\033[1;36m"
NC="\033[0m" # No Color

echo -e "${CYAN}==> [KernelCraft DevSecOps] Running pre-commit security verification...${NC}"

# Get staged files
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM 2>/dev/null || true)

if [ -z "$STAGED_FILES" ]; then
    echo -e "${GREEN}==> No staged files to inspect. Proceeding.${NC}"
    exit 0
fi

VIOLATIONS_FOUND=0

# ------------------------------------------------------------------------------
# 1. Reject Forbidden Credential & Keystore File Formats
# ------------------------------------------------------------------------------
FORBIDDEN_EXTENSIONS=("\\.jks$" "\\.keystore$" "\\.p12$" "\\.pkcs12$" "\\.pem$" "\\.key$" "\\.der$")

for file in $STAGED_FILES; do
    for pattern in "${FORBIDDEN_EXTENSIONS[@]}"; do
        if echo "$file" | grep -qE "$pattern"; then
            echo -e "${RED}[CRITICAL SECURITY VIOLATION] Staged private key or keystore file detected:${NC} $file"
            echo -e "   Remediation: Remove binary keys from git tracking immediately. Store in Android Keystore or local.properties."
            VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
        fi
    done
done

# ------------------------------------------------------------------------------
# 2. Secret & Private Key Inspection in Staged Diffs
# ------------------------------------------------------------------------------
STAGED_DIFF=$(git diff --cached -U0 2>/dev/null || true)

if echo "$STAGED_DIFF" | grep -qE "^\+[[:space:]]*-----BEGIN (RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----"; then
    echo -e "${RED}[CRITICAL SECURITY VIOLATION] Hardcoded private key block detected in staged commit.${NC}"
    echo -e "   Remediation: Do not embed private keys into source files. Use Android Keystore."
    VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
fi

if echo "$STAGED_DIFF" | grep -qE "^\+.*AIza[0-9A-Za-z-_]{35}"; then
    echo -e "${RED}[CRITICAL SECURITY VIOLATION] Google API key pattern staged in commit diff.${NC}"
    echo -e "   Remediation: Store API credentials in secure environment variables or secret build config."
    VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
fi

if echo "$STAGED_DIFF" | grep -qE "^\+.*AKIA[0-9A-Z]{16}"; then
    echo -e "${RED}[CRITICAL SECURITY VIOLATION] AWS Access Key ID detected in staged commit diff.${NC}"
    VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
fi

# ------------------------------------------------------------------------------
# 3. AndroidManifest & XML Directive Verification
# ------------------------------------------------------------------------------
XML_STAGED=$(echo "$STAGED_FILES" | grep -E "\.xml$" || true)

if [ -n "$XML_STAGED" ]; then
    for xml_file in $XML_STAGED; do
        if [ -f "$xml_file" ]; then
            # Reject allowBackup="true"
            if git diff --cached "$xml_file" | grep -qE '^\+.*android:allowBackup="true"'; then
                echo -e "${RED}[CRITICAL SECURITY VIOLATION] Staged diff introduces android:allowBackup=\"true\" in $xml_file${NC}"
                echo -e "   Remediation: android:allowBackup must remain \"false\" to prevent adb backup data leakage."
                VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
            fi

            # Check unauthorized component export
            if git diff --cached "$xml_file" | grep -qE '^\+.*<(service|receiver|provider)[^>]*android:exported="true"'; then
                echo -e "${RED}[HIGH SECURITY VIOLATION] Staged diff exports non-Activity component: $xml_file${NC}"
                echo -e "   Remediation: Background services and receivers must be private (android:exported=\"false\")."
                VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
            fi
        fi
    done
fi

# ------------------------------------------------------------------------------
# 4. Cleartext HTTP Detection in Source Files
# ------------------------------------------------------------------------------
SOURCE_STAGED=$(echo "$STAGED_FILES" | grep -E "\.(kt|java|xml)$" || true)

if [ -n "$SOURCE_STAGED" ]; then
    for src in $SOURCE_STAGED; do
        if [ -f "$src" ]; then
            # Find added http:// lines while filtering out known XML namespace schemas
            HTTP_LINES=$(git diff --cached "$src" | grep -E '^\+[[:space:]]*.*http://' | \
                         grep -v "schemas.android.com" | \
                         grep -v "www.w3.org" | \
                         grep -v "schema.gradle.org" || true)
            if [ -n "$HTTP_LINES" ]; then
                echo -e "${RED}[HIGH SECURITY VIOLATION] Insecure cleartext http:// URI introduced in $src:${NC}"
                echo -e "$HTTP_LINES"
                echo -e "   Remediation: Upgrade endpoints to https:// or use local Android asset resources."
                VIOLATIONS_FOUND=$((VIOLATIONS_FOUND + 1))
            fi
        fi
    done
fi

# ------------------------------------------------------------------------------
# Final Verdict
# ------------------------------------------------------------------------------
if [ $VIOLATIONS_FOUND -ne 0 ]; then
    echo -e "\n${RED}========================================================================${NC}"
    echo -e "${RED} [COMMIT REJECTED] Found $VIOLATIONS_FOUND security violation(s) in staged files.${NC}"
    echo -e "${RED} KernelCraft enforces a Zero-Tolerance OWASP MASVS security policy.${NC}"
    echo -e "${RED} Please correct the issues above and re-stage before committing.${NC}"
    echo -e "${RED}========================================================================${NC}\n"
    exit 1
fi

echo -e "${GREEN}==> [KernelCraft DevSecOps] All pre-commit security checks passed successfully.${NC}"
exit 0

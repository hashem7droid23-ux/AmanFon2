#!/usr/bin/env bash
# Run on your trusted Linux/macOS computer. Never in a public/shared AI container.
set -euo pipefail
set +x
umask 077
REPO='hashem7droid23-ux/AmanFon2'
for command in keytool python3; do command -v "$command" >/dev/null || { echo "Missing: $command"; exit 1; }; done
KEY_DIR="$HOME/.aman-phone-signing"
mkdir -p "$KEY_DIR"
KEYSTORE="$KEY_DIR/aman-phone-production.p12"
if [[ -e "$KEYSTORE" ]]; then
  echo 'An existing production key was found. No new key will be generated or overwrite it.'
  echo 'Reuse the existing key for every future production update.'
  exit 1
fi
read -r -s -p 'Create a strong keystore password (minimum 16 characters): ' STORE_PASSWORD
printf '\n'
read -r -s -p 'Repeat password: ' CONFIRM
printf '\n'
[[ "$STORE_PASSWORD" == "$CONFIRM" && ${#STORE_PASSWORD} -ge 16 ]] || { echo 'Passwords differ or are too short.'; exit 1; }
unset CONFIRM
export STORE_PASSWORD
export KEY_PASSWORD="$STORE_PASSWORD"
export KEY_ALIAS='amanphone'
trap 'unset STORE_PASSWORD KEY_PASSWORD KEY_ALIAS' EXIT
keytool -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -alias "$KEY_ALIAS" \
  -storepass:env STORE_PASSWORD -keypass:env KEY_PASSWORD -keyalg RSA -keysize 3072 \
  -validity 10000 -dname 'CN=Aman Phone,O=Aman Phone,C=YE'
keytool -list -v -keystore "$KEYSTORE" -storepass:env STORE_PASSWORD -alias "$KEY_ALIAS" > "$KEY_DIR/certificate-info.txt"
keytool -exportcert -keystore "$KEYSTORE" -storepass:env STORE_PASSWORD -alias "$KEY_ALIAS" -file "$KEY_DIR/certificate.der"
python3 - "$KEY_DIR" <<'PY'
import hashlib, pathlib, sys
p = pathlib.Path(sys.argv[1]); certificate = (p/'certificate.der').read_bytes()
sha = hashlib.sha256(certificate).hexdigest()
(p/'certificate-sha256.txt').write_text(sha+'\n')
print('Certificate SHA-1:', ':'.join(f'{b:02X}' for b in hashlib.sha1(certificate).digest()))
print('Certificate SHA-256:', sha)
PY
printf '\nKey generated locally. Back it up securely, including the password, before publishing.\n'
printf 'Private folder: %s\n' "$KEY_DIR"
printf 'Never upload this folder to your repository or send it in chat.\n'
read -r -p 'Upload signing secrets to your GitHub repository now using gh? Type UPLOAD: ' ANSWER
if [[ "$ANSWER" != 'UPLOAD' ]]; then
  echo 'No GitHub secrets changed. The private key remains on your computer.'
  exit 0
fi
command -v gh >/dev/null || { echo 'Install GitHub CLI and run gh auth login, then set the secrets using the guide.'; exit 1; }
gh auth status >/dev/null
# Secret values are passed through stdin, not CLI arguments or logged output.
python3 - "$KEYSTORE" <<'PY' | gh secret set RELEASE_KEYSTORE_BASE64 --repo "$REPO"
import base64, pathlib, sys
sys.stdout.write(base64.b64encode(pathlib.Path(sys.argv[1]).read_bytes()).decode())
PY
printf '%s' "$STORE_PASSWORD" | gh secret set STORE_PASSWORD --repo "$REPO"
printf '%s' "$KEY_PASSWORD" | gh secret set KEY_PASSWORD --repo "$REPO"
printf '%s' "$KEY_ALIAS" | gh secret set KEY_ALIAS --repo "$REPO"
cat "$KEY_DIR/certificate-sha256.txt" | tr -d '\n' | gh secret set RELEASE_CERT_SHA256 --repo "$REPO"
echo 'Signing secrets uploaded. This did not change workflows, Firebase, or publish an APK.'

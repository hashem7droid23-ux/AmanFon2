# Production signing and update publication

Status: update-check feature implemented; current workflow still publishes DEBUG. Nothing here changes signing credentials, configures GitHub Secrets, publishes a production APK or guarantees upgrade compatibility.

## One-time setup by the repository owner

1. Generate a private release keystore on a trusted machine and keep a secure backup. Do not reuse the publicly committed debug key for public production distribution. Never post the keystore or passwords in chat or commit them.
2. Configure GitHub Actions secrets: RELEASE_KEYSTORE_BASE64, STORE_PASSWORD, KEY_PASSWORD, KEY_ALIAS. Decode the secret to a temporary file and export KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD, KEY_ALIAS for Gradle. The existing release configuration reads those environment variables.
3. Register the release certificate SHA-1/SHA-256 in Firebase and the correct Android OAuth client for com.aistudio.lostphone.ymndx. Refresh google-services.json if required. Verify Google OAuth audience, enabled login providers and production App Check separately; do not disable enforcement to hide errors.
4. Have a workflow-authorized maintainer change .github/workflows/build-apk.yml to run tests, lint and assembleRelease. Fail if signing secrets are missing; never generate a random fallback key. Upload only the signed release APK as AmanPhone.apk.
5. Test login, photos and reports on real devices and inspect the APK with apksigner verify --verbose --print-certs. Confirm package name, versionCode and that the APK is not debuggable.

## Existing debug installations

A new production key normally cannot update existing debug-signed installations in place. Users may need a one-time uninstall/install transition, which removes local app data. Verify important reports are published before migration. Do not instruct users to uninstall without explaining the loss and confirming their data. Future production updates must use the same private signing key, package ID and increasing versionCode. Test an actual upgrade on a device; tests alone do not certify it.

## Publish update notices only after successful APK publication

The app reads update.json from main and verifies its APK SHA-256 digest, byte size and URL against the uploaded asset in GitHub release v1.0-apk. It also checks the installed signing certificate, package and build channel. Missing data, wrong key, wrong digest, drafts, incompatible channels and network failures do not prompt a download.

For each release:

- Increase versionCode/versionName in app/build.gradle.kts. Use user-visible release notes, not internal implementation details.
- Build, test, verify the APK certificate and upload the exact APK first. Serialize publication jobs so an older build cannot overwrite a newer APK.
- After the APK upload succeeds, update the matching debug or production object in update.json: enabled=true, actual versionCode/versionName, signerSha256 (certificate SHA-256, 64 hex characters without colons), apkSha256 (actual APK SHA-256), apkBytes (actual size), exact GitHub asset downloadUrl and Arabic releaseNotes. Never activate values from a build that has not been published.
- Use GitHub release asset metadata to verify digest and size. update.json must describe the actual uploaded APK, not just the current source version.
- Changing update.json on main currently triggers another build. For reliable automation, add workflow path filters that exclude metadata/docs-only updates or publish the metadata through a separate non-recursive process, preserving tests on code changes. A regenerated APK with a different digest will fail the updater gate until the manifest matches it.
- Replace the workflow's static release body with generated user-facing notes using body_path. This workflow change still requires appropriate GitHub authorization. Do not claim this link is configured until tested.

update.json intentionally starts disabled: no compatible newer published binary is being advertised yet. Users of 1.9 will see notices for future activated releases, not for 1.9 itself. The check runs when entering the foreground, at most once per 15 minutes per process; it is not a background push notification. Later postpones reminders for 24 hours. Download opens the direct HTTPS APK URL in a browser, and Android installation remains user-approved. The updater does not download or install silently and does not inspect the downloaded APK itself; certificate compatibility is checked from release metadata and Android checks signatures at installation.

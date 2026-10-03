# Aman Phone 1.8: code fixes and remaining deployment steps

Implemented: stable cloud IDs, Room v1 to v2 migration without destructive fallback, server-confirmed delete/recovery/publish, server-only IMEI checks with unavailable state, no automatic demo seeding, owned-report photo payloads, regression tests.

## Required before using photo uploads in 1.8
Deploy the repository's firestore.rules to project gen-lang-client-0428838696, database ai-studio-android-bbdab0c6-e15e-48f0-8ca7-0910319d9bda. Deploy to the named database, not the default database. Existing photo documents remain readable. New uploads include reportId and old rules reject them until this deployment is complete.

## Regression tests
assembleDebug finalizes with testDebugUnitTest. A failing test makes the Gradle invocation fail before CI reaches the release upload. For additional Android lint checks run bash ci/check.sh. Device/manual checks remain necessary: two-device create/recover/delete, permissions, offline failures, Google login, photos and app upgrade with existing data.

## Signing and production: not changed by this update
The current fixed debug key is retained for update compatibility. The release workflow still produces the debug APK. Never generate a random fallback key when the registered key is absent. A workflow administrator should back up the existing key in a private secret and verify its registered SHA-1 before publishing. Never send keystore passwords in chat or commit a new production private key.

A new release key cannot update the installed debug-signed app in place. Decide the distribution/signing migration first, register the production SHA fingerprints and OAuth Android client, store signing inputs in GitHub Secrets, then build and test assembleRelease. Do not ship a new signature accidentally.

## Firebase/Google console
Verify deployed rules, enabled auth providers, phone SMS billing/restrictions and production App Check. Verify OAuth Audience publishing status in the console; repository code cannot confirm that status.

## Known limits
Legacy local rows have no original owner/cloud ID. They are adopted only when matching remote fields; otherwise they are retained and cannot be remotely managed until identity is established. The app does not claim that an IMEI with no active report is safe. Writes are only called successful after acknowledgment, but a connection loss during a write may leave its final outcome uncertain; check the feed before retrying.

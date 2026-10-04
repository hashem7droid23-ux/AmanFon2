package com.example.util

import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    private val url = "https://github.com/hashem7droid23-ux/AmanFon2/releases/download/v1.0-apk/AmanPhone.apk"
    private val signer = "a".repeat(64)
    @Test fun acceptsOnlyExactRepositoryHttpsApkDownloads() {
        assertTrue(UpdatePolicy.trustedDownload(url))
        assertFalse(UpdatePolicy.trustedDownload(url.replace("https:", "http:")))
        assertFalse(UpdatePolicy.trustedDownload(url.replace("github.com", "github.com.evil.example")))
        assertFalse(UpdatePolicy.trustedDownload(url.replace("AmanFon2", "another-repo")))
        assertFalse(UpdatePolicy.trustedDownload(url + "?redirect=evil"))
        assertFalse(UpdatePolicy.trustedDownload(url.replace("AmanPhone.apk", "../AmanPhone.apk")))
        assertFalse(UpdatePolicy.trustedDownload(url.replace("AmanPhone.apk", "%41manPhone.apk")))
    }
    @Test fun rejectsOlderVersionsDifferentChannelsOrChangedSigningKeys() {
        assertTrue(UpdatePolicy.compatible(10, 11, "production", "production", signer, signer, "com.aistudio.lostphone.ymndx"))
        assertFalse(UpdatePolicy.compatible(10, 10, "production", "production", signer, signer, "com.aistudio.lostphone.ymndx"))
        assertFalse(UpdatePolicy.compatible(10, 9, "production", "production", signer, signer, "com.aistudio.lostphone.ymndx"))
        assertFalse(UpdatePolicy.compatible(10, 11, "debug", "production", signer, signer, "com.aistudio.lostphone.ymndx"))
        assertFalse(UpdatePolicy.compatible(10, 11, "production", "production", signer, "b".repeat(64), "com.aistudio.lostphone.ymndx"))
        assertFalse(UpdatePolicy.compatible(10, 11, "production", "production", "", "", "com.aistudio.lostphone.ymndx"))
        assertFalse(UpdatePolicy.compatible(10, 11, "production", "production", signer, signer, "other.app"))
    }
    @Test fun matchesPublishedAssetDigestAndSizeBeforeOfferingDownload() {
        assertTrue(UpdatePolicy.verifiedAsset(url, signer, 1234, url, "sha256:$signer", 1234))
        assertFalse(UpdatePolicy.verifiedAsset(url, signer, 1234, url, "sha256:${"b".repeat(64)}", 1234))
        assertFalse(UpdatePolicy.verifiedAsset(url, signer, 1234, url, "sha256:$signer", 5678))
        assertFalse(UpdatePolicy.verifiedAsset(url, signer, 1234, url, "", 1234))
    }
}

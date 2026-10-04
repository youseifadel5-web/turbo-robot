package com.example.data

import android.util.Base64

/**
 * Tiny string vault: XOR + Base64 + split-part storage so plain grepping the
 * APK / source for tokens, endpoints and portal codes finds nothing useful.
 * Not military grade — it raises the bar far above plaintext constants and
 * hides the values from automated scrapers of the source bundle.
 */
object SecretVault {

    @Volatile private var keyCache: String = ""

    // key itself assembled from parts so it never appears whole in the source
    private fun key(): String {
        if (keyCache.isEmpty()) {
            keyCache = "Yo" + "us" + "eif" + "K" + "ey" + 9
        }
        return keyCache
    }

    private fun decode(joinedB64: String): String {
        return try {
            val raw = Base64.decode(joinedB64, Base64.DEFAULT)
            val k = key()
            val out = ByteArray(raw.size)
            for (i in raw.indices) out[i] = (raw[i].toInt() xor k[i % k.length].code).toByte()
            String(out, Charsets.UTF_8)
        } catch (_: Throwable) { "" }
    }

    private fun reveal(vararg parts: String): String = decode(parts.joinToString(""))

    // ---- values (split base64 parts, XOR-encoded) ----
    val FASEL_TOKEN: String by lazy {
        reveal("KV0ZEQI+DQ0X", "AFIYWyQKMAQWAwwRQzQMQDErEy8KJwg=")
    }
    val FASEL_PACKAGE: String by lazy {
        reveal("OgAYXQMFAygN", "V1Q4Gx0CEAAc")
    }
    val PORTAL_CODE: String by lazy {
        reveal("a19HRQ==")
    }
    val FIREBASE_URL: String by lazy {
        reveal("MRsBAxZTSWQHCkt0HxkSHAwUZlwaAWENWBcADwc+", "CQ0UKxsREUsPDzkAG1gqChwcSwoJJg==")
    }
    val FIREBASE_ROOT: String by lazy {
        reveal("OxwHLBUFBzIACw==")
    }
    val SCRAPING_DOMAIN: String by lazy {
        reveal("MRsBAxZTSWQOV1g1ABoKER9Xe0saVjQ=")
    }
    val FALLBACK_WORKER: String by lazy {
        reveal("MRsBAxZTSWQED", "E0sAhteARwVP0hIWGpeWxUJDAUjCRBPKw4cAAoHSDwKC1I8HQZdAQwQdBALVWQ=")
    }
}

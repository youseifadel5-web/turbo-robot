package com.example.player

import android.util.Log
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Tiny local HTTP proxy used ONLY for Google Cast (Chromecast) casting.
 *
 * Why: the TV / receiver fetches the stream URL itself and therefore sends NO
 * Referer / User-Agent. Many IPTV streams reject that (HTTP 403), so they play on
 * the phone (which does send the headers via playUrl(...)) but fail on the TV.
 *
 * This proxy runs on the phone, fetches the upstream stream WITH the same headers
 * the phone player uses, and re-serves it to the TV on the LAN as
 *   http://<phone-lan-ip>:<port>/stream?u=<upstream>&r=<referer>&ua=<user-agent>
 *
 * HLS (.m3u8) playlists are rewritten so their segment / key / map URIs also go
 * through this proxy (otherwise the TV would fetch them directly and hit 403).
 *
 * No external dependencies — plain ServerSocket + HttpURLConnection.
 */
object CastProxy {
    private const val TAG = "CastProxy"
    private const val DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private val pool = Executors.newCachedThreadPool()

    @Volatile private var server: ServerSocket? = null
    @Volatile private var running = false
    @Volatile private var port = 0

    /** Best-effort LAN IPv4 (prefers Wi-Fi / Ethernet over VPN tunnels). */
    fun lanIp(): String? {
        return try {
            val nis = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
            val ordered = nis.sortedByDescending { n ->
                val nm = n.name.lowercase()
                if (nm.startsWith("wlan") || nm.startsWith("eth") || nm.startsWith("ap")) 1 else 0
            }
            val addrs = ordered.flatMap { it.inetAddresses.toList() }
                .filter { it is Inet4Address && !it.isLoopbackAddress }
            (addrs.firstOrNull { isPrivate(it.hostAddress) } ?: addrs.firstOrNull())?.hostAddress
        } catch (_: Throwable) {
            null
        }
    }

    private fun isPrivate(ip: String?): Boolean {
        if (ip == null) return false
        val second = ip.split(".").getOrNull(1)?.toIntOrNull() ?: -1
        return ip.startsWith("10.") || ip.startsWith("192.168.") ||
            (ip.startsWith("172.") && second in 16..31)
    }

    /** Returns a LAN proxy URL for [upstream], or null if it can't be proxied. */
    fun proxyUrl(upstream: String, referer: String?, userAgent: String?): String? {
        if (!upstream.startsWith("http", ignoreCase = true)) return null
        val ip = lanIp() ?: return null
        if (!start()) return null
        return buildUrl(ip, port, upstream, referer, userAgent)
    }

    private fun buildUrl(ip: String, port: Int, upstream: String, referer: String?, ua: String?): String {
        val enc = URLEncoder.encode(upstream, "UTF-8")
        val r = URLEncoder.encode(referer ?: "", "UTF-8")
        val u = URLEncoder.encode(ua ?: "", "UTF-8")
        return "http://$ip:$port/stream?u=$enc&r=$r&ua=$u"
    }

    @Synchronized
    private fun start(): Boolean {
        if (running) return true
        for (p in 8899..8910) {
            try {
                val ss = ServerSocket(p)
                server = ss
                port = p
                running = true
                pool.execute {
                    while (running) {
                        val client = try { ss.accept() } catch (_: Throwable) { break }
                        pool.execute { handle(client) }
                    }
                }
                Log.i(TAG, "proxy listening on $p")
                return true
            } catch (_: Throwable) {
                // port busy — try the next one
            }
        }
        Log.w(TAG, "could not bind a proxy port")
        return false
    }

    fun stop() {
        running = false
        try { server?.close() } catch (_: Throwable) {}
        server = null
        port = 0
    }

    private fun handle(client: Socket) {
        try {
            client.use { sock ->
                sock.soTimeout = 20000
                val input = sock.getInputStream()
                val requestLine = readLine(input) ?: return
                while (true) {
                    val h = readLine(input) ?: break
                    if (h.isEmpty()) break
                }
                val out = BufferedOutputStream(sock.getOutputStream())
                val target = requestLine.split(" ").getOrNull(1)
                if (target == null) { writeStatus(out, "400 Bad Request"); return }
                if (target.substringBefore('?') != "/stream") { writeStatus(out, "404 Not Found"); return }
                val params = parseQuery(target.substringAfter('?', ""))
                val upstream = params["u"]
                if (upstream.isNullOrBlank()) { writeStatus(out, "400 Bad Request"); return }
                relay(out, upstream, params["r"], params["ua"])
            }
        } catch (t: Throwable) {
            Log.w(TAG, "handle: ${t.message}")
        }
    }

    private fun relay(out: BufferedOutputStream, upstream: String, referer: String?, ua: String?) {
        val conn = (URL(upstream).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 25000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", ua?.takeIf { it.isNotBlank() } ?: DEFAULT_UA)
            referer?.takeIf { it.isNotBlank() }?.let { setRequestProperty("Referer", it) }
            setRequestProperty("Accept", "*/*")
            try {
                val uri = URI(upstream)
                setRequestProperty("Origin", "${uri.scheme}://${uri.host}")
            } catch (_: Throwable) {}
        }
        val code = try { conn.responseCode } catch (_: Throwable) { 502 }
        val ctype = try { conn.contentType.orEmpty() } catch (_: Throwable) { "" }
        val body: InputStream? = try {
            if (code in 200..299) conn.inputStream else conn.errorStream
        } catch (_: Throwable) { null }
        if (body == null) { writeStatus(out, "502 Bad Gateway"); return }

        val isHls = ctype.contains("mpegurl", true) || ctype.contains("m3u", true) ||
            upstream.substringBefore('?').endsWith(".m3u8", true)
        val ip = lanIp() ?: "127.0.0.1"

        if (isHls) {
            val text = try { body.readBytes().toString(Charsets.UTF_8) } catch (_: Throwable) { "" }
            val bytes = rewritePlaylist(text, upstream, referer, ua, ip).toByteArray(Charsets.UTF_8)
            out.write((
                "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/vnd.apple.mpegurl\r\n" +
                    "Content-Length: ${bytes.size}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
                ).toByteArray())
            out.write(bytes)
            out.flush()
        } else {
            out.write((
                "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: ${ctype.ifBlank { "application/octet-stream" }}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
                ).toByteArray())
            try { body.copyTo(out) } catch (_: Throwable) {}
            out.flush()
        }
        try { body.close() } catch (_: Throwable) {}
    }

    private fun rewritePlaylist(text: String, base: String, referer: String?, ua: String?, ip: String): String {
        val baseUri = try { URI(base) } catch (_: Throwable) { null }
        val sb = StringBuilder()
        for (raw in text.split("\n")) {
            val line = raw.trimEnd('\r')
            when {
                line.isBlank() -> sb.append('\n')
                line.startsWith("#") -> sb.append(rewriteAttrUris(line, baseUri, referer, ua, ip)).append('\n')
                else -> sb.append(buildUrl(ip, port, resolve(line, baseUri), referer, ua)).append('\n')
            }
        }
        return sb.toString()
    }

    private val uriAttr = Regex("URI=\"([^\"]*)\"")

    private fun rewriteAttrUris(line: String, baseUri: URI?, referer: String?, ua: String?, ip: String): String {
        if (!line.contains("URI=")) return line
        return uriAttr.replace(line) { m ->
            val value = m.groupValues.getOrNull(1).orEmpty()
            "URI=\"" + buildUrl(ip, port, resolve(value, baseUri), referer, ua) + "\""
        }
    }

    private fun resolve(ref: String, baseUri: URI?): String {
        return try {
            baseUri?.resolve(ref)?.toString() ?: ref
        } catch (_: Throwable) {
            ref
        }
    }

    private fun parseQuery(q: String): Map<String, String> {
        if (q.isBlank()) return emptyMap()
        return q.split("&").mapNotNull { part ->
            val i = part.indexOf('=')
            if (i < 0) null else {
                val k = URLDecoder.decode(part.substring(0, i), "UTF-8")
                val v = URLDecoder.decode(part.substring(i + 1), "UTF-8")
                k to v
            }
        }.toMap()
    }

    private fun readLine(input: InputStream): String? {
        val buf = ByteArrayOutputStream()
        var c = input.read()
        if (c == -1) return null
        while (c != -1 && c != '\n'.code) {
            if (c != '\r'.code) buf.write(c)
            c = input.read()
        }
        return buf.toString("UTF-8")
    }

    private fun writeStatus(out: OutputStream, status: String) {
        val body = status.toByteArray()
        out.write(("HTTP/1.1 $status\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray())
        out.write(body)
        out.flush()
    }
}

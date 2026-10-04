package com.example.data

/** A gateway/source entry the user can enable or disable independently. */
data class GatewayEntry(val id: String, val name: String, val enabled: Boolean = true)

/** Default known catalogs alongside Fasel HD (user can toggle each one later). */
object FaselGateways {
    fun defaults(): List<GatewayEntry> = listOf(
        GatewayEntry("googlefire", "Google Firebase (جوجل فاير)", enabled = true),
        GatewayEntry("faselhd", "فاصل HD", enabled = true),
        GatewayEntry("hikaye", "حكاية TV", enabled = true),
        GatewayEntry("custom", "محتواي الشخصي", enabled = true)
    )
}

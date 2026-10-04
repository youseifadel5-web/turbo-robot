package com.example.data

import android.content.Context
import android.content.SharedPreferences

/**
 * User-level switches stored in SharedPreferences.
 *  - hideImages: "لو مش عايز صور تنزل واوفر نت" — تطبق على كل المصادر.
 *  - sourceEnabled: per-source (network/folder) enable toggles — ليها حريه الاختيار.
 *  - gatewayMode: 'sites' = شاشة تشغيل/إطفاء المواقع.
 */
object UserSettings {
    private const val PREFS = "youseif_settings"
    private const val KEY_HIDE_IMAGES = "hide_images"
    private const val KEY_GATEWAY_MODE = "gateway_mode"
    private const val KEY_FAVORITE_SOURCES = "fav_sources"
    private const val KEY_CATALOG_REFRESH_AT = "catalog_refresh_at"
    /** Catalog cache lifetime — 24h to save mobile data */
    const val CATALOG_TTL_MS: Long = 24L * 60 * 60 * 1000

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    private fun p(): SharedPreferences =
        prefs ?: throw IllegalStateException("UserSettings.init() not called")

    var hideImages: Boolean
        get() = p().getBoolean(KEY_HIDE_IMAGES, false)
        set(v) = p().edit().putBoolean(KEY_HIDE_IMAGES, v).apply()

    var gatewayMode: String
        get() = p().getString(KEY_GATEWAY_MODE, "sites") ?: "sites"
        set(v) = p().edit().putString(KEY_GATEWAY_MODE, v).apply()

    fun isSourceEnabled(id: String, default: Boolean = true): Boolean {
        val key = "src_$id"
        if (!p().contains(key)) return default
        return p().getBoolean(key, default)
    }

    fun setSourceEnabled(id: String, enabled: Boolean) {
        p().edit().putBoolean("src_$id", enabled).apply()
    }

    fun setAllSources(enabled: Boolean, ids: List<String>) {
        val e = p().edit()
        for (id in ids) e.putBoolean("src_$id", enabled)
        e.apply()
    }

    fun favoriteSources(): MutableSet<String> =
        p().getStringSet(KEY_FAVORITE_SOURCES, emptySet())?.toMutableSet() ?: mutableSetOf()

    fun setFavoriteSource(id: String, fav: Boolean) {
        val set = favoriteSources().apply { if (fav) add(id) else remove(id) }
        p().edit().putStringSet(KEY_FAVORITE_SOURCES, set).apply()
    }

    /** مفضلة VOD: أفلام/مسلسلات/أنمي/أغاني — مفاتيح زي f:123 أو h:456 أو p:id */
    private const val KEY_VOD_FAVS = "vod_favorites_v1"
    fun vodFavorites(): MutableSet<String> =
        p().getStringSet(KEY_VOD_FAVS, emptySet())?.toMutableSet() ?: mutableSetOf()
    fun isVodFavorite(key: String): Boolean = key in vodFavorites()
    
    private const val KEY_LOCAL_AUDIO_FAVS = "local_audio_favs_v1"
    private const val KEY_LOCAL_VIDEO_FAVS = "local_video_favs_v1"
    fun localAudioFavorites(): MutableSet<String> =
        p().getStringSet(KEY_LOCAL_AUDIO_FAVS, emptySet())?.toMutableSet() ?: mutableSetOf()
    fun localVideoFavorites(): MutableSet<String> =
        p().getStringSet(KEY_LOCAL_VIDEO_FAVS, emptySet())?.toMutableSet() ?: mutableSetOf()
    fun toggleLocalAudioFavorite(id: String): Boolean {
        val set = localAudioFavorites()
        val on = if (id in set) { set.remove(id); false } else { set.add(id); true }
        p().edit().putStringSet(KEY_LOCAL_AUDIO_FAVS, set).apply()
        return on
    }
    fun toggleLocalVideoFavorite(id: String): Boolean {
        val set = localVideoFavorites()
        val on = if (id in set) { set.remove(id); false } else { set.add(id); true }
        p().edit().putStringSet(KEY_LOCAL_VIDEO_FAVS, set).apply()
        return on
    }
    fun isLocalAudioFavorite(id: String) = id in localAudioFavorites()
    fun isLocalVideoFavorite(id: String) = id in localVideoFavorites()

    fun toggleVodFavorite(key: String): Boolean {
        val set = vodFavorites()
        val now = if (key in set) { set.remove(key); false } else { set.add(key); true }
        p().edit().putStringSet(KEY_VOD_FAVS, set).apply()
        return now
    }
    private const val KEY_RADIO_FAVS = "radio_favorites_v1"
    fun radioFavorites(): MutableSet<String> =
        p().getStringSet(KEY_RADIO_FAVS, emptySet())?.toMutableSet() ?: mutableSetOf()
    fun isRadioFavorite(key: String): Boolean = key in radioFavorites()
    fun toggleRadioFavorite(key: String): Boolean {
        val set = radioFavorites()
        val now = if (key in set) { set.remove(key); false } else { set.add(key); true }
        p().edit().putStringSet(KEY_RADIO_FAVS, set).apply()
        return now
    }
    fun getString(key: String, default: String = ""): String = p().getString(key, default) ?: default

    fun putString(key: String, value: String) {
        p().edit().putString(key, value).apply()
    }

    fun getInt(key: String, default: Int = 0): Int = p().getInt(key, default)

    fun putInt(key: String, value: Int) {
        p().edit().putInt(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean = p().getBoolean(key, default)

    fun putBoolean(key: String, value: Boolean) {
        p().edit().putBoolean(key, value).apply()
    }

    fun getLong(key: String, default: Long = 0L): Long = p().getLong(key, default)

    fun putLong(key: String, value: Long) {
        p().edit().putLong(key, value).apply()
    }

    /**
     * قطع النت عن كتالوج الشبكة فقط (تحديث قوائم) — مش عن التشغيل.
     * جلسة فقط — ما يتسجلش عشان ما يفضلش قاطع بعد ريستارت.
     */
    @Volatile
    private var offlineSession: Boolean = false
    var offlineMode: Boolean
        get() = offlineSession
        set(v) { offlineSession = v }

    var catalogRefreshAt: Long
        get() = getLong(KEY_CATALOG_REFRESH_AT, 0L)
        set(v) = putLong(KEY_CATALOG_REFRESH_AT, v)

    fun isCatalogFresh(ttlMs: Long = CATALOG_TTL_MS): Boolean {
        val at = catalogRefreshAt
        return at > 0L && (System.currentTimeMillis() - at) < ttlMs
    }

    fun markCatalogRefreshed() {
        catalogRefreshAt = System.currentTimeMillis()
    }

}

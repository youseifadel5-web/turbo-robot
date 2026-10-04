package com.example.player

/**
 * Hardware-key bridge — يسمح لتقليب القنوات/المحتوى من أزرار الريموت والكي بورد
 * (CHANNEL UP/DOWN, MEDIA NEXT/PREVIOUS, PAGE UP/DOWN, وأزرار الاتجاهات داخل المشغل).
 *
 * MainActivity يقرأ الأزرار ويوجهها هنا، و MainScreen يسجل المنطق الفعلي
 * (تغيير القناة/الفيديو/الأغنية حسب القائمة المعروضة حالياً).
 */
object KeyRouter {

    /** dir: -1 = السابق، +1 = التالي. ترجع true لو تم استهلاك الزرار. */
    @Volatile
    var handler: ((Int) -> Boolean)? = null

    /**
     * أزرار الاتجاهات (D-Pad) لا تُستهلك إلا وأنت داخل المشغل،
     * عشان ما نكسرش التنقل العادي في الواجهة.
     */
    @Volatile
    var dpadEnabled: Boolean = false

    fun dispatch(dir: Int): Boolean = try {
        handler?.invoke(dir) ?: false
    } catch (_: Throwable) {
        false
    }
}

package com.example.player

/**
 * جسر بين إشعار/جلسة الوسائط (PlaybackService) والواجهة (YouseifPlayerController).
 *
 * ليه محتاجينه؟
 * - المشغّل دايمًا بيحمّل عنصر واحد (Single MediaItem)، فـ seekToNextMediaItem()
 *   على تايم لاين فيه عنصر واحد = No-op، يعني أزرار الإشعار كانت ميتة.
 * - الجسر بيخلّي أزرار "السابق/التالي" تُنفّذ تقليب حقيقي على نفس السياق المعروض،
 *   وزر التشغيل يرجع آخر بث كان شغّال (حتى بعد قفل التطبيق).
 *
 * كل دالة ترجع true لو اتعمل الإجراء فعليًا.
 */
object PlaybackBridge {

    /** تقليب للتالي (قناة/فيلم/جودة/أغنية). */
    @Volatile
    var onNext: (() -> Boolean)? = null

    /** تقليب للسابق. */
    @Volatile
    var onPrevious: (() -> Boolean)? = null

    /** تشغيل/إيقاف، ولو مفيش حاجة محمّلة يرجّع آخر بث. */
    @Volatile
    var onToggleOrResume: (() -> Boolean)? = null

    fun next(): Boolean = try { onNext?.invoke() ?: false } catch (_: Throwable) { false }

    fun previous(): Boolean = try { onPrevious?.invoke() ?: false } catch (_: Throwable) { false }

    fun toggleOrResume(): Boolean = try { onToggleOrResume?.invoke() ?: false } catch (_: Throwable) { false }
}

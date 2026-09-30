extends Node
## L10n — EN/AR string table. Godot's TextServer joins Arabic letters and
## handles RTL natively, so this port FIXES the Unity known-issue where
## uGUI Text couldn't render joined Arabic.

const TABLE := {
	"menu.play": ["PLAY", "ابدأ السباق"],
	"menu.garage": ["GARAGE", "الجراج"],
	"menu.events": ["EVENTS", "الأحداث"],
	"menu.settings": ["SETTINGS", "الإعدادات"],
	"menu.music": ["DEVICE MUSIC", "موسيقى الجهاز"],
	"menu.coins": ["COINS", "العملات"],
	"menu.level": ["LV", "المستوى"],
	"menu.ghost": ["RACE GHOST", "تحدَّ الشبح"],
	"garage.race": ["RACE", "سباق"],
	"garage.customize": ["CUSTOMIZE", "تخصيص"],
	"garage.back": ["BACK", "رجوع"],
	"garage.owned": ["OWNED", "مملوكة"],
	"garage.buy": ["BUY", "شراء"],
	"garage.select": ["SELECT", "اختيار"],
	"garage.selected": ["SELECTED", "مختارة"],
	"garage.paint": ["PAINT", "الطلاء"],
	"garage.rims": ["RIMS", "الجنوط"],
	"garage.neon": ["NEON", "النيون"],
	"garage.stats": ["STATS", "الإحصائيات"],
	"garage.upgrade": ["UPGRADE", "ترقية"],
	"settings.title": ["SETTINGS", "الإعدادات"],
	"settings.graphics": ["GRAPHICS", "الرسوميات"],
	"settings.fps": ["FRAME RATE", "معدل الإطارات"],
	"settings.vibration": ["VIBRATION", "الاهتزاز"],
	"settings.controls": ["CONTROLS", "التحكم"],
	"settings.sensitivity": ["STEERING SENSITIVITY", "حساسية التوجيه"],
	"settings.language": ["LANGUAGE", "اللغة"],
	"settings.master": ["MASTER VOLUME", "الصوت العام"],
	"settings.music": ["MUSIC VOLUME", "صوت الموسيقى"],
	"settings.sfx": ["SFX VOLUME", "المؤثرات"],
	"settings.engine": ["ENGINE VOLUME", "صوت المحرك"],
	"settings.close": ["CLOSE", "إغلاق"],
	"results.title": ["RACE COMPLETE", "انتهى السباق"],
	"results.retry": ["RETRY", "إعادة"],
	"results.menu": ["MENU", "القائمة"],
	"results.next": ["NEXT EVENT", "الحدث التالي"],
	"results.position": ["POSITION", "المركز"],
	"results.time": ["TIME", "الزمن"],
	"results.bestlap": ["BEST LAP", "أفضل لفة"],
	"results.drift": ["DRIFT SCORE", "نقاط الدريفت"],
	"results.topspeed": ["TOP SPEED", "أقصى سرعة"],
	"results.reward": ["REWARD", "المكافأة"],
	"events.chapter": ["CHAPTER", "الفصل"],
	"hud.lap": ["LAP", "لفة"],
	"hud.nitro": ["NITRO", "نيترو"],
	"hud.speed": ["KM/H", "كم/س"],
	"hud.wrongway": ["WRONG WAY!", "اتجاه معاكس!"],
	"hud.countdown": ["GET READY", "استعد"],
	"loading.tip": ["TIP", "نصيحة"],
}


func lang_index() -> int:
	return clampi(Save.language, 0, 1)


func tr_neon(key: String) -> String:
	# "tr" is reserved on Node; use tr_neon everywhere.
	if TABLE.has(key):
		return String(TABLE[key][lang_index()])
	return key


func is_rtl() -> bool:
	return lang_index() == 1


func cycle() -> void:
	Save.language = 1 - Save.language
	Save.save()

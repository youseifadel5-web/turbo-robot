# GitHub Android Release Build

هذا المستودع يحتوي على مشروع Unity 6 كامل وWorkflow يبني APK Release عند كل push إلى `main` أو عند التشغيل اليدوي من تبويب **Actions**.

## الأسرار المطلوبة

أضف الأسرار التالية في GitHub من:

`Settings → Secrets and variables → Actions → New repository secret`

| الاسم | الاستخدام |
|---|---|
| `BOT_TOKEN` | توكن بوت Telegram، لا تضعه داخل الملفات أو الكود |
| `ADMIN_ID` | رقم/معرّف محادثة Telegram التي سيصل إليها APK |
| `UNITY_EMAIL` | حساب Unity المستخدم للبناء |
| `UNITY_PASSWORD` | كلمة مرور Unity |
| `UNITY_SERIAL` | رقم ترخيص Unity إن كان مطلوبًا |

إذا كان `BOT_TOKEN` و`ADMIN_ID` موجودين بالفعل، سيستخدمهما Workflow مباشرة من دون كشف القيم في السجل. لا يتم طباعة أي Secret.

## ما يحدث بعد البناء

1. يجهّز المشروع تلقائيًا عبر `NeonRushSetupWizard.Setup()`.
2. يبني المشاهد الثلاثة بالترتيب: MainMenu ثم Garage ثم Race_NeonCity.
3. يخرج `Build/NeonRushRacing-release.apk` مع ضغط LZ4HC وبدون Development Build.
4. يرفع APK وملف SHA256 كـArtifact.
5. يرسل APK إلى `ADMIN_ID` باستخدام Bot API عبر HTTPS.

## التشغيل

- افتح تبويب **Actions**.
- اختر **Build NeonRushRacing Android Release**.
- اضغط **Run workflow**.
- بعد النجاح ستجد APK في Artifacts، وسيصل أيضًا إلى Telegram.

> ملاحظة: النسخة الناتجة Release غير موقعة بمفتاح نشر خاص ما لم تضف إعداد Keystore إلى إعدادات CI. للتجربة والتثبيت المباشر، Unity ينتج APK قابلًا للبناء والتثبيت؛ للنشر على Google Play أضف Keystore خاصًا وسرّيه في GitHub.

using System;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;

namespace LyricEcho.Windows;

internal static class InterfaceText
{
    // Persian source | English | Arabic | Spanish | Turkish
    private static readonly string[] Rows = {
        "♫  در حال پخش|♫  Now playing|♫  قيد التشغيل|♫  En reproducción|♫  Şimdi çalıyor",
        "آهنگ فعلی|Current track|المقطع الحالي|Canción actual|Şu anki şarkı",
        "پخش‌کننده را متصل کن یا دستی جست‌وجو کن|Connect a player or search manually|اتصل بمشغل أو ابحث يدوياً|Conecta un reproductor o busca manualmente|Bir oynatıcı bağlayın veya elle arayın",
        "نام آهنگ|Song title|اسم الأغنية|Título de la canción|Şarkı adı",
        "خواننده|Artist|الفنان|Artista|Sanatçı",
        "جست‌وجوی متن  ←|Find lyrics  →|البحث عن الكلمات|Buscar letra|Şarkı sözlerini ara",
        "شناسایی مجدد|Detect again|إعادة الاكتشاف|Detectar de nuevo|Yeniden algıla",
        "منبع پخش|Playback source|مصدر التشغيل|Fuente de reproducción|Oynatma kaynağı",
        "انتخاب خودکار پخش‌کننده|Choose player automatically|اختيار المشغل تلقائياً|Elegir reproductor automáticamente|Oynatıcıyı otomatik seç",
        "ابزارها|Tools|الأدوات|Herramientas|Araçlar",
        "پنجره شناور  ↗|Floating window  ↗|نافذة عائمة  ↗|Ventana flotante  ↗|Yüzen pencere  ↗",
        "فایل LRC / متن|LRC / text file|ملف LRC / نص|Archivo LRC / texto|LRC / metin dosyası",
        "ورود دستی|Manual entry|إدخال يدوي|Entrada manual|Elle giriş",
        "متن و ترجمه|Lyrics and translation|الكلمات والترجمة|Letra y traducción|Şarkı sözleri ve çeviri",
        "با موسیقی همراه شو|Follow along with the music|تابع مع الموسيقى|Sigue la música|Müziği takip edin",
        "علاقه‌مندی|Favorite|المفضلة|Favorito|Favori",
        "دریافت دوباره متن|Refresh lyrics|تحديث الكلمات|Actualizar letra|Sözleri yenile",
        "✦  ترجمه|✦  Translate|✦  ترجمة|✦  Traducir|✦  Çevir",
        "رفتن به موقعیت آهنگ|Seek in track|الانتقال إلى موضع في المقطع|Ir a posición de canción|Şarkıda konuma git",
        "تنظیم هماهنگی|Sync offset|ضبط التزامن|Ajustar sincronización|Eşitleme ayarı",
        "نیم ثانیه عقب|Half a second back|نصف ثانية للخلف|Medio segundo atrás|Yarım saniye geri",
        "نیم ثانیه جلو|Half a second forward|نصف ثانية للأمام|Medio segundo adelante|Yarım saniye ileri",
        "آهنگ قبلی|Previous track|المقطع السابق|Canción anterior|Önceki şarkı",
        "پنج ثانیه عقب|Back five seconds|خمس ثوان للخلف|Cinco segundos atrás|Beş saniye geri",
        "پخش / مکث|Play / pause|تشغيل / إيقاف مؤقت|Reproducir / pausa|Oynat / duraklat",
        "پنج ثانیه جلو|Forward five seconds|خمس ثوان للأمام|Cinco segundos adelante|Beş saniye ileri",
        "آهنگ بعدی|Next track|المقطع التالي|Canción siguiente|Sonraki şarkı",
        "کتابخانه|Library|المكتبة|Biblioteca|Kitaplık",
        "کتابخانه من|My library|مكتبتي|Mi biblioteca|Kitaplığım",
        "آهنگ‌های ذخیره‌شده و ترجمه‌ها|Saved songs and translations|الأغاني والترجمات المحفوظة|Canciones y traducciones guardadas|Kaydedilen şarkılar ve çeviriler",
        "فقط علاقه‌مندی‌ها|Favorites only|المفضلة فقط|Solo favoritos|Yalnızca favoriler",
        "جست‌وجوی نام آهنگ یا خواننده|Search song or artist|البحث عن أغنية أو فنان|Buscar canción o artista|Şarkı veya sanatçı ara",
        "به‌روزرسانی|Refresh|تحديث|Actualizar|Yenile",
        "☆ علاقه‌مندی|☆ Favorite|☆ المفضلة|☆ Favorito|☆ Favori",
        "حذف آهنگ|Delete song|حذف الأغنية|Eliminar canción|Şarkıyı sil",
        "یک آهنگ انتخاب کنید|Select a song|اختر أغنية|Selecciona una canción|Bir şarkı seçin",
        "تنظیمات|Settings|الإعدادات|Ajustes|Ayarlar",
        "ظاهر و تجربه|Appearance and behavior|المظهر والسلوك|Apariencia y comportamiento|Görünüm ve davranış",
        "زبان برنامه|App language|لغة التطبيق|Idioma de la aplicación|Uygulama dili",
        "تم|Theme|السمة|Tema|Tema",
        "حرکت خودکار متن|Auto-scroll lyrics|تمرير الكلمات تلقائياً|Desplazar letra automáticamente|Sözleri otomatik kaydır",
        "نمایش خودکار پنجره شناور|Open floating window automatically|فتح النافذة العائمة تلقائياً|Abrir ventana flotante automáticamente|Yüzen pencereyi otomatik aç",
        "ترجمه خودکار (مصرف API)|Automatic translation (uses API)|ترجمة تلقائية (تستخدم API)|Traducción automática (usa API)|Otomatik çeviri (API kullanır)",
        "تشخیص خودکار پخش|Detect playback automatically|اكتشاف التشغيل تلقائياً|Detectar reproducción automáticamente|Oynatmayı otomatik algıla",
        "اندازه متن|Text size|حجم النص|Tamaño del texto|Metin boyutu",
        "شفافیت پنجره شناور|Floating window opacity|شفافية النافذة العائمة|Opacidad de la ventana flotante|Yüzen pencere saydamlığı",
        "تنظیمات ترجمه|Translation settings|إعدادات الترجمة|Ajustes de traducción|Çeviri ayarları",
        "سرویس‌های آماده|Providers|مزودو الخدمة|Proveedores|Sağlayıcılar",
        "سفارشی|Custom|مخصص|Personalizado|Özel",
        "پروفایل‌های API|API profiles|ملفات API|Perfiles API|API profilleri",
        "نام پروفایل|Profile name|اسم الملف|Nombre del perfil|Profil adı",
        "ذخیره پروفایل|Save profile|حفظ الملف|Guardar perfil|Profili kaydet",
        "فعال‌سازی|Activate|تفعيل|Activar|Etkinleştir",
        "حذف|Delete|حذف|Eliminar|Sil",
        "زبان مقصد|Translation language|لغة الترجمة|Idioma de traducción|Çeviri dili",
        "آدرس Gemini یا API سازگار با OpenAI|Gemini or OpenAI-compatible API URL|عنوان Gemini أو API متوافق مع OpenAI|URL de Gemini o API compatible con OpenAI|Gemini veya OpenAI uyumlu API adresi",
        "مدل|Model|النموذج|Modelo|Model",
        "نام مدل سفارشی|Custom model name|اسم النموذج المخصص|Nombre de modelo personalizado|Özel model adı",
        "افزودن مدل سفارشی|Add custom model|إضافة نموذج مخصص|Añadir modelo personalizado|Özel model ekle",
        "کلید API|API key|مفتاح API|Clave API|API anahtarı",
        "ذخیره تنظیمات و کلید|Save settings and key|حفظ الإعدادات والمفتاح|Guardar ajustes y clave|Ayarları ve anahtarı kaydet",
        "بازگردانی Gemini|Reset Gemini|إعادة تعيين Gemini|Restablecer Gemini|Gemini'yi sıfırla",
        "بررسی اتصال و مدل‌ها|Check connection and models|فحص الاتصال والنماذج|Comprobar conexión y modelos|Bağlantıyı ve modelleri kontrol et",
        "کلید برای حساب ویندوز شما رمزگذاری می‌شود. برای حذف کلید ذخیره‌شده، آن را پاک و تنظیمات را ذخیره کنید.|The key is encrypted for your Windows account. Clear it and save to remove it.|يُشفّر المفتاح لحساب Windows. امسحه واحفظ الإعدادات لحذفه.|La clave se cifra para tu cuenta de Windows. Bórrala y guarda para eliminarla.|Anahtar Windows hesabınız için şifrelenir. Silmek için temizleyip kaydedin.",
        "عیب‌یابی|Diagnostics|التشخيص|Diagnóstico|Tanılama",
        "رویدادهای برنامه|Application events|أحداث التطبيق|Eventos de la aplicación|Uygulama olayları",
        "پاک کردن گزارش|Clear log|مسح السجل|Borrar registro|Günlüğü temizle",
        "برای شروع، آهنگی پخش کنید یا نام آن را دستی وارد کنید.|Play a song or enter its name to get started.|شغّل أغنية أو أدخل اسمها للبدء.|Reproduce una canción o introduce su nombre para empezar.|Başlamak için bir şarkı çalın veya adını girin.",
        "در انتظار متن آهنگ|Waiting for lyrics|بانتظار كلمات الأغنية|Esperando la letra|Şarkı sözleri bekleniyor",
        "هماهنگی|Sync|التزامن|Sincronizar|Eşitle",
        "حالت جمع‌وجور|Compact mode|وضع مختصر|Modo compacto|Kompakt görünüm",
        "نمایش کنترل‌ها|Show controls|إظهار عناصر التحكم|Mostrar controles|Kontrolleri göster",
        "بستن پنجره|Close window|إغلاق النافذة|Cerrar ventana|Pencereyi kapat",
        "برای جابه‌جایی پنجره بکشید|Drag to move window|اسحب لتحريك النافذة|Arrastra para mover|Taşımak için sürükleyin",
        "نیم ثانیه تأخیر|Delay half a second|تأخير نصف ثانية|Retrasar medio segundo|Yarım saniye geciktir",
        "نیم ثانیه جلوتر|Advance half a second|تقديم نصف ثانية|Adelantar medio segundo|Yarım saniye ilerlet",
        "پخش|Play|تشغيل|Reproducir|Oynat",
        "مکث|Pause|إيقاف مؤقت|Pausa|Duraklat",
        "باز کردن LyricEcho|Open LyricEcho|فتح LyricEcho|Abrir LyricEcho|LyricEcho'yu aç",
        "خروج کامل|Quit|خروج|Salir|Çıkış",
        "نام آهنگ را وارد کنید.|Enter a song title.|أدخل اسم الأغنية.|Introduce el título.|Şarkı adını girin.",
        "نام مدل را وارد کنید.|Enter a model name.|أدخل اسم النموذج.|Introduce el modelo.|Model adını girin.",
        "ابتدا متن آهنگ را دریافت کنید.|Load lyrics first.|حمّل كلمات الأغنية أولاً.|Carga primero la letra.|Önce sözleri yükleyin.",
        "ابتدا متن همین آهنگ و زبان را بارگذاری کنید.|Load lyrics for this song and language first.|حمّل كلمات هذه الأغنية وبهذه اللغة أولاً.|Carga la letra de esta canción e idioma primero.|Önce bu şarkının ve dilin sözlerini yükleyin.",
        "پخش‌کننده‌ای متصل نیست.|No player is connected.|لا يوجد مشغل متصل.|No hay reproductor conectado.|Bağlı oynatıcı yok.",
        "این فرمان در پخش‌کننده فعلی پشتیبانی نشد.|The current player does not support this command.|المشغل الحالي لا يدعم هذا الأمر.|El reproductor actual no admite este comando.|Geçerli oynatıcı bu komutu desteklemiyor.",
        "پخش‌کننده فعالی پیدا نشد؛ می‌توانید دستی جست‌وجو کنید.|No active player found. You can search manually.|لم يُعثر على مشغل نشط. يمكنك البحث يدوياً.|No hay reproductor activo. Puedes buscar manualmente.|Etkin oynatıcı bulunamadı. Elle arayabilirsiniz.",
        "تشخیص خودکار غیرفعال است.|Automatic detection is off.|الاكتشاف التلقائي متوقف.|La detección automática está desactivada.|Otomatik algılama kapalı.",
        "کلید ذخیره‌شده حذف شد.|Saved key removed.|تم حذف المفتاح المحفوظ.|Clave guardada eliminada.|Kayıtlı anahtar silindi.",
        "تنظیمات و کلید ذخیره شدند.|Settings and key saved.|تم حفظ الإعدادات والمفتاح.|Ajustes y clave guardados.|Ayarlar ve anahtar kaydedildi.",
        "فایل باز شد.|File opened.|تم فتح الملف.|Archivo abierto.|Dosya açıldı.",
        "پروفایل ذخیره شد.|Profile saved.|تم حفظ الملف.|Perfil guardado.|Profil kaydedildi.",
        "پروفایل حذف شد.|Profile deleted.|تم حذف الملف.|Perfil eliminado.|Profil silindi.",
        "پروفایل فعال شد؛ برای پیش‌فرض شدن تنظیمات را ذخیره کنید.|Profile activated. Save settings to make it the default.|تم تفعيل الملف. احفظ الإعدادات لجعله افتراضياً.|Perfil activado. Guarda los ajustes para usarlo por defecto.|Profil etkin. Varsayılan yapmak için ayarları kaydedin.",
        "انتخاب خودکار پخش‌کننده فعال شد.|Automatic player selection enabled.|تم تفعيل الاختيار التلقائي للمشغل.|Selección automática del reproductor activada.|Otomatik oynatıcı seçimi etkin.",
        "ابتدا آهنگ را در کتابخانه ذخیره کنید.|Save the song to the library first.|احفظ الأغنية في المكتبة أولاً.|Guarda primero la canción.|Önce şarkıyı kitaplığa kaydedin.",
        "یک آهنگ از کتابخانه انتخاب کنید.|Select a song from the library.|اختر أغنية من المكتبة.|Selecciona una canción de la biblioteca.|Kitaplıktan bir şarkı seçin.",
        "از علاقه‌مندی‌ها حذف شد.|Removed from favorites.|تمت الإزالة من المفضلة.|Eliminado de favoritos.|Favorilerden çıkarıldı.",
        "به علاقه‌مندی‌ها اضافه شد.|Added to favorites.|أضيف إلى المفضلة.|Añadido a favoritos.|Favorilere eklendi.",
        "آهنگ حذف شد.|Song deleted.|تم حذف الأغنية.|Canción eliminada.|Şarkı silindi.",
        "نمایش و ذخیره|Show and save|عرض وحفظ|Mostrar y guardar|Göster ve kaydet",
        "متن اصلی یا LRC|Original lyrics or LRC|الكلمات الأصلية أو LRC|Letra original o LRC|Orijinal sözler veya LRC",
        "ترجمه (اختیاری، هر خط جدا)|Translation (optional, one line each)|الترجمة (اختيارية، سطر لكل سطر)|Traducción (opcional, una línea por línea)|Çeviri (isteğe bağlı, satır başına)",
        "ورود متن یا LRC|Enter lyrics or LRC|إدخال الكلمات أو LRC|Introducir letra o LRC|Sözleri veya LRC girin",
        "متنی وارد نشده است.|No lyrics entered.|لم يتم إدخال كلمات.|No se ha introducido letra.|Söz girilmedi.",
        "تعداد خطوط ترجمه باید با متن اصلی یکسان باشد.|Translation and original must have the same number of lines.|يجب أن يتساوى عدد أسطر الترجمة والأصل.|La traducción y el original deben tener el mismo número de líneas.|Çeviri ve orijinal aynı sayıda satır içermeli.",
        "در حال بررسی اتصال...|Checking connection...|جارٍ فحص الاتصال...|Comprobando conexión...|Bağlantı kontrol ediliyor...",
        "ذخیره تنظیمات ناموفق بود: |Could not save settings: |تعذر حفظ الإعدادات: |No se pudieron guardar los ajustes: |Ayarlar kaydedilemedi: ",
        "ذخیره زبان ناموفق بود: |Could not save language: |تعذر حفظ اللغة: |No se pudo guardar el idioma: |Dil kaydedilemedi: ",
        "اتصال به رسانه ویندوز ممکن نشد: |Could not connect to Windows media: |تعذر الاتصال بوسائط Windows: |No se pudo conectar a medios de Windows: |Windows medyasına bağlanılamadı: ",
        "خطا در خواندن پخش‌کننده: |Could not read player: |تعذرت قراءة المشغل: |No se pudo leer el reproductor: |Oynatıcı okunamadı: ",
        "دریافت متن ناموفق بود: |Could not load lyrics: |تعذر تحميل الكلمات: |No se pudo cargar la letra: |Sözler yüklenemedi: ",
        "ترجمه ناموفق بود: |Translation failed: |فشلت الترجمة: |Error de traducción: |Çeviri başarısız: ",
        "ذخیره کلید ناموفق بود: |Could not save key: |تعذر حفظ المفتاح: |No se pudo guardar la clave: |Anahtar kaydedilemedi: ",
        "فرمان پخش انجام نشد: |Playback command failed: |فشل أمر التشغيل: |Falló el comando de reproducción: |Oynatma komutu başarısız: ",
        "ذخیره پروفایل ناموفق بود: |Could not save profile: |تعذر حفظ الملف: |No se pudo guardar el perfil: |Profil kaydedilemedi: ",
        "فعال‌سازی پروفایل ناموفق بود: |Could not activate profile: |تعذر تفعيل الملف: |No se pudo activar el perfil: |Profil etkinleştirilemedi: ",
        "حذف پروفایل ناموفق بود: |Could not delete profile: |تعذر حذف الملف: |No se pudo eliminar el perfil: |Profil silinemedi: ",
        "ذخیره علاقه‌مندی ناموفق بود: |Could not save favorite: |تعذر حفظ المفضلة: |No se pudo guardar el favorito: |Favori kaydedilemedi: ",
        "حذف ناموفق بود: |Could not delete: |تعذر الحذف: |No se pudo eliminar: |Silinemedi: ",
        "ذخیره متن ناموفق بود: |Could not save lyrics: |تعذر حفظ الكلمات: |No se pudo guardar la letra: |Sözler kaydedilemedi: ",
        "بررسی اتصال ناموفق بود: |Connection check failed: |فشل فحص الاتصال: |Falló la comprobación: |Bağlantı kontrolü başarısız: ",
        "خواندن کلید ذخیره‌شده ممکن نشد؛ کلید را دوباره وارد کنید.|Could not read saved key. Enter it again.|تعذرت قراءة المفتاح المحفوظ. أدخله مجدداً.|No se pudo leer la clave guardada. Introdúcela de nuevo.|Kayıtlı anahtar okunamadı. Yeniden girin.",
        "نشانی API باید HTTPS یا آدرس محلی باشد.|API address must use HTTPS or be local.|يجب أن يكون عنوان API آمناً أو محلياً.|La dirección API debe usar HTTPS o ser local.|API adresi HTTPS veya yerel olmalı.",
        "پاسخ مدل آرایه ترجمه ندارد.|The model response has no translation array.|لا تحتوي استجابة النموذج على مصفوفة ترجمة.|La respuesta del modelo no contiene una lista de traducciones.|Model yanıtında çeviri dizisi yok.",
        "تعداد خطوط ترجمه با متن اصلی برابر نیست.|The translation line count does not match the original.|عدد أسطر الترجمة لا يطابق الأصل.|El número de líneas no coincide.|Çeviri satır sayısı orijinalle eşleşmiyor.",
        "خواندن تنظیمات ممکن نشد؛ مقادیر پیش‌فرض استفاده می‌شوند.|Could not read settings; defaults will be used.|تعذرت قراءة الإعدادات؛ ستُستخدم القيم الافتراضية.|No se pudieron leer los ajustes; se usarán valores predeterminados.|Ayarlar okunamadı; varsayılanlar kullanılacak.",
        "نام پروفایل را وارد کنید.|Enter a profile name.|أدخل اسم الملف.|Introduce el nombre del perfil.|Profil adı girin.",
        "یک پروفایل انتخاب کنید.|Select a profile.|اختر ملفاً.|Selecciona un perfil.|Bir profil seçin.",
        "نشانی معتبر HTTPS یا آدرس محلی وارد کنید.|Enter a valid HTTPS or local address.|أدخل عنوان HTTPS صالحاً أو عنواناً محلياً.|Introduce una dirección HTTPS válida o local.|Geçerli bir HTTPS veya yerel adres girin.",
        "باز کردن فایل ناموفق بود: |Could not open file: |تعذر فتح الملف: |No se pudo abrir el archivo: |Dosya açılamadı: ",
        "حذف پروفایل|Delete profile|حذف الملف|Eliminar perfil|Profili sil",
        "حذف آهنگ|Delete song|حذف الأغنية|Eliminar canción|Şarkıyı sil",
        "پروفایل «{0}» حذف شود؟|Delete profile “{0}”?|هل تريد حذف الملف «{0}»؟|¿Eliminar el perfil «{0}»?|“{0}” profili silinsin mi?",
        "آهنگ «{0}» از کتابخانه حذف شود؟|Delete “{0}” from the library?|هل تريد حذف «{0}» من المكتبة؟|¿Eliminar «{0}» de la biblioteca?|“{0}” kitaplıktan silinsin mi?",
        "{0} خط بارگذاری شد.|{0} lines loaded.|تم تحميل {0} سطر.|{0} líneas cargadas.|{0} satır yüklendi.",
        "خطای سرویس ({0}). آدرس، مدل و کلید را بررسی کنید.|Service error ({0}). Check the address, model, and key.|خطأ الخدمة ({0}). تحقق من العنوان والنموذج والمفتاح.|Error del servicio ({0}). Comprueba la dirección, el modelo y la clave.|Servis hatası ({0}). Adresi, modeli ve anahtarı kontrol edin.",
        "خطای اتصال: HTTP {0}. کلید و نشانی را بررسی کنید.|Connection error: HTTP {0}. Check the key and address.|خطأ اتصال: HTTP {0}. تحقق من المفتاح والعنوان.|Error de conexión: HTTP {0}. Comprueba la clave y la dirección.|Bağlantı hatası: HTTP {0}. Anahtarı ve adresi kontrol edin.",
        "اتصال موفق؛ {0} میلی‌ثانیه، {1} مدل.|Connected in {0} ms; {1} models.|تم الاتصال خلال {0} مللي ثانية؛ {1} نموذج.|Conectado en {0} ms; {1} modelos.|{0} ms içinde bağlandı; {1} model.",
        "آماده|Ready|جاهز|Listo|Hazır",
        "در حال جست‌وجو...|Searching...|جارٍ البحث...|Buscando...|Aranıyor...",
        "در حال ترجمه...|Translating...|جارٍ الترجمة...|Traduciendo...|Çevriliyor...",
        "بارگذاری از کتابخانه محلی|Loaded from local library|تم التحميل من المكتبة المحلية|Cargado de la biblioteca local|Yerel kitaplıktan yüklendi",
        "ترجمه ذخیره شد.|Translation saved.|تم حفظ الترجمة.|Traducción guardada.|Çeviri kaydedildi.",
        "متنی پیدا نشد.|No lyrics found.|لم يتم العثور على كلمات.|No se encontró la letra.|Şarkı sözü bulunamadı.",
        "متن زمان‌بندی‌شده دریافت شد.|Synced lyrics loaded.|تم تحميل الكلمات المتزامنة.|Letra sincronizada cargada.|Eşzamanlı sözler yüklendi.",
        "متن دریافت شد؛ زمان‌بندی در دسترس نیست.|Lyrics loaded; timing unavailable.|تم تحميل الكلمات دون توقيت.|Letra cargada sin sincronización.|Sözler yüklendi; zamanlama yok."
    };

    private static readonly Dictionary<string, string[]> Translations = Build();
    private static Dictionary<string, string[]> Build()
    {
        var result = new Dictionary<string, string[]>(StringComparer.Ordinal);
        foreach (var row in Rows)
        {
            var values = row.Split('|');
            if (values.Length == 5) result[values[0]] = values;
        }
        return result;
    }

    public static List<(DependencyObject Element, DependencyProperty Property, string Original)> Capture(DependencyObject root)
    {
        var entries = new List<(DependencyObject, DependencyProperty, string)>();
        var seen = new HashSet<DependencyObject>();
        void Visit(DependencyObject element)
        {
            if (!seen.Add(element)) return;
            void Add(DependencyProperty property)
            {
                if (BindingOperations.IsDataBound(element, property)) return;
                if (element.GetValue(property) is string value && Translations.ContainsKey(value))
                    entries.Add((element, property, value));
            }
            if (element is TextBlock) Add(TextBlock.TextProperty);
            if (element is HeaderedContentControl) Add(HeaderedContentControl.HeaderProperty);
            if (element is ContentControl) Add(ContentControl.ContentProperty);
            if (element is FrameworkElement) Add(FrameworkElement.ToolTipProperty);
            foreach (var child in LogicalTreeHelper.GetChildren(element))
                if (child is DependencyObject dependency) Visit(dependency);
        }
        Visit(root);
        return entries;
    }

    public static void Apply(IEnumerable<(DependencyObject Element, DependencyProperty Property, string Original)> entries, string language)
    {
        foreach (var (element, property, original) in entries)
            element.SetCurrentValue(property, Translate(original, language));
    }

    public static string Translate(string value, string language)
    {
        int index = language switch { "en" => 1, "ar" => 2, "es" => 3, "tr" => 4, _ => 0 };
        if (Translations.TryGetValue(value, out var values)) return values[index];
        foreach (var pair in Translations)
            if (pair.Key.EndsWith(": ", StringComparison.Ordinal) && value.StartsWith(pair.Key, StringComparison.Ordinal))
                return pair.Value[index] + value[pair.Key.Length..];
        return value;
    }
}

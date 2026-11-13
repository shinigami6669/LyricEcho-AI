using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.IO;
using System.Windows.Media.Imaging;
using Windows.Storage.Streams;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Threading;
using Microsoft.Win32;
using Windows.Media.Control;

namespace LyricEcho.Windows;

public record LyricLine(double? Seconds, string Original, string Translation = "");
public record SavedSong(string Title, string Artist, string Language, List<LyricLine> Lines, bool Favorite = false);
public record ApiProfile(string Name, string Endpoint, string Model, string EncryptedKey);
public record AppSettings(string Language, string Endpoint, string Model, int Theme = 0, double FontSize = 18, double Opacity = 1, bool Scroll = true, bool Overlay = false, bool Translate = true, bool Tracking = true, bool TranslationPreferenceSet = false, bool OverlayPreferenceSet = false, string UiLanguage = "fa");

public partial class MainWindow : Window
{
    private readonly HttpClient http = new() { Timeout = TimeSpan.FromSeconds(45) };
    private readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromMilliseconds(500) };
    private readonly string dataDir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "LyricEchoWindows");
    private GlobalSystemMediaTransportControlsSessionManager? manager;
    private GlobalSystemMediaTransportControlsSession? session;
    private List<LyricLine> lines = new();
    private string trackKey = "";
    private double offset;
    private bool polling;
    private FloatingLyricsWindow? overlay;
    private GlobalSystemMediaTransportControlsSessionPlaybackInfo? overlayPlayback;
    private int requestVersion;
    private bool loadingSettings;
    private bool settingsReady;
    private bool exiting;
    private System.Windows.Forms.NotifyIcon? trayIcon;
    private GlobalSystemMediaTransportControlsSession? selectedSession;
    private bool updatingSessions;
    private DateTime lastSessionRefresh = DateTime.MinValue;
    private string loadedTrackKey = "";
    private string loadedLanguage = "";
    private int translationVersion;
    private readonly List<SavedSong> library = new();
    private List<(DependencyObject Element, DependencyProperty Property, string Original)> interfaceTexts = new();

    public MainWindow()
    {
        InitializeComponent();
        interfaceTexts = InterfaceText.Capture(this);
        InterfaceLanguage.SelectedValue = "fa";
        SetModels(new[] { "gemini-flash-lite-latest", "gemini-2.5-flash", "gemini-2.5-pro" }, "gemini-flash-lite-latest");
        Loaded += async (_, _) => { LoadSettings(); LoadKey(); InitializeTray(); settingsReady = true; RefreshProfiles(); RefreshLibrary(); System.ComponentModel.DependencyPropertyDescriptor.FromProperty(TextBlock.TextProperty, typeof(TextBlock)).AddValueChanged(Status, (_, _) => LocalizeStatus()); await ConnectMedia(); timer.Start(); };
        Closing += (_, e) => { if (!exiting && trayIcon != null) { e.Cancel = true; Hide(); } };
        Closed += (_, _) => { timer.Stop(); overlay?.Close(); if (trayIcon != null) { trayIcon.Visible = false; trayIcon.Icon?.Dispose(); trayIcon.ContextMenuStrip?.Dispose(); trayIcon.Dispose(); } http.Dispose(); };
        Application.Current.SessionEnding += (_, _) => exiting = true;
        timer.Tick += async (_, _) => await PollMedia();
    }

    private string CurrentUiLanguage => InterfaceLanguage?.SelectedValue as string ?? "fa";

    private void InterfaceLanguage_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (InterfaceLanguage == null) return;
        ApplyInterfaceLanguage();
        if (!settingsReady || loadingSettings) return;
        try { SaveSettings(); }
        catch (Exception ex) { Status.Text = "ذخیره تنظیمات ناموفق بود: " + ex.Message; }
    }

    private void ApplyInterfaceLanguage()
    {
        string language = CurrentUiLanguage;
        InterfaceText.Apply(interfaceTexts, language);
        FlowDirection = language is "fa" or "ar" ? FlowDirection.RightToLeft : FlowDirection.LeftToRight;
        if (LibraryPreviewTitle != null)
            LibraryPreviewTitle.Text = LibraryList?.SelectedItem is SavedSong selected
                ? selected.Title : InterfaceText.Translate("یک آهنگ انتخاب کنید", language);
        if (LibraryList != null && LibraryCount != null) FilterLibrary();
        if (trayIcon?.ContextMenuStrip is { } menu && menu.Items.Count >= 2)
        {
            menu.Items[0].Text = InterfaceText.Translate("باز کردن LyricEcho", language);
            menu.Items[1].Text = InterfaceText.Translate("خروج کامل", language);
        }
        overlay?.SetInterfaceLanguage(language);
    }

    private bool translatingStatus;
    private void LocalizeStatus()
    {
        if (translatingStatus) return;
        var localized = InterfaceText.Translate(Status.Text, CurrentUiLanguage);
        if (localized != Status.Text)
        {
            translatingStatus = true;
            Status.Text = localized;
            translatingStatus = false;
        }
        Log(Status.Text);
    }

    private string SelectedLanguage => LanguageInput.SelectedValue as string ?? "Persian";

    private void SelectLanguage(string language)
    {
        if (string.IsNullOrWhiteSpace(language)) language = "Persian";
        foreach (ComboBoxItem item in LanguageInput.Items)
        {
            if (string.Equals(item.Tag as string, language, StringComparison.OrdinalIgnoreCase))
            {
                LanguageInput.SelectedItem = item;
                return;
            }
        }
        var custom = new ComboBoxItem { Content = language, Tag = language };
        LanguageInput.Items.Add(custom);
        LanguageInput.SelectedItem = custom;
    }

    private void InitializeTray()
    {
        var menu = new System.Windows.Forms.ContextMenuStrip();
        menu.Items.Add(InterfaceText.Translate("باز کردن LyricEcho", CurrentUiLanguage), null, (_, _) => RestoreWindow());
        menu.Items.Add(InterfaceText.Translate("خروج کامل", CurrentUiLanguage), null, (_, _) => { exiting = true; Close(); });
        var resource = Application.GetResourceStream(new Uri("pack://application:,,,/LyricEcho.Windows;component/Assets/LyricEcho.ico"));
        using var iconStream = resource.Stream;
        using var loadedIcon = new System.Drawing.Icon(iconStream);
        trayIcon = new System.Windows.Forms.NotifyIcon
        {
            Text = "LyricEcho", Icon = (System.Drawing.Icon)loadedIcon.Clone(),
            ContextMenuStrip = menu, Visible = true
        };
        trayIcon.DoubleClick += (_, _) => RestoreWindow();
    }

    private void RestoreWindow()
    {
        Show();
        if (WindowState == WindowState.Minimized) WindowState = WindowState.Normal;
        Activate();
    }

    private void SaveSettings()
    {
        Directory.CreateDirectory(dataDir);
        var settings = new AppSettings(SelectedLanguage, EndpointInput.Text.Trim(), SelectedModel,
            ThemeChoice.SelectedIndex, FontSizeSlider.Value, OverlayOpacity.Value, AutoScroll.IsChecked == true,
            AutoOverlay.IsChecked == true, AutoTranslate.IsChecked == true, TrackingEnabled.IsChecked == true, true, true, InterfaceLanguage.SelectedValue as string ?? "fa");
        File.WriteAllText(Path.Combine(dataDir, "settings.json"), JsonSerializer.Serialize(settings));
    }

    private async void LanguageInput_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (!settingsReady || loadingSettings) return;
        try { SaveSettings(); }
        catch (Exception ex) { Status.Text = "ذخیره زبان ناموفق بود: " + ex.Message; }
        if (!string.IsNullOrWhiteSpace(TitleInput.Text)) await FetchLyrics(TitleInput.Text.Trim(), ArtistInput.Text.Trim());
    }

    private async void AutoTranslate_Changed(object sender, RoutedEventArgs e)
    {
        if (!settingsReady || loadingSettings) return;
        try { SaveSettings(); }
        catch (Exception ex) { Status.Text = "ذخیره تنظیمات ناموفق بود: " + ex.Message; }
        if (AutoTranslate.IsChecked == true && lines.Count > 0 && !lines.Any(line => !string.IsNullOrWhiteSpace(line.Translation)))
            await TranslateLyrics();
    }

    private string SelectedModel => ModelInput.SelectedItem as string ?? "gemini-flash-lite-latest";

    private void SetModels(System.Collections.Generic.IEnumerable<string> models, string selected)
    {
        var options = models.Where(x => !string.IsNullOrWhiteSpace(x)).Distinct(StringComparer.OrdinalIgnoreCase).ToList();
        if (!string.IsNullOrWhiteSpace(selected) && !options.Contains(selected, StringComparer.OrdinalIgnoreCase)) options.Insert(0, selected);
        ModelInput.ItemsSource = options;
        ModelInput.SelectedItem = options.FirstOrDefault(x => string.Equals(x, selected, StringComparison.OrdinalIgnoreCase)) ?? options.FirstOrDefault();
    }

    private void SelectModel(string model)
    {
        var options = ModelInput.Items.Cast<string>().ToList();
        SetModels(options, model);
    }

    private static string[] ModelsForProvider(int provider) => provider switch
    {
        0 => new[] { "gemini-flash-lite-latest", "gemini-2.5-flash", "gemini-2.5-pro" },
        1 => new[] { "gpt-4o-mini", "gpt-4o" },
        2 => new[] { "meta-llama/llama-3.3-70b-instruct:free" },
        3 => new[] { "deepseek-chat", "deepseek-reasoner" },
        4 => new[] { "llama-3.3-70b-versatile" },
        5 => new[] { "llama3:latest" },
        _ => Array.Empty<string>()
    };

    private void AddCustomModel_Click(object sender, RoutedEventArgs e)
    {
        string model = CustomModelInput.Text.Trim();
        if (string.IsNullOrEmpty(model)) { Status.Text = "نام مدل را وارد کنید."; return; }
        SelectModel(model);
        CustomModelInput.Clear();
    }

    private void AutoOverlay_Changed(object sender, RoutedEventArgs e)
    {
        if (!settingsReady || loadingSettings) return;
        try { SaveSettings(); }
        catch (Exception ex) { Status.Text = "ذخیره تنظیمات ناموفق بود: " + ex.Message; }
    }

    private async Task LoadAlbumArt(GlobalSystemMediaTransportControlsSessionMediaProperties properties, string expectedTrack)
    {
        if (properties.Thumbnail == null) return;
        try
        {
            using var stream = await properties.Thumbnail.OpenReadAsync();
            if (stream.Size == 0 || stream.Size > 8 * 1024 * 1024) return;
            using var reader = new DataReader(stream.GetInputStreamAt(0));
            uint size = (uint)stream.Size;
            if (await reader.LoadAsync(size) != size) return;
            var bytes = new byte[size];
            reader.ReadBytes(bytes);
            if (trackKey != expectedTrack) return;
            using var imageStream = new MemoryStream(bytes);
            var bitmap = new BitmapImage();
            bitmap.BeginInit();
            bitmap.CacheOption = BitmapCacheOption.OnLoad;
            bitmap.StreamSource = imageStream;
            bitmap.EndInit();
            bitmap.Freeze();
            AlbumArt.Source = bitmap;
        }
        catch (Exception) { /* Some players do not expose readable artwork. */ }
    }

    private async Task ConnectMedia()
    {
        try { manager = await GlobalSystemMediaTransportControlsSessionManager.RequestAsync(); await PollMedia(); }
        catch (Exception ex) { Status.Text = "اتصال به رسانه ویندوز ممکن نشد: " + ex.Message; }
    }

    private async Task PollMedia()
    {
        if (polling || manager == null) return;
        polling = true;
        try
        {
            if (TrackingEnabled.IsChecked != true) { UpdateOverlayPlayback(null); PlaybackStatus.Text = "تشخیص خودکار غیرفعال است."; return; }
            if ((DateTime.UtcNow - lastSessionRefresh).TotalSeconds > 3) RefreshSessions();
            session = selectedSession ?? manager.GetCurrentSession();
            if (session == null) { AlbumArt.Source = null; trackKey = ""; UpdateOverlayPlayback(null); PlaybackStatus.Text = "پخش‌کننده فعالی پیدا نشد؛ می‌توانید دستی جست‌وجو کنید."; return; }
            var props = await session.TryGetMediaPropertiesAsync();
            var title = props.Title?.Trim() ?? "";
            var artist = props.Artist?.Trim() ?? "";
            var key = title + "\n" + artist;
            if (!string.IsNullOrWhiteSpace(title) && key != trackKey)
            {
                trackKey = key;
                TitleInput.Text = title; ArtistInput.Text = artist;
                AlbumArt.Source = null;
                _ = LoadAlbumArt(props, key);
                UpdateOverlay();
                if (AutoOverlay.IsChecked == true && overlay == null) Overlay_Click(this, new RoutedEventArgs());
                _ = FetchLyrics(title, artist);
            }
            var timeline = session.GetTimelineProperties();
            var playback = session.GetPlaybackInfo();
            UpdateOverlayPlayback(playback);
            double pos = timeline.Position.TotalSeconds;
            if (playback.PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing)
                pos += Math.Max(0, (DateTimeOffset.UtcNow - timeline.LastUpdatedTime).TotalSeconds) * (playback.PlaybackRate ?? 1);
            if (timeline.EndTime > TimeSpan.Zero) pos = Math.Min(pos, timeline.EndTime.TotalSeconds);
            if (!ProgressSlider.IsMouseCaptureWithin) { ProgressSlider.Maximum = Math.Max(1, timeline.EndTime.TotalSeconds); ProgressSlider.Value = Math.Clamp(pos, 0, ProgressSlider.Maximum); }
            PlaybackStatus.Text = $"{title} — {artist} | {TimeSpan.FromSeconds(Math.Max(0, pos)):mm\\:ss} | {playback.PlaybackStatus}";
            if (!string.Equals(loadedTrackKey, key, StringComparison.OrdinalIgnoreCase) || !lines.Any(line => line.Seconds.HasValue)) return;
            int index = -1;
            for (int i = 0; i < lines.Count; i++) if (lines[i].Seconds is double t && t <= pos + offset) index = i;
            if (LyricsList.SelectedIndex != index) { LyricsList.SelectedIndex = index; if (index >= 0 && AutoScroll.IsChecked == true) LyricsList.ScrollIntoView(lines[index]); }
        }
        catch (Exception ex) { UpdateOverlayPlayback(null); PlaybackStatus.Text = "خطا در خواندن پخش‌کننده: " + ex.Message; }
        finally { polling = false; }
    }

    private async void Detect_Click(object sender, RoutedEventArgs e) { trackKey = ""; if (manager == null) await ConnectMedia(); else await PollMedia(); }
    private async void Fetch_Click(object sender, RoutedEventArgs e) => await FetchLyrics(TitleInput.Text.Trim(), ArtistInput.Text.Trim());
    private async Task FetchLyrics(string title, string artist, bool force = false)
    {
        if (string.IsNullOrWhiteSpace(title)) { Status.Text = "نام آهنگ را وارد کنید."; return; }
        int version = ++requestVersion;
        string language = SelectedLanguage;
        loadedTrackKey = "";
        loadedLanguage = "";
        SetLines(new List<LyricLine>());
        try
        {
            Status.Text = "در حال جست‌وجو...";
            var cached = force ? null : await ReadCache(title, artist, language);
            if (version != requestVersion) return;
            if (cached != null) { loadedTrackKey = title + "\n" + artist; loadedLanguage = language; SetLines(cached.Lines); Status.Text = "بارگذاری از کتابخانه محلی"; if (AutoTranslate.IsChecked == true && !cached.Lines.Any(x => !string.IsNullOrWhiteSpace(x.Translation))) await TranslateLyrics(); return; }
            var uri = "https://lrclib.net/api/get?track_name=" + Uri.EscapeDataString(title) + "&artist_name=" + Uri.EscapeDataString(artist);
            using var response = await http.GetAsync(uri);
            response.EnsureSuccessStatusCode();
            using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
            string? synced = GetString(json.RootElement, "syncedLyrics");
            string? plain = GetString(json.RootElement, "plainLyrics");
            if (version != requestVersion) return;
            var parsed = ParseLyrics(!string.IsNullOrWhiteSpace(synced) ? synced : plain ?? "");
            loadedTrackKey = title + "\n" + artist;
            loadedLanguage = language;
            SetLines(parsed);
            Status.Text = parsed.Count == 0 ? "متنی پیدا نشد." : (string.IsNullOrWhiteSpace(synced) ? "متن دریافت شد؛ زمان‌بندی در دسترس نیست." : "متن زمان‌بندی‌شده دریافت شد.");
            if (parsed.Count > 0) { await SaveCache(title, artist, language, parsed); RefreshLibrary(); if (AutoTranslate.IsChecked == true && version == requestVersion) await TranslateLyrics(); }
        }
        catch (Exception ex) { if (version == requestVersion) Status.Text = "دریافت متن ناموفق بود: " + ex.Message; }
    }
    private static string? GetString(JsonElement element, string name) => element.TryGetProperty(name, out var p) && p.ValueKind == JsonValueKind.String ? p.GetString() : null;
    private static List<LyricLine> ParseLyrics(string text)
    {
        var result = new List<LyricLine>();
        foreach (var raw in text.Replace("\r", "").Split('\n'))
        {
            var matches = Regex.Matches(raw, @"\[(\d+):(\d{2})(?:[\.:](\d{1,3}))?\]");
            string content = Regex.Replace(raw, @"\[\d+:\d{2}(?:[\.:]\d{1,3})?\]", "").Trim();
            if (string.IsNullOrWhiteSpace(content)) continue;
            if (matches.Count == 0) { if (!raw.StartsWith('[')) result.Add(new(null, content)); continue; }
            foreach (Match m in matches)
            {
                double fraction = m.Groups[3].Success ? double.Parse("0." + m.Groups[3].Value, System.Globalization.CultureInfo.InvariantCulture) : 0;
                result.Add(new(int.Parse(m.Groups[1].Value) * 60 + int.Parse(m.Groups[2].Value) + fraction, content));
            }
        }
        return result.OrderBy(x => x.Seconds ?? double.MaxValue).ToList();
    }
    private void SetLines(List<LyricLine> newLines) { lines = newLines; LyricsList.ItemsSource = null; LyricsList.ItemsSource = lines; UpdateOverlay(); }
    private string KeyPath => Path.Combine(dataDir, "api-key.dat");
    private void LoadKey()
    {
        try { if (File.Exists(KeyPath)) ApiKey.Password = System.Text.Encoding.UTF8.GetString(System.Security.Cryptography.ProtectedData.Unprotect(File.ReadAllBytes(KeyPath), null, System.Security.Cryptography.DataProtectionScope.CurrentUser)); }
        catch (Exception) { Status.Text = "خواندن کلید ذخیره‌شده ممکن نشد؛ کلید را دوباره وارد کنید."; }
    }
    private void SaveKey_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            Directory.CreateDirectory(dataDir);
            SaveSettings();
            if (string.IsNullOrWhiteSpace(ApiKey.Password)) { if (File.Exists(KeyPath)) File.Delete(KeyPath); Status.Text = "کلید ذخیره‌شده حذف شد."; return; }
            File.WriteAllBytes(KeyPath, System.Security.Cryptography.ProtectedData.Protect(System.Text.Encoding.UTF8.GetBytes(ApiKey.Password), null, System.Security.Cryptography.DataProtectionScope.CurrentUser));
            Status.Text = "تنظیمات و کلید ذخیره شدند.";
        }
        catch (Exception ex) { Status.Text = "ذخیره کلید ناموفق بود: " + ex.Message; }
    }
    private async void Translate_Click(object sender, RoutedEventArgs e) => await TranslateLyrics();
    private async Task TranslateLyrics()
    {
        if (lines.Count == 0) { Status.Text = "ابتدا متن آهنگ را دریافت کنید."; return; }
        string endpoint = EndpointInput.Text.Trim().TrimEnd('/');
        string model = SelectedModel;
        string language = SelectedLanguage;
        string title = TitleInput.Text.Trim(), artist = ArtistInput.Text.Trim();
        if (!string.Equals(loadedTrackKey, title + "\n" + artist, StringComparison.OrdinalIgnoreCase) ||
            !string.Equals(loadedLanguage, language, StringComparison.OrdinalIgnoreCase))
        {
            Status.Text = "ابتدا متن همین آهنگ و زبان را بارگذاری کنید.";
            return;
        }
        var input = lines.ToList();
        int version = requestVersion;
        int currentTranslation = ++translationVersion;
        try
        {
            Status.Text = "در حال ترجمه...";
            if (!Uri.TryCreate(endpoint, UriKind.Absolute, out var endpointUri) || (endpointUri.Scheme != "https" && !(endpointUri.Scheme == "http" && endpointUri.IsLoopback))) throw new InvalidDataException(InterfaceText.Translate("نشانی API باید HTTPS یا آدرس محلی باشد.", CurrentUiLanguage));
            bool gemini = endpointUri.Host.Equals("generativelanguage.googleapis.com", StringComparison.OrdinalIgnoreCase);
            var instruction = "Translate each item into " + language + ". Return only a JSON array of strings, with exactly the same number and order of items. Do not add commentary.\n" + JsonSerializer.Serialize(input.Select(x => x.Original));
            using var req = gemini
                ? new HttpRequestMessage(HttpMethod.Post, endpoint + "/v1beta/models/" + Uri.EscapeDataString(model) + ":generateContent") { Content = JsonContent.Create(new { contents = new[] { new { parts = new[] { new { text = instruction } } } }, generationConfig = new { temperature = 0.3, responseMimeType = "application/json" } }) }
                : new HttpRequestMessage(HttpMethod.Post, endpoint + "/chat/completions") { Content = JsonContent.Create(new { model, temperature = 0.3, messages = new[] { new { role = "user", content = instruction } } }) };
            if (!string.IsNullOrWhiteSpace(ApiKey.Password))
            {
                if (gemini) req.Headers.Add("x-goog-api-key", ApiKey.Password);
                else req.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", ApiKey.Password);
            }
            using var res = await http.SendAsync(req);
            if (!res.IsSuccessStatusCode) throw new HttpRequestException(string.Format(InterfaceText.Translate("خطای سرویس ({0}). آدرس، مدل و کلید را بررسی کنید.", CurrentUiLanguage), (int)res.StatusCode));
            using var doc = JsonDocument.Parse(await res.Content.ReadAsStringAsync());
            string content = gemini
                ? doc.RootElement.GetProperty("candidates")[0].GetProperty("content").GetProperty("parts")[0].GetProperty("text").GetString() ?? ""
                : doc.RootElement.GetProperty("choices")[0].GetProperty("message").GetProperty("content").GetString() ?? "";
            content = Regex.Replace(content, @"<think>[\s\S]*?</think>", "", RegexOptions.IgnoreCase).Trim();
            int start = content.IndexOf('['), end = content.LastIndexOf(']');
            if (start < 0 || end < start) throw new InvalidDataException(InterfaceText.Translate("پاسخ مدل آرایه ترجمه ندارد.", CurrentUiLanguage));
            var translations = JsonSerializer.Deserialize<List<string>>(content[start..(end + 1)]) ?? new();
            if (translations.Count != input.Count) throw new InvalidDataException(InterfaceText.Translate("تعداد خطوط ترجمه با متن اصلی برابر نیست.", CurrentUiLanguage));
            if (version != requestVersion || currentTranslation != translationVersion ||
                !string.Equals(loadedTrackKey, TitleInput.Text.Trim() + "\n" + ArtistInput.Text.Trim(), StringComparison.OrdinalIgnoreCase) ||
                !string.Equals(loadedLanguage, SelectedLanguage, StringComparison.OrdinalIgnoreCase)) return;
            var translated = input.Select((x, i) => x with { Translation = translations[i] }).ToList();
            SetLines(translated); await SaveCache(title, artist, language, translated); RefreshLibrary(); Status.Text = "ترجمه ذخیره شد.";
        }
        catch (Exception ex) { if (version == requestVersion && currentTranslation == translationVersion) Status.Text = "ترجمه ناموفق بود: " + ex.Message; }
    }
    private string CachePath(string title, string artist, string language)
    {
        string key = Convert.ToHexString(System.Security.Cryptography.SHA256.HashData(System.Text.Encoding.UTF8.GetBytes((title.Trim() + "\n" + artist.Trim() + "\n" + language.Trim()).ToLowerInvariant())));
        return Path.Combine(dataDir, key + ".json");
    }
    private async Task<SavedSong?> ReadCache(string title, string artist, string language)
    {
        string path = CachePath(title, artist, language);
        if (!File.Exists(path)) return null;
        try { return JsonSerializer.Deserialize<SavedSong>(await File.ReadAllTextAsync(path)); } catch (JsonException) { return null; }
    }
    private async Task SaveCache(string title, string artist, string language, List<LyricLine> data)
    {
        Directory.CreateDirectory(dataDir);
        var previous = await ReadCache(title, artist, language);
        await File.WriteAllTextAsync(CachePath(title, artist, language), JsonSerializer.Serialize(new SavedSong(title, artist, language, data, previous?.Favorite ?? false)));
    }
    private void Open_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog { Filter = "Lyrics|*.lrc;*.txt|All files|*.*" };
        if (dialog.ShowDialog() != true) return;
        try { requestVersion++; loadedTrackKey = TitleInput.Text.Trim() + "\n" + ArtistInput.Text.Trim(); loadedLanguage = SelectedLanguage; SetLines(ParseLyrics(File.ReadAllText(dialog.FileName))); Status.Text = "فایل باز شد."; }
        catch (Exception ex) { Status.Text = "باز کردن فایل ناموفق بود: " + ex.Message; }
    }

    private void LoadSettings()
    {
        try
        {
            string path = Path.Combine(dataDir, "settings.json");
            if (!File.Exists(path)) { ThemeChoice.SelectedIndex = 0; ProviderChoice.SelectedIndex = 0; return; }
            var saved = JsonSerializer.Deserialize<AppSettings>(File.ReadAllText(path));
            if (saved == null) return;
            loadingSettings = true; InterfaceLanguage.SelectedValue = saved.UiLanguage ?? "fa"; ApplyInterfaceLanguage(); SelectLanguage(saved.Language); EndpointInput.Text = saved.Endpoint; SelectModel(saved.Model); ThemeChoice.SelectedIndex = Math.Clamp(saved.Theme, 0, 4); FontSizeSlider.Value = Math.Clamp(saved.FontSize, 13, 28); OverlayOpacity.Value = Math.Clamp(saved.Opacity, 0.35, 1); AutoScroll.IsChecked = saved.Scroll; AutoOverlay.IsChecked = saved.OverlayPreferenceSet ? saved.Overlay : true; AutoTranslate.IsChecked = saved.TranslationPreferenceSet ? saved.Translate : true; TrackingEnabled.IsChecked = saved.Tracking; loadingSettings = false; ApplyTheme(); ApplyFont();
        }
        catch (Exception) { Status.Text = "خواندن تنظیمات ممکن نشد؛ مقادیر پیش‌فرض استفاده می‌شوند."; }
    }
    private void GeminiDefaults_Click(object sender, RoutedEventArgs e)
    { EndpointInput.Text = "https://generativelanguage.googleapis.com"; SelectModel("gemini-flash-lite-latest"); }

    private void RefreshLibrary()
    {
        library.Clear();
        if (Directory.Exists(dataDir))
            foreach (string file in Directory.EnumerateFiles(dataDir, "*.json"))
                try { var song = JsonSerializer.Deserialize<SavedSong>(File.ReadAllText(file)); if (song?.Lines != null && !string.IsNullOrWhiteSpace(song.Title)) library.Add(song); }
                catch (Exception ex) when (ex is IOException or JsonException or UnauthorizedAccessException) { }
        FilterLibrary();
    }
    private void FilterLibrary()
    {
        if (LibraryList == null || LibrarySearch == null || LibraryCount == null) return;
        string query = LibrarySearch.Text.Trim();
        var selectedSong = LibraryList.SelectedItem as SavedSong;
        var filtered = library.Where(x => (FavoritesOnly?.IsChecked != true || x.Favorite) && (x.Title.Contains(query, StringComparison.CurrentCultureIgnoreCase) || x.Artist.Contains(query, StringComparison.CurrentCultureIgnoreCase))).OrderBy(x => x.Title).ToList();
        LibraryList.ItemsSource = filtered;
        if (selectedSong != null)
            LibraryList.SelectedItem = filtered.FirstOrDefault(song => song.Title == selectedSong.Title && song.Artist == selectedSong.Artist && song.Language == selectedSong.Language);
        LibraryCount.Text = CurrentUiLanguage switch { "en" => $"{filtered.Count} songs", "ar" => $"{filtered.Count} أغنية", "es" => $"{filtered.Count} canciones", "tr" => $"{filtered.Count} şarkı", _ => $"{filtered.Count} آهنگ" };
    }
    private void LibrarySearch_TextChanged(object sender, TextChangedEventArgs e) => FilterLibrary();
    private void LibraryRefresh_Click(object sender, RoutedEventArgs e) => RefreshLibrary();
    private void LibraryList_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (LibraryPreviewLines == null || LibraryPreviewTitle == null || LibraryPreviewArtist == null) return;
        if (LibraryList.SelectedItem is SavedSong song)
        {
            LibraryPreviewTitle.Text = song.Title;
            LibraryPreviewArtist.Text = song.Artist;
            LibraryPreviewLines.ItemsSource = song.Lines;
        }
        else
        {
            LibraryPreviewTitle.Text = InterfaceText.Translate("یک آهنگ انتخاب کنید", InterfaceLanguage?.SelectedValue as string ?? "fa");
            LibraryPreviewArtist.Text = "";
            LibraryPreviewLines.ItemsSource = null;
        }
    }
    private async Task MediaAction(Func<GlobalSystemMediaTransportControlsSession, Task<bool>> action)
    {
        try { if (session == null) { Status.Text = "پخش‌کننده‌ای متصل نیست."; return; } if (!await action(session)) Status.Text = "این فرمان در پخش‌کننده فعلی پشتیبانی نشد."; }
        catch (Exception ex) { Status.Text = "فرمان پخش انجام نشد: " + ex.Message; }
    }
    private async void PlayPause_Click(object sender, RoutedEventArgs e) => await MediaAction(async x => await x.TryTogglePlayPauseAsync());
    private async Task SeekRelative(double delta)
    {
        if (session == null) { Status.Text = "پخش‌کننده‌ای متصل نیست."; return; }
        var timeline = session.GetTimelineProperties();
        double seconds = Math.Clamp(timeline.Position.TotalSeconds + delta, 0, Math.Max(0, timeline.EndTime.TotalSeconds));
        await MediaAction(async x => await x.TryChangePlaybackPositionAsync(TimeSpan.FromSeconds(seconds).Ticks));
    }
    private async void SeekBack_Click(object sender, RoutedEventArgs e) => await SeekRelative(-5);
    private async void SeekForward_Click(object sender, RoutedEventArgs e) => await SeekRelative(5);
    private async void Previous_Click(object sender, RoutedEventArgs e) => await MediaAction(async x => await x.TrySkipPreviousAsync());
    private async void Next_Click(object sender, RoutedEventArgs e) => await MediaAction(async x => await x.TrySkipNextAsync());


    private string ProfilesPath => Path.Combine(dataDir, "profiles.json");
    private List<ApiProfile> ReadProfiles()
    {
        try { return File.Exists(ProfilesPath) ? JsonSerializer.Deserialize<List<ApiProfile>>(File.ReadAllText(ProfilesPath)) ?? new() : new(); }
        catch (Exception ex) when (ex is IOException or JsonException or UnauthorizedAccessException) { Log("خطا در خواندن پروفایل‌ها"); return new(); }
    }
    private void RefreshProfiles()
    {
        if (ProfileChoice == null) return;
        ProfileChoice.ItemsSource = ReadProfiles().Select(x => x.Name).OrderBy(x => x).ToArray();
    }
    private void SaveProfile_Click(object sender, RoutedEventArgs e)
    {
        string name = ProfileName.Text.Trim(); if (string.IsNullOrWhiteSpace(name)) { Status.Text = "نام پروفایل را وارد کنید."; return; }
        try
        {
            var profiles = ReadProfiles();
            var encrypted = Convert.ToBase64String(System.Security.Cryptography.ProtectedData.Protect(System.Text.Encoding.UTF8.GetBytes(ApiKey.Password), null, System.Security.Cryptography.DataProtectionScope.CurrentUser));
            profiles.RemoveAll(x => string.Equals(x.Name, name, StringComparison.OrdinalIgnoreCase));
            profiles.Add(new ApiProfile(name, EndpointInput.Text.Trim(), SelectedModel, encrypted));
            Directory.CreateDirectory(dataDir); File.WriteAllText(ProfilesPath, JsonSerializer.Serialize(profiles)); RefreshProfiles(); ProfileChoice.SelectedItem = name; Status.Text = "پروفایل ذخیره شد.";
        }
        catch (Exception ex) { Status.Text = "ذخیره پروفایل ناموفق بود: " + ex.Message; }
    }
    private void LoadProfile_Click(object sender, RoutedEventArgs e)
    {
        if (ProfileChoice.SelectedItem is not string name) { Status.Text = "یک پروفایل انتخاب کنید."; return; }
        try
        {
            var profile = ReadProfiles().First(x => x.Name == name);
            EndpointInput.Text = profile.Endpoint; SelectModel(profile.Model); ProfileName.Text = profile.Name;
            ApiKey.Password = System.Text.Encoding.UTF8.GetString(System.Security.Cryptography.ProtectedData.Unprotect(Convert.FromBase64String(profile.EncryptedKey), null, System.Security.Cryptography.DataProtectionScope.CurrentUser));
            Status.Text = "پروفایل فعال شد؛ برای پیش‌فرض شدن تنظیمات را ذخیره کنید.";
        }
        catch (Exception ex) { Status.Text = "فعال‌سازی پروفایل ناموفق بود: " + ex.Message; }
    }
    private void DeleteProfile_Click(object sender, RoutedEventArgs e)
    {
        if (ProfileChoice.SelectedItem is not string name) return;
        if (MessageBox.Show(string.Format(InterfaceText.Translate("پروفایل «{0}» حذف شود؟", CurrentUiLanguage), name), InterfaceText.Translate("حذف پروفایل", CurrentUiLanguage), MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes) return;
        try { var profiles = ReadProfiles(); profiles.RemoveAll(x => x.Name == name); File.WriteAllText(ProfilesPath, JsonSerializer.Serialize(profiles)); RefreshProfiles(); Status.Text = "پروفایل حذف شد."; }
        catch (Exception ex) { Status.Text = "حذف پروفایل ناموفق بود: " + ex.Message; }
    }
    private void Log(string message)
    {
        if (DiagnosticsText == null || string.IsNullOrWhiteSpace(message)) return;
        DiagnosticsText.AppendText($"[{DateTime.Now:HH:mm:ss}] {message}\n");
        if (DiagnosticsText.LineCount > 300) DiagnosticsText.Text = DiagnosticsText.Text[^Math.Min(DiagnosticsText.Text.Length, 20000)..];
        DiagnosticsText.ScrollToEnd();
    }
    private void ClearLog_Click(object sender, RoutedEventArgs e) => DiagnosticsText.Clear();
    private void ThemeChoice_SelectionChanged(object sender, SelectionChangedEventArgs e) => ApplyTheme();
    private void ApplyTheme()
    {
        if (ThemeChoice == null) return;
        string[] colors = { "#7547CF", "#087F9E", "#168A67", "#C14370", "#AC751A" };
        int index = Math.Clamp(ThemeChoice.SelectedIndex, 0, 4);
        string[] backgrounds = { "#090C14", "#06131A", "#071711", "#190B14", "#181207" };
        string[] panels = { "#151A29", "#102631", "#10271E", "#2B1522", "#2B2112" };
        Resources["Accent"] = new SolidColorBrush((Color)ColorConverter.ConvertFromString(colors[index]));
        Resources["BackgroundBrush"] = new SolidColorBrush((Color)ColorConverter.ConvertFromString(backgrounds[index]));
        Resources["Panel"] = new SolidColorBrush((Color)ColorConverter.ConvertFromString(panels[index]));
        overlay?.ApplyPalette((Brush)Resources["Panel"], (Brush)Resources["Accent"]);
    }
    private void FontSizeSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e) => ApplyFont();
    private void ApplyFont() { if (LyricsList != null && FontSizeSlider != null) LyricsList.LayoutTransform = new ScaleTransform(FontSizeSlider.Value / 18, FontSizeSlider.Value / 18); }
    private void OverlayOpacity_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e) { if (overlay != null && OverlayOpacity != null) overlay.Opacity = OverlayOpacity.Value; }
    private void ProviderChoice_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (loadingSettings || EndpointInput == null || ModelInput == null || ProviderChoice == null) return;
        (string url, string model) = ProviderChoice.SelectedIndex switch {
            0 => ("https://generativelanguage.googleapis.com", "gemini-flash-lite-latest"),
            1 => ("https://api.openai.com/v1", "gpt-4o-mini"),
            2 => ("https://openrouter.ai/api/v1", "meta-llama/llama-3.3-70b-instruct:free"),
            3 => ("https://api.deepseek.com/v1", "deepseek-chat"),
            4 => ("https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
            5 => ("http://localhost:11434/v1", "llama3:latest"),
            _ => (EndpointInput.Text, SelectedModel) };
        EndpointInput.Text = url; SetModels(ModelsForProvider(ProviderChoice.SelectedIndex), model);
    }
    private void RefreshSessions()
    {
        if (manager == null || SessionChoice == null) return;
        lastSessionRefresh = DateTime.UtcNow;
        var sessions = manager.GetSessions();
        if (selectedSession != null && !sessions.Any(x => x.SourceAppUserModelId == selectedSession.SourceAppUserModelId)) selectedSession = null;
        var ids = sessions.Select(x => x.SourceAppUserModelId).ToArray();
        var currentIds = SessionChoice.Items.Cast<ComboBoxItem>().Select(x => x.Content?.ToString()).ToArray();
        if (ids.SequenceEqual(currentIds)) return;
        updatingSessions = true; SessionChoice.Items.Clear();
        foreach (var item in sessions) SessionChoice.Items.Add(new ComboBoxItem { Content = item.SourceAppUserModelId, Tag = item });
        if (selectedSession != null) foreach (ComboBoxItem item in SessionChoice.Items) if ((item.Tag as GlobalSystemMediaTransportControlsSession)?.SourceAppUserModelId == selectedSession.SourceAppUserModelId) { SessionChoice.SelectedItem = item; selectedSession = (GlobalSystemMediaTransportControlsSession)item.Tag; break; }
        updatingSessions = false;
    }
    private void SessionChoice_SelectionChanged(object sender, SelectionChangedEventArgs e)
    { if (updatingSessions || SessionChoice?.SelectedItem is not ComboBoxItem item) return; selectedSession = item.Tag as GlobalSystemMediaTransportControlsSession; trackKey = ""; }
    private void AutoSession_Click(object sender, RoutedEventArgs e) { selectedSession = null; updatingSessions = true; SessionChoice.SelectedIndex = -1; updatingSessions = false; trackKey = ""; Status.Text = "انتخاب خودکار پخش‌کننده فعال شد."; }
    private async void ProgressSlider_MouseUp(object sender, System.Windows.Input.MouseButtonEventArgs e)
    { if (session == null) return; long position = TimeSpan.FromSeconds(ProgressSlider.Value).Ticks; await MediaAction(async x => await x.TryChangePlaybackPositionAsync(position)); }
    private async void RefreshLyrics_Click(object sender, RoutedEventArgs e) => await FetchLyrics(TitleInput.Text.Trim(), ArtistInput.Text.Trim(), true);
    private void LibraryFilterChanged(object sender, RoutedEventArgs e) => FilterLibrary();
    private async void FavoriteCurrent_Click(object sender, RoutedEventArgs e)
    {
        var song = await ReadCache(TitleInput.Text.Trim(), ArtistInput.Text.Trim(), SelectedLanguage);
        if (song == null) { Status.Text = "ابتدا آهنگ را در کتابخانه ذخیره کنید."; return; }
        await ToggleFavorite(song);
    }
    private async void FavoriteLibrary_Click(object sender, RoutedEventArgs e)
    { if (LibraryList.SelectedItem is SavedSong song) await ToggleFavorite(song); else Status.Text = "یک آهنگ از کتابخانه انتخاب کنید."; }
    private async Task ToggleFavorite(SavedSong song)
    {
        try { await File.WriteAllTextAsync(CachePath(song.Title, song.Artist, song.Language), JsonSerializer.Serialize(song with { Favorite = !song.Favorite })); RefreshLibrary(); Status.Text = song.Favorite ? "از علاقه‌مندی‌ها حذف شد." : "به علاقه‌مندی‌ها اضافه شد."; }
        catch (Exception ex) { Status.Text = "ذخیره علاقه‌مندی ناموفق بود: " + ex.Message; }
    }
    private void DeleteLibrary_Click(object sender, RoutedEventArgs e)
    {
        if (LibraryList.SelectedItem is not SavedSong song) return;
        if (MessageBox.Show(string.Format(InterfaceText.Translate("آهنگ «{0}» از کتابخانه حذف شود؟", CurrentUiLanguage), song.Title), InterfaceText.Translate("حذف آهنگ", CurrentUiLanguage), MessageBoxButton.YesNo, MessageBoxImage.Question) != MessageBoxResult.Yes) return;
        try { File.Delete(CachePath(song.Title, song.Artist, song.Language)); RefreshLibrary(); Status.Text = "آهنگ حذف شد."; }
        catch (Exception ex) { Status.Text = "حذف ناموفق بود: " + ex.Message; }
    }
    private void Manual_Click(object sender, RoutedEventArgs e)
    {
        var text = new TextBox { AcceptsReturn = true, TextWrapping = TextWrapping.Wrap, VerticalScrollBarVisibility = ScrollBarVisibility.Auto, FlowDirection = FlowDirection.LeftToRight, Margin = new Thickness(5) };
        var translation = new TextBox { AcceptsReturn = true, TextWrapping = TextWrapping.Wrap, VerticalScrollBarVisibility = ScrollBarVisibility.Auto, FlowDirection = FlowDirection.RightToLeft, Margin = new Thickness(5) };
        var save = new Button { Content = InterfaceText.Translate("نمایش و ذخیره", CurrentUiLanguage), Margin = new Thickness(10) };
        var columns = new Grid { Margin = new Thickness(10) }; columns.ColumnDefinitions.Add(new ColumnDefinition()); columns.ColumnDefinitions.Add(new ColumnDefinition());
        var originalPanel = new DockPanel(); var originalLabel = new TextBlock { Text = InterfaceText.Translate("متن اصلی یا LRC", CurrentUiLanguage), Margin = new Thickness(5) }; DockPanel.SetDock(originalLabel, Dock.Top); originalPanel.Children.Add(originalLabel); originalPanel.Children.Add(text);
        var translationPanel = new DockPanel(); var translationLabel = new TextBlock { Text = InterfaceText.Translate("ترجمه (اختیاری، هر خط جدا)", CurrentUiLanguage), Margin = new Thickness(5) }; DockPanel.SetDock(translationLabel, Dock.Top); translationPanel.Children.Add(translationLabel); translationPanel.Children.Add(translation);
        Grid.SetColumn(translationPanel, 1); columns.Children.Add(originalPanel); columns.Children.Add(translationPanel);
        var layout = new DockPanel(); DockPanel.SetDock(save, Dock.Bottom); layout.Children.Add(save); layout.Children.Add(columns);
        var dialog = new Window { Title = InterfaceText.Translate("ورود متن یا LRC", CurrentUiLanguage), Width = 800, Height = 520, Owner = this, Content = layout, WindowStartupLocation = WindowStartupLocation.CenterOwner };
        save.Click += async (_, _) => {
            var parsed = ParseLyrics(text.Text); if (parsed.Count == 0) { Status.Text = "متنی وارد نشده است."; return; }
            var translated = translation.Text.Replace("\r", "").Split('\n');
            if (!string.IsNullOrWhiteSpace(translation.Text) && translated.Length != parsed.Count) { MessageBox.Show(dialog, InterfaceText.Translate("تعداد خطوط ترجمه باید با متن اصلی یکسان باشد.", CurrentUiLanguage)); return; }
            var final = parsed.Select((x, i) => x with { Translation = string.IsNullOrWhiteSpace(translation.Text) ? "" : translated[i] }).ToList();
            requestVersion++; loadedTrackKey = TitleInput.Text.Trim() + "\n" + ArtistInput.Text.Trim(); loadedLanguage = SelectedLanguage; SetLines(final);
            try { if (!string.IsNullOrWhiteSpace(TitleInput.Text)) { await SaveCache(TitleInput.Text.Trim(), ArtistInput.Text.Trim(), SelectedLanguage, final); RefreshLibrary(); } Status.Text = string.Format(InterfaceText.Translate("{0} خط بارگذاری شد.", CurrentUiLanguage), lines.Count); dialog.Close(); }
            catch (Exception ex) { Status.Text = "ذخیره متن ناموفق بود: " + ex.Message; }
        }; dialog.ShowDialog();
    }
    private async void TestApi_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            string baseUrl = EndpointInput.Text.Trim().TrimEnd('/');
            if (!Uri.TryCreate(baseUrl, UriKind.Absolute, out var uri) || (uri.Scheme != "https" && !(uri.Scheme == "http" && uri.IsLoopback))) { Status.Text = "نشانی معتبر HTTPS یا آدرس محلی وارد کنید."; return; }
            bool gemini = uri.Host.Equals("generativelanguage.googleapis.com", StringComparison.OrdinalIgnoreCase);
            using var req = new HttpRequestMessage(HttpMethod.Get, baseUrl + (gemini ? "/v1beta/models" : "/models"));
            if (!string.IsNullOrWhiteSpace(ApiKey.Password)) { if (gemini) req.Headers.Add("x-goog-api-key", ApiKey.Password); else req.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", ApiKey.Password); }
            Status.Text = "در حال بررسی اتصال...";
            var watch = System.Diagnostics.Stopwatch.StartNew(); using var response = await http.SendAsync(req); watch.Stop();
            if (!response.IsSuccessStatusCode) { Status.Text = string.Format(InterfaceText.Translate("خطای اتصال: HTTP {0}. کلید و نشانی را بررسی کنید.", CurrentUiLanguage), (int)response.StatusCode); return; }
            using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
            var property = gemini ? "models" : "data"; var field = gemini ? "name" : "id";
            var models = json.RootElement.TryGetProperty(property, out var list) && list.ValueKind == JsonValueKind.Array ? list.EnumerateArray().Select(x => GetString(x, field)).Where(x => x != null).Take(100).ToArray() : Array.Empty<string?>();
            Status.Text = string.Format(InterfaceText.Translate("اتصال موفق؛ {0} میلی‌ثانیه، {1} مدل.", CurrentUiLanguage), watch.ElapsedMilliseconds, models.Length);
            Log("Models: " + string.Join(", ", models));
            if (EndpointInput.Text.Trim().TrimEnd('/') == baseUrl && models.Length > 0)
            {
                string current = SelectedModel;
                SetModels(models.Where(x => !string.IsNullOrWhiteSpace(x)).Select(x => gemini && x!.StartsWith("models/") ? x[7..] : x!), current);
            }
        }
        catch (Exception ex) { Status.Text = "بررسی اتصال ناموفق بود: " + ex.Message; }
    }
    private void OffsetMinus_Click(object sender, RoutedEventArgs e) => AdjustOffset(-0.5);
    private void OffsetPlus_Click(object sender, RoutedEventArgs e) => AdjustOffset(0.5);

    private void AdjustOffset(double seconds)
    {
        offset += seconds;
        OffsetLabel.Text = $"{offset:+0.0;-0.0;0.0} s";
        overlay?.SetOffset(offset);
    }

    private void LyricsList_SelectionChanged(object sender, SelectionChangedEventArgs e) => UpdateOverlay();

    private void UpdateOverlay()
    {
        if (overlay == null) return;

        string title = TitleInput.Text.Trim();
        string artist = ArtistInput.Text.Trim();
        var line = string.Equals(loadedTrackKey, title + "\n" + artist, StringComparison.OrdinalIgnoreCase)
            ? LyricsList.SelectedItem as LyricLine : null;
        overlay.SetLyrics(line, title, artist);
    }

    private void UpdateOverlayPlayback(GlobalSystemMediaTransportControlsSessionPlaybackInfo? playback)
    {
        overlayPlayback = playback;
        overlay?.SetPlayback(
            playback?.PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing,
            playback?.Controls.IsPlayPauseToggleEnabled == true,
            playback?.Controls.IsPreviousEnabled == true,
            playback?.Controls.IsNextEnabled == true);
    }

    private void Overlay_Click(object sender, RoutedEventArgs e)
    {
        if (overlay != null)
        {
            overlay.Activate();
            return;
        }

        var floating = new FloatingLyricsWindow { Opacity = OverlayOpacity.Value };
        floating.Previous.Click += Previous_Click;
        floating.PlayPause.Click += PlayPause_Click;
        floating.Next.Click += Next_Click;
        floating.Delay.Click += OffsetMinus_Click;
        floating.Advance.Click += OffsetPlus_Click;
        floating.Closed += (_, _) => overlay = null;
        overlay = floating;
        floating.ApplyPalette((Brush)Resources["Panel"], (Brush)Resources["Accent"]);
        floating.SetInterfaceLanguage(CurrentUiLanguage);
        floating.SetOffset(offset);
        UpdateOverlayPlayback(overlayPlayback);
        UpdateOverlay();
        floating.Show();
    }
}


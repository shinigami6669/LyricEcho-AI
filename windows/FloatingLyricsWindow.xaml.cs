using System.Globalization;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Automation;
using System.Windows.Input;
using System.Windows.Media;

namespace LyricEcho.Windows;

public partial class FloatingLyricsWindow : Window
{
    private string interfaceLanguage = "fa";
    private List<(DependencyObject Element, DependencyProperty Property, string Original)> interfaceTexts = new();
    private LyricLine? currentLine;
    private string currentTitle = "";
    private string currentArtist = "";
    private bool isPlaying;
    public FloatingLyricsWindow()
    {
        InitializeComponent();
        interfaceTexts = InterfaceText.Capture(this);
        SetLyrics(null, "", "");
        SetOffset(0);
        SetPlayback(false, false, false, false);
    }

    public void ApplyPalette(Brush surface, Brush accent)
    {
        Resources["Panel"] = surface;
        Resources["Accent"] = accent;
    }

    public void SetInterfaceLanguage(string language)
    {
        interfaceLanguage = language;
        InterfaceText.Apply(interfaceTexts, language);
        SetLyrics(currentLine, currentTitle, currentArtist);
        string label = InterfaceText.Translate(isPlaying ? "مکث" : "پخش", interfaceLanguage);
        PlayPause.ToolTip = label;
        AutomationProperties.SetName(PlayPause, label);
    }

    public void SetLyrics(LyricLine? line, string title, string artist)
    {
        currentLine = line;
        currentTitle = title;
        currentArtist = artist;
        TrackTitle.Text = string.IsNullOrWhiteSpace(title) ? "LyricEcho"
            : string.IsNullOrWhiteSpace(artist) ? title : $"{title} — {artist}";
        TrackTitle.ToolTip = TrackTitle.Text;
        OriginalLine.Text = line?.Original ?? InterfaceText.Translate("در انتظار متن آهنگ", interfaceLanguage);
        OriginalLine.FontSize = line == null ? 17 : 22;
        OriginalLine.FlowDirection = TextDirection(OriginalLine.Text);
        TranslationLine.Text = line?.Translation ?? "";
        TranslationLine.FlowDirection = TextDirection(TranslationLine.Text);
        TranslationLine.Visibility = string.IsNullOrWhiteSpace(TranslationLine.Text)
            ? Visibility.Collapsed : Visibility.Visible;
    }

    private static FlowDirection TextDirection(string text)
    {
        foreach (char character in text)
        {
            if (!char.IsLetter(character)) continue;
            return character is >= '\u0590' and <= '\u08ff'
                ? FlowDirection.RightToLeft : FlowDirection.LeftToRight;
        }

        return FlowDirection.LeftToRight;
    }

    public void SetPlayback(bool playing, bool canToggle, bool canSkipPrevious, bool canSkipNext)
    {
        isPlaying = playing;
        PlayPause.Content = playing ? "Ⅱ" : "▶";
        string label = InterfaceText.Translate(playing ? "مکث" : "پخش", interfaceLanguage);
        PlayPause.ToolTip = label;
        AutomationProperties.SetName(PlayPause, label);
        PlayPause.IsEnabled = canToggle;
        Previous.IsEnabled = canSkipPrevious;
        Next.IsEnabled = canSkipNext;
    }

    public void SetOffset(double seconds)
    {
        OffsetText.Text = seconds.ToString("+0.0;-0.0;0.0", CultureInfo.InvariantCulture) + " s";
    }

    private void DragHeader(object sender, MouseButtonEventArgs e)
    {
        if (e.LeftButton == MouseButtonState.Pressed) DragMove();
    }

    private void ToggleCompact(object sender, RoutedEventArgs e)
    {
        bool compact = Controls.Visibility == Visibility.Visible;
        Controls.Visibility = compact ? Visibility.Collapsed : Visibility.Visible;
        CompactButton.Content = compact ? "+" : "−";
        string label = InterfaceText.Translate(compact ? "نمایش کنترل‌ها" : "حالت جمع‌وجور", interfaceLanguage);
        CompactButton.ToolTip = label;
        AutomationProperties.SetName(CompactButton, label);
    }

    private void CloseOverlay(object sender, RoutedEventArgs e) => Close();

    private void HandleKey(object sender, KeyEventArgs e)
    {
        if (e.Key != Key.Escape) return;
        e.Handled = true;
        Close();
    }
}

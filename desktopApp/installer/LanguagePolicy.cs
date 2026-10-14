using System;
using System.Globalization;
using System.Runtime.InteropServices;

public static class LanguagePolicy {
    public static readonly string[] Languages={"en","ru","be","fr","de","es","it","zh","ja","ko"};
    [DllImport("kernel32.dll")] private static extern ushort GetUserDefaultUILanguage();
    public static string Resolve(string value) {
        string code=(value ?? "").Replace('_','-').Split('-')[0].ToLowerInvariant();
        return Array.IndexOf(Languages,code)>=0 ? code : "en";
    }
    public static string SystemLanguage() {
        try {return Resolve(CultureInfo.GetCultureInfo(GetUserDefaultUILanguage()).Name);}
        catch {return "en";}
    }
}

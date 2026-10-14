using System;
using System.IO;
using System.Reflection;
using System.Diagnostics;
using System.Windows.Forms;
using System.Collections.Generic;
using System.Web.Script.Serialization;

public static class Bootstrapper {
    [STAThread] public static int Main(string[] args) {
        string temporary=null;bool quiet=Array.Exists(args,a=>a.Equals("/quiet",StringComparison.OrdinalIgnoreCase)||a.Equals("/qn",StringComparison.OrdinalIgnoreCase));string language=LanguagePolicy.SystemLanguage();
        try {
            string extra="",lang=null;bool passive=false,noRestart=false;
            foreach(string arg in args) {
                string option=arg.ToLowerInvariant();
                if(option=="/quiet" || option=="/qn")quiet=true;
                else if(option=="/passive")passive=true;
                else if(option=="/norestart")noRestart=true;
                else if(arg.StartsWith("/lang=",StringComparison.OrdinalIgnoreCase)){lang=LanguagePolicy.Resolve(arg.Substring(6));language=lang;}
                else if(option=="/help" || option=="/?") {if(!quiet)MessageBox.Show("FitEasy: /quiet /passive /norestart /lang=en|ru|be|fr|de|es|it|zh|ja|ko","FitEasy");return 0;}
                else throw new ArgumentException();
            }
            extra=(quiet?" /qn":passive?" /passive":"")+(noRestart?" /norestart":"");
            temporary=Path.Combine(Path.GetTempPath(),"FitEasy-setup-"+Guid.NewGuid().ToString("N"));Directory.CreateDirectory(temporary);
            string msi=Path.Combine(temporary,"FitEasy.msi");
            using(var input=Assembly.GetExecutingAssembly().GetManifestResourceStream("FitEasy.msi"))
            using(var output=File.Create(msi))input.CopyTo(output);
            string command="/i \""+msi+"\""+extra+(lang==null ? "" : " FITEASYLANGUAGE="+lang);
            using(var process=Process.Start(new ProcessStartInfo(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System),"msiexec.exe"),command) {UseShellExecute=true})) {
                process.WaitForExit();return process.ExitCode;
            }
        } catch(Exception) {
            if(!quiet) {
                string message="Unable to open the installer. Download it again or use the MSI package.";
                try {using(var stream=Assembly.GetExecutingAssembly().GetManifestResourceStream("languages.json"))using(var reader=new StreamReader(stream)) {
                    var rows=new JavaScriptSerializer().Deserialize<Dictionary<string,Dictionary<string,string>>>(reader.ReadToEnd());message=rows[language]["MSG_FAILURE"];
                }}catch {}
                MessageBox.Show(message,"FitEasy",MessageBoxButtons.OK,MessageBoxIcon.Error);
            }
            return 1603;
        }
        finally {if(temporary!=null)try {Directory.Delete(temporary,true);}catch {}}
    }
}

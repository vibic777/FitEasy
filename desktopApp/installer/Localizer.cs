using System;
using System.IO;
using System.Reflection;
using System.Collections.Generic;
using System.Web.Script.Serialization;
using System.Windows.Forms;
using System.Diagnostics;
using System.Net.Sockets;
using System.Text;
using Microsoft.Win32;
using Microsoft.Deployment.WindowsInstaller;

public static class Localizer {
    public static bool IsDataTarget(string target,string local) {
        try {return String.Equals(Path.GetFullPath(target).TrimEnd(Path.DirectorySeparatorChar),Path.GetFullPath(Path.Combine(local,"FitEasy")).TrimEnd(Path.DirectorySeparatorChar),StringComparison.OrdinalIgnoreCase);}catch {return false;}
    }
    private static void CheckDataTree(string path) {
        if((File.GetAttributes(path)&System.IO.FileAttributes.ReparsePoint)!=0)throw new IOException("Reparse point");
        if(Directory.Exists(path))foreach(string item in Directory.GetFileSystemEntries(path))CheckDataTree(item);
        else using(File.Open(path,FileMode.Open,FileAccess.ReadWrite,FileShare.None)){}
    }
    [CustomAction] public static ActionResult AskDeleteData(Session session) {
        try {
            if(session["FITEASYDELETEDATA"]!="" && session["FITEASYDELETEDATA"]!="0" && session["FITEASYDELETEDATA"]!="1")return ActionResult.Failure;
            int level;Int32.TryParse(session["UILevel"],out level);
            if(level<=2)return ActionResult.Success;
            var messages=ReadTranslations()[String.IsNullOrEmpty(session["FITEASYLANGUAGE"]) ? LanguagePolicy.SystemLanguage() : LanguagePolicy.Resolve(session["FITEASYLANGUAGE"])];
            using(var record=new Record(0)) {
                record.FormatString=messages["MSG_DELETE_DATA"];
                var answer=session.Message((InstallMessage)((int)InstallMessage.User|0x20|0x3|0x100),record);
                if(answer==MessageResult.Cancel)return ActionResult.UserExit;
                session["FITEASYDELETEDATA"]=answer==MessageResult.Yes ? "1" : "0";
            }
            return CloseRunning(session);
        }catch(Exception ex){session.Log("FitEasy removal choice failed: "+ex.GetType().Name);return ActionResult.Failure;}
    }
    [CustomAction] public static ActionResult DeleteData(Session session) {
        try {
            string path=session["FITEASYDELETEPATH"];
            string local=Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            if(!IsDataTarget(path,local))throw new IOException("Invalid data target");
            if(Directory.Exists(path)){CheckDataTree(path);Directory.Delete(path,true);}
            session.Log("FitEasy current-user data removed");
        }catch(Exception ex){session.Log("FitEasy data cleanup was not completed: "+ex.GetType().Name);return ActionResult.Failure;}
        return ActionResult.Success;
    }
    public static Dictionary<string,Dictionary<string,string>> ReadTranslations() {
        using(var stream=Assembly.GetExecutingAssembly().GetManifestResourceStream("languages.json"))
        using(var reader=new StreamReader(stream,Encoding.UTF8))
            return new JavaScriptSerializer().Deserialize<Dictionary<string,Dictionary<string,string>>>(reader.ReadToEnd());
    }
    [CustomAction] public static ActionResult SelectLanguage(Session session) {
        try {
            string requested=session["FITEASYLANGUAGE"];
            string lang=String.IsNullOrEmpty(requested) ? LanguagePolicy.SystemLanguage() : LanguagePolicy.Resolve(requested);
            session["FITEASYLANGUAGE"]=lang;
            foreach(var pair in ReadTranslations()[lang]) {
                if(!pair.Key.StartsWith("MSG_")) session[pair.Key]=pair.Value.Replace("[ProductName]",session["ProductName"]);
            }
            session.Log("FitEasy installer language: "+lang);
            return ActionResult.Success;
        } catch(Exception ex) {session.Log("FitEasy language initialization failed: "+ex.GetType().Name);return ActionResult.Failure;}
    }
    private static List<string> InstalledPaths() {
        var result=new List<string>();
        foreach(var hive in new[]{RegistryHive.CurrentUser,RegistryHive.LocalMachine})
        foreach(var view in new[]{RegistryView.Registry64,RegistryView.Registry32}) {
            using(var root=RegistryKey.OpenBaseKey(hive,view))
            using(var uninstall=root.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Uninstall")) {
                if(uninstall==null)continue;
                foreach(string key in uninstall.GetSubKeyNames()) using(var item=uninstall.OpenSubKey(key)) {
                    if(item!=null && (string)item.GetValue("DisplayName")=="FitEasy" && (string)item.GetValue("Publisher")=="Vibic777") {
                        string location=item.GetValue("InstallLocation") as string;
                        if(!String.IsNullOrEmpty(location))result.Add(Path.GetFullPath(Path.Combine(location,"FitEasy.exe")));
                    }
                }
            }
        }
        return result;
    }
    private static List<Process> Running(List<string> paths) {
        var result=new List<Process>();
        foreach(var process in Process.GetProcessesByName("FitEasy")) {
            try {
                string path=Path.GetFullPath(process.MainModule.FileName);
                if(paths.Exists(p=>String.Equals(p,path,StringComparison.OrdinalIgnoreCase)))result.Add(process);
                else process.Dispose();
            } catch {process.Dispose();}
        }
        return result;
    }
    [CustomAction] public static ActionResult CloseRunning(Session session) {
        try {
            var paths=InstalledPaths();
            string lang=String.IsNullOrEmpty(session["FITEASYLANGUAGE"]) ? LanguagePolicy.SystemLanguage() : LanguagePolicy.Resolve(session["FITEASYLANGUAGE"]);
            var messages=ReadTranslations()[lang];
            using(var owner=new Form {TopMost=true}) {
                while(true) {
                    var active=Running(paths);bool present=active.Count>0;active.ForEach(p=>p.Dispose());
                    if(!present)return ActionResult.Success;
                    var answer=MessageBox.Show(owner,messages["MSG_CLOSE"],"FitEasy",MessageBoxButtons.YesNoCancel,MessageBoxIcon.Question);
                    if(answer==DialogResult.Cancel)return ActionResult.UserExit;
                    if(answer==DialogResult.Yes) {
                        try {
                            var lines=File.ReadAllLines(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),@"FitEasy\instance.port"));
                            int port;if(lines.Length==2 && Int32.TryParse(lines[0],out port) && port>0 && port<=65535) {
                                using(var client=new TcpClient()) {
                                    var connect=client.BeginConnect("127.0.0.1",port,null,null);
                                    if(!connect.AsyncWaitHandle.WaitOne(3000))throw new TimeoutException();client.EndConnect(connect);
                                    client.ReceiveTimeout=3000;client.SendTimeout=3000;
                                    var stream=client.GetStream();var bytes=Encoding.ASCII.GetBytes("quit\n"+lines[1]+"\n");stream.Write(bytes,0,bytes.Length);
                                    using(var reader=new StreamReader(stream))if(reader.ReadLine()=="ok") {
                                        var waiting=Running(paths);foreach(var p in waiting) {try {p.WaitForExit(15000);}finally {p.Dispose();}}
                                    }
                                }
                            }
                        }catch {}
                    }
                    active=Running(paths);present=active.Count>0;active.ForEach(p=>p.Dispose());
                    if(present && MessageBox.Show(owner,messages["MSG_RETRY"],"FitEasy",MessageBoxButtons.RetryCancel,MessageBoxIcon.Information)==DialogResult.Cancel)return ActionResult.UserExit;
                }
            }
        } catch(Exception ex) {session.Log("FitEasy running-app check failed: "+ex.GetType().Name);return ActionResult.Failure;}
    }
}

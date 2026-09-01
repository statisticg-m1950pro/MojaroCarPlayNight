package com.monjaro.carplaynightv2;

import android.content.Context;
import java.io.*;
import java.util.*;

public final class RootShell {
    public static class Result {
        public int code;
        public String out="";
        public String err="";
    }

    public static boolean hasRoot() {
        Result r=exec("id");
        return r.code==0 && r.out.contains("uid=0");
    }

    public static Result exec(String cmd) {
        Result r=new Result();
        Process p=null;
        try {
            p=new ProcessBuilder("su","-c",cmd).start();
            BufferedReader o=new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader e=new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuilder so=new StringBuilder(), se=new StringBuilder();
            String s;
            while((s=o.readLine())!=null) so.append(s).append('\n');
            while((s=e.readLine())!=null) se.append(s).append('\n');
            r.code=p.waitFor();
            r.out=so.toString();
            r.err=se.toString();
        } catch(Throwable t) {
            r.code=-1; r.err=t.toString();
        } finally {
            if(p!=null) p.destroy();
        }
        return r;
    }

    public static boolean installDaemon(Context c) {
        String script=
"#!/system/bin/sh\n"+
"STATE=/data/local/tmp/monjaro_carplay_state\n"+
"LOG=/data/local/tmp/monjaro_carplay_root.log\n"+
"LAST=''\n"+
"echo root-daemon-start > $LOG\n"+
"while true; do\n"+
"  V=$(settings get system realThemeDaynightMode 2>/dev/null)\n"+
"  if [ \"$V\" != \"$LAST\" ] && [ -n \"$V\" ] && [ \"$V\" != \"null\" ]; then\n"+
"    LAST=$V\n"+
"    if [ \"$V\" = \"1\" ]; then N=1; else N=0; fi\n"+
"    echo $N > $STATE\n"+
"    echo \"$(date +%s) mode=$V night=$N\" >> $LOG\n"+
"    # Root hook candidates. Harmless if files/actions do not exist.\n"+
"    echo $N > /data/local/tmp/night_mode\n"+
"    echo $N > /tmp/night_mode 2>/dev/null\n"+
"  fi\n"+
"  sleep 1\n"+
"done\n";
        File f=new File(c.getCacheDir(),"monjaro_rootd.sh");
        try(FileOutputStream fos=new FileOutputStream(f)) {
            fos.write(script.getBytes("UTF-8"));
        } catch(Exception ex) { return false; }

        Result a=exec("mkdir -p /data/local/tmp && cp '"+f.getAbsolutePath()+"' /data/local/tmp/monjaro_rootd.sh && chmod 755 /data/local/tmp/monjaro_rootd.sh");
        if(a.code!=0) return false;
        Result b=exec("pkill -f monjaro_rootd.sh 2>/dev/null; nohup /system/bin/sh /data/local/tmp/monjaro_rootd.sh >/dev/null 2>&1 &");
        return b.code==0;
    }

    public static Result diagnostics() {
        String cmd=
"echo '=== ROOT ==='; id; "+
"echo '=== AUTOKIT PACKAGES ==='; pm list packages | grep -i autokit; "+
"echo '=== RUNNING ==='; ps -A | grep -i -E 'autokit|carlink|carplay'; "+
"echo '=== NIGHT FILES ==='; ls -l /tmp/night_mode /data/local/tmp/night_mode /data/local/tmp/monjaro_carplay_state 2>&1; "+
"echo '=== THEME ==='; settings get system realThemeDaynightMode; "+
"echo '=== USB ==='; ls /dev/bus/usb/*/* 2>/dev/null | head -20; ";
        return exec(cmd);
    }
}

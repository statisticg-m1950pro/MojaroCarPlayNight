package com.monjaro.carplaynightv2;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.widget.*;

public class MainActivity extends Activity {
    private TextView status, log;
    private BroadcastReceiver statusReceiver;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        status=findViewById(R.id.status);
        log=findViewById(R.id.log);

        Switch enabled=findViewById(R.id.enabled);
        Switch invert=findViewById(R.id.invert);
        enabled.setChecked(Prefs.enabled(this));
        invert.setChecked(Prefs.invert(this));

        enabled.setOnCheckedChangeListener((x,v)->{
            Prefs.setEnabled(this,v);
            startService(new Intent(this,ThemeWatchService.class));
            update();
        });
        invert.setOnCheckedChangeListener((x,v)->{
            Prefs.setInvert(this,v);
            startService(new Intent(this,ThemeWatchService.class));
            update();
        });

        findViewById(R.id.root).setOnClickListener(v->{
            boolean ok=RootShell.installDaemon(this);
            Toast.makeText(this, ok?"ROOT-движок запущен":"Не удалось запустить ROOT-движок", Toast.LENGTH_LONG).show();
            update();
        });

        findViewById(R.id.requestUsb).setOnClickListener(v->{UsbNightController.requestPermission(this);update();});
        findViewById(R.id.testNight).setOnClickListener(v->{UsbNightController.setNightMode(this,true);update();});
        findViewById(R.id.testDay).setOnClickListener(v->{UsbNightController.setNightMode(this,false);update();});

        findViewById(R.id.diagnostics).setOnClickListener(v->{
            RootShell.Result r=RootShell.diagnostics();
            log.setText(r.out+(r.err.isEmpty()?"":"\nERR:\n"+r.err));
        });

        statusReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){update();}};
        registerReceiver(statusReceiver,new IntentFilter("com.monjaro.carplaynightv2.STATUS"));
        startService(new Intent(this,ThemeWatchService.class));
        update();
    }

    @Override protected void onDestroy(){
        try{unregisterReceiver(statusReceiver);}catch(Exception ignored){}
        super.onDestroy();
    }
    @Override protected void onResume(){super.onResume();update();}

    private void update(){
        int raw=Settings.System.getInt(getContentResolver(),"realThemeDaynightMode",-1);
        boolean night=raw==1;
        if(Prefs.invert(this)) night=!night;
        status.setText(
            "realThemeDaynightMode = "+raw+
            "\nGMC: "+(night?"НОЧЬ":"ДЕНЬ")+
            "\nROOT: "+(RootShell.hasRoot()?"ДОСТУП ЕСТЬ":"НЕТ ДОСТУПА")+
            "\nROOT daemon: /data/local/tmp/monjaro_rootd.sh"+
            "\nUSB: "+UsbNightController.getLastStatus());
    }
}

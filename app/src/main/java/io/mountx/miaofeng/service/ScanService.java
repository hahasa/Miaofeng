package io.mountx.miaofeng.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import io.mountx.miaofeng.sql.MyDB;
import io.mountx.miaofeng.ui.FileActivity;

public class ScanService extends Service {

    private boolean activityisrunning;
    private Handler scanhandler;
    private ScanThread scanthread;
    private boolean isscanned;
    private MyDB musicFileDB;
    private SharedPreferences scanpreferences;
    private FileActivity.ScanOver scanover;
    private boolean threadisactive = false;

    @Override
    public IBinder onBind(Intent intent) {

        return new MyBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        scanpreferences = this.getSharedPreferences("prefile", MODE_PRIVATE);
        isscanned = scanpreferences.getBoolean("isscanned", false);

        musicFileDB = new MyDB(this);
        musicFileDB.openOrCreate("music.db");

        scanhandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1: {
                        scanhandler.removeCallbacks(scanthread);
                        threadisactive = false;
                        Editor scaneditor = scanpreferences.edit();
                        scaneditor.putBoolean("isscanning", false);
                        scaneditor.commit();
                        if (activityisrunning)
                            scanover.doScanOver();
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        };

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {

        musicFileDB.close();
        stopThread();
        super.onDestroy();
    }

    //退出时关闭子线程
    public void stopThread() {
        if (threadisactive) {
            scanhandler.removeCallbacks(scanthread);
            Editor scaneditor = scanpreferences.edit();
            scaneditor.putBoolean("exception", true);
            scaneditor.putBoolean("isscanning", false);
            scaneditor.commit();
        }
    }

    public class MyBinder extends Binder {
        public void isNotRunning() {
            activityisrunning = false;
        }

        public void isRunning() {
            activityisrunning = true;
        }

        public void setScanOver(FileActivity.ScanOver _scanover) {
            scanover = _scanover;
        }

        public void startScan() {
            if (!threadisactive) {
                Editor scaneditor = scanpreferences.edit();
                scaneditor.putBoolean("isscanning", true);
                scaneditor.commit();
                scanthread = new ScanThread(ScanService.this, scanhandler, isscanned, musicFileDB, scanpreferences);
                scanhandler.post(scanthread);
                threadisactive = true;
            }
        }
    }
}

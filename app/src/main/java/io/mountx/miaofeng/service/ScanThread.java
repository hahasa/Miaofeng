package io.mountx.miaofeng.service;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import io.mountx.miaofeng.sql.LoadSongs;
import io.mountx.miaofeng.sql.MyDB;

public class ScanThread extends Thread {

    //数据声明
    private boolean isscanned = true;
    private MyDB musicFileDB;
    private ContentResolver contentresolver;
    private SharedPreferences scanpreferences;
    private Context context;
    private Handler scannedhandler;


    private String sd_path;

    //构造方法
    public ScanThread(Context context, Handler scannedhandler, boolean isscanned, MyDB musicFileDB,
                      SharedPreferences scanpreferences) {
        this.isscanned = isscanned;
        this.musicFileDB = musicFileDB;
        this.scanpreferences = scanpreferences;
        this.context = context;
        this.scannedhandler = scannedhandler;
        contentresolver = context.getContentResolver();
    }

    @Override
    public void run() {
        if (isscanned) {
            Cursor tablecursor = musicFileDB.getAllTable();
            if (null != tablecursor && tablecursor.getCount() > 0) {

                for (tablecursor.moveToFirst(); !tablecursor.isAfterLast(); tablecursor.moveToNext()) {
                    int tableColumnIndex = tablecursor.getColumnIndex("file_table");
                    String tablename = tablecursor.getString(tableColumnIndex);
                    musicFileDB.deleteAll(tablename);
                }
            }
            musicFileDB.deleteAll("tablefile");
        }
        //扫描SD卡更新系统提供的数据库
        scanSD();
        //扫描完成后会调用refreshMyDB()方法更新程序的数据库

    }

    public void refreshMyDB() {
        LoadSongs loadsongs = new LoadSongs(contentresolver, musicFileDB);
        loadsongs.loadFiles();
        Editor scaneditor = scanpreferences.edit();
        scaneditor.putBoolean("isscanned", true);
        scaneditor.commit();
        Message message = new Message();
        message.what = 1;
        scannedhandler.sendMessage(message);


    }

    //扫描SD卡
    public void scanSD() {
        sd_path = Environment.getExternalStorageDirectory().getPath();
        new MediaScannerNotifier(context, sd_path);


    }

    private class MediaScannerNotifier implements MediaScannerConnectionClient {
        private MediaScannerConnection mconnection;
        private String sd_path;

        public MediaScannerNotifier(Context context, String path) {
            this.sd_path = path;
            mconnection = new MediaScannerConnection(context, this);
            mconnection.connect();
        }

        public void onMediaScannerConnected() {

            mconnection.scanFile(sd_path, null);
        }


        public void onScanCompleted(String path, Uri uri) {

            mconnection.disconnect();
            refreshMyDB();
        }

    }
}

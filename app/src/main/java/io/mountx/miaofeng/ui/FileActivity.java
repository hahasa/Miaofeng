package io.mountx.miaofeng.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import io.mountx.miaofeng.R;
import io.mountx.miaofeng.service.ScanService;
import io.mountx.miaofeng.sql.MyDB;

//程序的文件夹界面的Activity
public class FileActivity extends Activity {

    //数据声明
    public MyDB musicFileDB = null;
    private ListView explist;
    private ActionBar actionbar_file;
    private int filecount;
    private boolean isscanned;
    private boolean _isscanned;
    private SharedPreferences scanpreferences;
    private boolean isscanning;
    private ScanService.MyBinder binder = null;
    private Intent serviceintent;
    private boolean serviceisactive = false;
    private boolean scanexception = false;

    //onCreate方法
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        //ListView的监听
        explist = (ListView) findViewById(R.id.explist);
        explist.setOnItemClickListener(mylistener);

        serviceintent = new Intent();
        // serviceintent.setAction("com.freecoder.ultraaudio.service.ScanService");

        serviceintent.setClass(this, ScanService.class);
        actionbar_file = this.getActionBar();
        actionbar_file.setDisplayHomeAsUpEnabled(true);
        actionbar_file.setDisplayShowHomeEnabled(false);

        musicFileDB = new MyDB(this);
        musicFileDB.openOrCreate("music.db");

        scanpreferences = this.getSharedPreferences("prefile", MODE_PRIVATE);
        isscanned = scanpreferences.getBoolean("isscanned", false);
        _isscanned = scanpreferences.getBoolean("_isscanned", false);
        isscanning = scanpreferences.getBoolean("isscanning", false);
        scanexception = scanpreferences.getBoolean("exception", false);

        if (isscanned) {


            if (scanexception) {
                actionbar_file.setTitle(getResources().getString(R.string.anothorscan));

            } else {
                if (isscanning) {
                    actionbar_file.setTitle(getResources().getString(R.string.isscanning));
                    bindService(serviceintent, conn, Context.BIND_AUTO_CREATE);
                } else {
                    readDB();
                }
            }
        } else {
            if (_isscanned) {
                actionbar_file.setTitle(getResources().getString(R.string.isscanning));
                bindService(serviceintent, conn, Context.BIND_AUTO_CREATE);
            } else
                actionbar_file.setTitle(getResources().getString(R.string.scanfirstly));
        }
    }

    //ListView的监听
    private ListView.OnItemClickListener mylistener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent();
            intent.setClass(FileActivity.this, SongActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("position", position + 1);
            intent.putExtras(bundle);
            startActivity(intent);
            overridePendingTransition(R.animator.right_left, android.R.anim.fade_out);
        }
    };

    //加载ActionBar
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_file_menu, menu);
        return true;
    }

    //ActionBar的监听
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home: {
                sendInfo();
                overridePendingTransition(R.animator.left_right, 0);
                break;
            }
            case R.id.activity_file_menu_scan: {
                if (!_isscanned) {
                    Editor scaneditor = scanpreferences.edit();
                    scaneditor.putBoolean("_isscanned", true);
                    scaneditor.commit();
                }
                if (scanexception) {
                    Editor scaneditor = scanpreferences.edit();
                    scaneditor.putBoolean("exception", false);
                    scaneditor.commit();
                }
                if (!isscanning) {
                    actionbar_file.setTitle(getResources().getString(R.string.isscanning));
                    listDisable(explist);
                    isscanning = true;
                    startService(serviceintent);
                    bindService(serviceintent, conn, Context.BIND_AUTO_CREATE);
                    break;
                }
            }
        }
        return true;
    }

    //扫描完成后service调用activity的方法
    public class ScanOver {
        public ScanOver() {

        }

        public void doScanOver() {
            unbindService(conn);
            stopService(serviceintent);
            isscanning = false;
            serviceisactive = false;
            readDB();
        }
    }


    //绑定扫描服务后，开始扫描子线程，告诉扫描服务activity处在激活状态
    private ServiceConnection conn = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {

        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            ScanOver scanover = new ScanOver();
            binder = (ScanService.MyBinder) service;
            binder.startScan();
            serviceisactive = true;
            binder.setScanOver(scanover);
            binder.isRunning();
        }
    };

    //读取数据库
    @SuppressWarnings("deprecation")
    public void readDB() {
        Cursor cursor = musicFileDB.getAllFile("tablefile");
        if (cursor != null && cursor.getCount() >= 0) {
            filecount = cursor.getCount();
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.activity_file_list,
                    cursor, new String[]{"file_name", "file_path", "file_count"},
                    new int[]{R.id.txtfilename, R.id.txtfilepath, R.id.txtfilecount});
            actionbar_file.setTitle(filecount + getResources().getString(R.string.countfile));
            explist.setAdapter(adapter);
            listEnable(explist);
        }
    }

    //使list使能
    private void listEnable(ListView list) {
        list.setEnabled(true);
        list.setAlpha(1);
    }

    //使list无效
    private void listDisable(ListView list) {
        list.setEnabled(false);
        list.setAlpha(0.5f);
    }

    //向MainActvity传递数据，用来判断ScanService是否在运行
    public void sendInfo() {
        Intent intent = new Intent();
        intent.putExtra("scanservice", serviceisactive);
        setResult(RESULT_OK, intent);
        finish();
    }


    /*关闭数据库
     * 如果还在扫描，解绑扫描服务
     */
    @Override
    protected void onDestroy() {

        musicFileDB.close();
        if (isscanning) {

            binder.isNotRunning();
            unbindService(conn);

        } else {
            stopService(serviceintent);
        }
        super.onDestroy();

    }

    //点击返回键，向MainActivity传递数据，结束本身的Activity
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK)
            sendInfo();
        return true;
    }
}
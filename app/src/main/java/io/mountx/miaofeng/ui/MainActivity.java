package io.mountx.miaofeng.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import io.mountx.miaofeng.R;
import io.mountx.miaofeng.service.MusicService;
import io.mountx.miaofeng.sql.MyDB;

public class MainActivity extends Activity {

    //数据声明
    private ActionBar actionbar_main;
    private TabHost tabhost;
    private Intent intent;
    private Intent serviceintent;
    private MyDB playlistDB;
    private ListView music_list;
    private Cursor playlistcursor;
    private MusicService.MyBinder binder = null;
    private SeekBar seekbar;
    private String playlist_name;
    private int playlist_count;
    private int playlist_num;
    private int position_num;
    private String playlist_path;
    private TextView txtnum, txtfilename;
    private boolean isplaying = false;
    private boolean isplaying_pause = false;
    private boolean scanservice = false;
    private boolean playservice = false;
    private boolean prev_next = false;
    private SharedPreferences sharedpreferences;
    private ImageButton btnprev;
    private ImageButton btnplay;
    private ImageButton btnnext;
    private ImageButton btnmode;
    private int playmode = 0;
    private long mExitTime = 0;
    private GestureDetector mygesture;
    private static final int ACTIVITY_EDIT = 1;
    //调节音量
    private int volume;
    private AudioManager audiomanager;
    //耳机监听
    private MyBroadcastReceiver headsetPlugReceiver;
    //来电监听
    private TelephoneyBroadcastReceiver telephoneyRecevier;

    private OnMusicOver over;

    //onCreate方法
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        actionbar_main = this.getActionBar();
        actionbar_main.setDisplayShowHomeEnabled(false);

        seekbar = (SeekBar) findViewById(R.id.seekbar);
        txtnum = (TextView) findViewById(R.id.txt_num);
        txtfilename = (TextView) findViewById(R.id.txtfilename);
        music_list = (ListView) findViewById(R.id.music_list);
        music_list.setOnItemClickListener(clickListener);
        music_list.setOnItemLongClickListener(longClickListener);
        playlistDB = new MyDB(this);
        playlistDB.openOrCreate("playlist.db");
        playlistDB.createPlaylistTable("playlist");
        //ScanService通知MainActivity是否在扫描
        sharedpreferences = this.getSharedPreferences("prefile", MODE_PRIVATE);

        btnplay = (ImageButton) findViewById(R.id.btnplay);
        btnprev = (ImageButton) findViewById(R.id.btnprev);
        btnnext = (ImageButton) findViewById(R.id.btnnext);
        btnmode = (ImageButton) findViewById(R.id.btnmode);
        btnplay.setOnClickListener(mylistener);
        btnprev.setOnClickListener(mylistener);
        btnnext.setOnClickListener(mylistener);
        btnmode.setOnClickListener(mylistener);

        mygesture = new GestureDetector(gestureListener);
        tabhost = (TabHost) findViewById(R.id.tabhost);

        audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //动态注册耳机和来电的广播
        registerHeadsetPlugReceiver();

        over = new OnMusicOver();
        //初始化Main界面
        initView();
    }

    //播放暂停的监听
    private ImageButton.OnClickListener mylistener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnplay: {
                    if (isplaying) {
                        if (isplaying_pause) {
                            binder.dostart();
                            isplaying_pause = false;
                            btnplay.setImageDrawable(getResources().getDrawable(R.drawable.music_pause));
                        } else {
                            binder.dopause();
                            isplaying_pause = true;
                            btnplay.setImageDrawable(getResources().getDrawable(R.drawable.music_play));
                        }
                    }
                    break;
                }

                case R.id.btnmode: {
                    switch (playmode) {
                        case 0: {
                            playmode = 1;
                            btnmode.setImageDrawable(getResources().getDrawable(R.drawable.music_all));
                            break;
                        }
                        case 1: {
                            playmode = 2;
                            btnmode.setImageDrawable(getResources().getDrawable(R.drawable.music_one));
                            break;
                        }
                        case 2: {
                            playmode = 0;
                            btnmode.setImageDrawable(getResources().getDrawable(R.drawable.music_loop));
                            break;
                        }
                        default: {
                            playmode = 0;
                            btnmode.setImageDrawable(getResources().getDrawable(R.drawable.music_loop));
                            break;
                        }
                    }
                    break;
                }

                case R.id.btnnext: {
                    if (prev_next) {
                        if (playmode != 2) {
                            OnMusicOver over = new OnMusicOver();
                            over.onMusicOver();
                        } else {
                            if (!playlistcursor.isLast()) {
                                playlistcursor.moveToNext();
                                playlist_num = playlistcursor.getPosition() + 1;
                                //获得播放歌曲名称
                                int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
                                playlist_name = playlistcursor.getString(musicColumnIndex);
                                //获得播放歌曲的路径
                                musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
                                playlist_path = playlistcursor.getString(musicColumnIndex);
                            } else {
                                playlistcursor.moveToFirst();
                                playlist_num = 1;
                                //获得播放歌曲名称
                                int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
                                playlist_name = playlistcursor.getString(musicColumnIndex);
                                //获得播放歌曲的路径
                                musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
                                playlist_path = playlistcursor.getString(musicColumnIndex);
                            }
                            if (binder != null)
                                unbindMusicService();
                            bindMusicService();
                        }
                    }
                    break;
                }

                case R.id.btnprev: {
                    if (prev_next) {
                        OnMusicOver over = new OnMusicOver();
                        over.onMusicPrev();
                    }
                    break;
                }

            }

        }
    };
    //播放列表Click监听
    private ListView.OnItemClickListener clickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            playlist_num = position + 1;
            //播放歌曲
            playlistcursor.moveToPosition(position);
            //获得播放歌曲名称
            int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
            playlist_name = playlistcursor.getString(musicColumnIndex);
            //获得播放歌曲的路径
            musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
            playlist_path = playlistcursor.getString(musicColumnIndex);
            if (binder == null) {
                bindMusicService();
            } else {
                if (binder != null)
                    unbindMusicService();
                bindMusicService();

            }
        }
    };

    //绑定音乐播放服务
    private void bindMusicService() {

        serviceintent = new Intent();
        //  serviceintent.setAction("com.freecoder.ultraaudio.service.MusicService");
        serviceintent.setClass(this, MusicService.class);
        serviceintent.putExtra("path", playlist_path);
        startService(serviceintent);
        bindService(serviceintent, conn, Context.BIND_AUTO_CREATE);

    }

    //解绑音乐播放服务
    private void unbindMusicService() {
        binder.dostop();
        binder.doremovethread();
        binder = null;
        unbindService(conn);
        stopService(serviceintent);
        isplaying = false;
        playservice = false;
    }

    //歌曲播放完毕的类
    public class OnMusicOver {
        //文件不存在的数量
        public int fileCount = 0;

        //构造方法
        public OnMusicOver() {

        }

        //一首歌曲播放完毕后的处理，也被下一首按钮调用
        public void onMusicOver() {
            switch (playmode) {
                case 0: {
                    if (!playlistcursor.isLast()) {
                        playlistcursor.moveToNext();
                        playlist_num = playlistcursor.getPosition() + 1;
                        //获得播放歌曲名称
                        int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
                        playlist_name = playlistcursor.getString(musicColumnIndex);
                        //获得播放歌曲的路径
                        musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
                        playlist_path = playlistcursor.getString(musicColumnIndex);
                    } else {
                        playlistcursor.moveToFirst();
                        playlist_num = 1;
                        //获得播放歌曲名称
                        int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
                        playlist_name = playlistcursor.getString(musicColumnIndex);
                        //获得播放歌曲的路径
                        musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
                        playlist_path = playlistcursor.getString(musicColumnIndex);
                    }
                    if (binder != null)
                        unbindMusicService();
                    bindMusicService();
                    break;
                }
                case 1: {
                    if (!playlistcursor.isLast()) {
                        playlistcursor.moveToNext();
                        playlist_num = playlistcursor.getPosition() + 1;
                        //获得播放歌曲名称
                        int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
                        playlist_name = playlistcursor.getString(musicColumnIndex);
                        //获得播放歌曲的路径
                        musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
                        playlist_path = playlistcursor.getString(musicColumnIndex);
                        if (binder != null)
                            unbindMusicService();
                        bindMusicService();
                    } else {
                        if (binder != null)
                            unbindMusicService();
                        txtnum.setText("");
                        txtfilename.setText("");
                        seekbar.setProgress(0);
                        prev_next = false;
                    }
                    break;
                }
                case 2: {
                    if (binder != null)
                        unbindMusicService();
                    bindMusicService();
                    break;
                }
            }
        }

        //上一首按钮的处理方法
        public void onMusicPrev() {
            switch (playmode) {
                case 0: {
                    if (!playlistcursor.isFirst()) {
                        playlistcursor.moveToPrevious();
                        playlist_num = playlistcursor.getPosition() + 1;
                        //获得播放歌曲名称
                        int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
                        playlist_name = playlistcursor.getString(musicColumnIndex);
                        //获得播放歌曲的路径
                        musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
                        playlist_path = playlistcursor.getString(musicColumnIndex);
                    } else {
                        playlistcursor.moveToLast();
                        playlist_num = playlistcursor.getPosition() + 1;
                        //获得播放歌曲名称
                        int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
                        playlist_name = playlistcursor.getString(musicColumnIndex);
                        //获得播放歌曲的路径
                        musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
                        playlist_path = playlistcursor.getString(musicColumnIndex);
                    }
                    if (binder != null)
                        unbindMusicService();
                    bindMusicService();
                    break;
                }
                case 1: {
                    if (!playlistcursor.isFirst()) {
                        playlistcursor.moveToPrevious();
                        playlist_num = playlistcursor.getPosition() + 1;
                        //获得播放歌曲名称
                        int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
                        playlist_name = playlistcursor.getString(musicColumnIndex);
                        //获得播放歌曲的路径
                        musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
                        playlist_path = playlistcursor.getString(musicColumnIndex);
                        if (binder != null)
                            unbindMusicService();
                        bindMusicService();
                    } else {
                        if (binder != null)
                            unbindMusicService();
                        txtnum.setText("");
                        txtfilename.setText("");
                        seekbar.setProgress(0);
                        prev_next = false;
                    }
                    break;
                }
                case 2: {
                    if (!playlistcursor.isFirst()) {
                        playlistcursor.moveToPrevious();
                        playlist_num = playlistcursor.getPosition() + 1;
                        //获得播放歌曲名称
                        int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
                        playlist_name = playlistcursor.getString(musicColumnIndex);
                        //获得播放歌曲的路径
                        musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
                        playlist_path = playlistcursor.getString(musicColumnIndex);
                    } else {
                        playlistcursor.moveToLast();
                        playlist_num = playlistcursor.getPosition() + 1;
                        //获得播放歌曲名称
                        int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
                        playlist_name = playlistcursor.getString(musicColumnIndex);
                        //获得播放歌曲的路径
                        musicColumnIndex = playlistcursor.getColumnIndex("playlist_path");
                        playlist_path = playlistcursor.getString(musicColumnIndex);
                    }
                    if (binder != null)
                        unbindMusicService();
                    bindMusicService();
                    break;
                }
            }
        }

        //统计不存在的文件数量
        public void countFile() {
            fileCount++;
        }

        //不存在的文件太多，停止MusicService
        public void onMusicDone() {
            fileCount = 0;
            if (binder != null)
                unbindMusicService();

        }

    }

    //连接播放服务的处理
    private ServiceConnection conn = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {

        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            //OnMusicOver over=new OnMusicOver();
            binder = (MusicService.MyBinder) service;
            binder.init(seekbar, over);
            txtnum.setText(playlist_num + "/" + playlist_count);
            txtfilename.setText(playlist_name);
            playservice = true;
            isplaying = true;
            prev_next = true;
            btnplay.setImageDrawable(getResources().getDrawable(R.drawable.music_pause));
        }
    };

    //播放列表长按得监听
    private ListView.OnItemLongClickListener longClickListener = new ListView.OnItemLongClickListener() {
        private String playlist_name;
        private int playlist_id;

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            position_num = position;
            playlistcursor.moveToPosition(position);
            //获得播放列表歌曲名称
            int musicColumnIndex = playlistcursor.getColumnIndex("playlist_name");
            playlist_name = playlistcursor.getString(musicColumnIndex);
            musicColumnIndex = playlistcursor.getColumnIndex("_id");
            playlist_id = playlistcursor.getInt(musicColumnIndex);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getResources().getString(R.string.deletefromlist))
                    .setMessage(getResources().getString(R.string.delete) + playlist_name + getResources().getString(R.string.thissong))
                    .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int i) {

                        }
                    })
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            //从播放列表数据库中删除
                            playlistDB.delete("playlist", playlist_id);
                            if (position_num < playlist_num - 1) {
                                playlist_num -= 1;
                            }
                            loadPlaylist();
                        }
                    })
                    .show();

            return true;
        }
    };

    //加载播放列表
    @SuppressWarnings("deprecation")
    private void loadPlaylist() {
        playlistcursor = playlistDB.getPlaylist("playlist");
        playlist_count = playlistcursor.getCount();
        if (playlistcursor != null && playlistcursor.getCount() >= 0) {

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.activity_song_list,
                    playlistcursor, new String[]{"playlist_name", "playlist_time", "playlist_artist", "playlist_album"},
                    new int[]{R.id.song_name, R.id.song_time, R.id.song_artist, R.id.song_album});

            music_list.setAdapter(adapter);
            if (isplaying)
                txtnum.setText(playlist_num + "/" + playlist_count);
        }
    }

    @Override
    protected void onResume() {

        volume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC);
        super.onResume();
        loadPlaylist();

    }

    //菜单的设置
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    //菜单的监听事件
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.activity_main_menu_file: {
                intent = new Intent();
                intent.setClass(MainActivity.this, FileActivity.class);
                startActivityForResult(intent, ACTIVITY_EDIT);
                overridePendingTransition(R.animator.right_left, android.R.anim.fade_out);
                break;
            }
            case R.id.activity_main_menu_help: {
                intent = new Intent();
                intent.setClass(MainActivity.this, HelpActivity.class);
                startActivity(intent);
                break;
            }
        }
        return true;
    }

    //主界面初始化
    public void initView() {


        tabhost.setup();
        TabSpec spec1 = tabhost.newTabSpec("sepc1");
        View view1 = View.inflate(this, R.layout.tab_item1, null);
        spec1.setIndicator(view1);
        spec1.setContent(R.id.tab1);
        tabhost.addTab(spec1);

        TabSpec spec2 = tabhost.newTabSpec("spec2");
        View view2 = View.inflate(this, R.layout.tab_item2, null);
        spec2.setIndicator(view2);
        spec2.setContent(R.id.tab2);
        tabhost.addTab(spec2);

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    //关闭数据库
    @Override
    protected void onDestroy() {


        playlistDB.close();
        unbindService(conn);
        stopService(serviceintent);
        unregisterReceiver();
        super.onDestroy();

    }

    //接收FileActivty传回来的数据，判断ScanService是否运行
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_EDIT)
            if (resultCode == RESULT_OK) {
                scanservice = data.getExtras().getBoolean("scanservice");

            }
    }


    /*双击返回键退出程序
     * 关闭Service和子线程 */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                scanservice = sharedpreferences.getBoolean("isscanning", false);
                if (System.currentTimeMillis() - mExitTime > 2000) {
                    if (scanservice) {
                        Toast.makeText(this, getResources().getString(R.string.scannotexit), Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        Toast.makeText(this, getResources().getString(R.string.anothorkey), Toast.LENGTH_SHORT)
                                .show();
                    }
                    mExitTime = System.currentTimeMillis();

                } else {
                    if (scanservice) {
                        Intent intent = new Intent();
                        intent.setAction("com.freecoder.ultraaudio.service.ScanService");
                        stopService(intent);
                        scanservice = false;
                        mExitTime = System.currentTimeMillis();
                        Toast.makeText(this, getResources().getString(R.string.anothorkey), Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        if (playservice) {
                            if (binder != null)
                                unbindMusicService();
                        }

                        System.exit(0);
                    }
                }
                break;
            }
            case KeyEvent.KEYCODE_VOLUME_UP: {

                if (volume != 15) {
                    volume++;
                    audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                }

                break;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {

                if (volume != 0) {
                    volume--;
                    audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                }

                break;
            }

        }
        return true;

    }

    //动态注册广播接收
    private void registerHeadsetPlugReceiver() {
        headsetPlugReceiver = new MyBroadcastReceiver();
        telephoneyRecevier = new TelephoneyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(headsetPlugReceiver, filter);
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction("android.intent.action.PHONE_STATE");
        registerReceiver(telephoneyRecevier, filter1);
    }

    //动态注销广播接收
    private void unregisterReceiver() {
        this.unregisterReceiver(headsetPlugReceiver);
        this.unregisterReceiver(telephoneyRecevier);
    }

    //耳机广播接收的实现类
    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("state")) {
                if (intent.getIntExtra("state", 0) == 0) {
                    if (isplaying)
                        if (!isplaying_pause) {
                            binder.dopause();
                            isplaying_pause = true;
                            btnplay.setImageDrawable(getResources().getDrawable(R.drawable.music_play));
                        }
                }
            }
        }

    }

    //来电广播接收的实现类
    private class TelephoneyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            int state = telephony.getCallState();
            if (state != TelephonyManager.CALL_STATE_OFFHOOK) {
                if (isplaying)
                    if (!isplaying_pause) {
                        binder.dopause();
                        isplaying_pause = true;
                        btnplay.setImageDrawable(getResources().getDrawable(R.drawable.music_play));
                    }
            }

        }

    }

    //滑动处理实现类
    private GestureDetector.OnGestureListener gestureListener = new GestureDetector.OnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {


        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {

            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            return false;

        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distanceX = e2.getRawX() - e1.getRawX();
            if (distanceX != 0) {
                if (distanceX > 0) {

                    tabhost.setCurrentTab(0);
                } else {

                    tabhost.setCurrentTab(1);
                }
            }
            return true;
        }

    };

    //左右滑动
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mygesture.onTouchEvent(event);

    }

}

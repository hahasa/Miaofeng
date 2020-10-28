package io.mountx.miaofeng.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import io.mountx.miaofeng.R;
import io.mountx.miaofeng.sql.MyDB;

public class SongActivity extends Activity {

    private ActionBar actionbar_song;
    private ListView list_song;
    private Intent intent;
    private Bundle bundle;
    private int position;
    private MyDB musicSongDB;
    private int musicColumnIndex;
    private int songcount;
    private Cursor songcursor;
    private MyDB playlistDB;
    private String playlist_name;
    private String playlist_path;
    private String playlist_artist;
    private String playlist_album;
    private String playlist_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);

        intent = this.getIntent();
        bundle = intent.getExtras();
        position = bundle.getInt("position");

        playlistDB = new MyDB(this);
        playlistDB.openOrCreate("playlist.db");

        actionbar_song = this.getActionBar();
        actionbar_song.setDisplayHomeAsUpEnabled(true);
        actionbar_song.setDisplayShowHomeEnabled(false);
        //ListView的监听
        list_song = (ListView) findViewById(R.id.list_song);
        list_song.setOnItemClickListener(mylistener);

        musicSongDB = new MyDB(this);
        musicSongDB.openOrCreate("music.db");
        Cursor cursor = musicSongDB.getTableName(position);
        musicColumnIndex = cursor.getColumnIndex("file_table");
        String table_name = cursor.getString(musicColumnIndex);
        songcursor = musicSongDB.getAllSong(table_name);
        loadList();
    }

    @SuppressWarnings("deprecation")
    private void loadList() {

        if (songcursor != null && songcursor.getCount() >= 0) {
            songcount = songcursor.getCount();
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.activity_song_list,
                    songcursor, new String[]{"song_name", "song_time", "song_artist", "song_album"},
                    new int[]{R.id.song_name, R.id.song_time, R.id.song_artist, R.id.song_album});
            actionbar_song.setTitle(songcount + getResources().getString(R.string.song));
            list_song.setAdapter(adapter);
        }
    }

    //ListView的监听
    private ListView.OnItemClickListener mylistener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //把此歌曲添加到列表中

            songcursor.moveToPosition(position);
            //获得播放列表歌曲名称
            musicColumnIndex = songcursor.getColumnIndex("song_name");
            playlist_name = songcursor.getString(musicColumnIndex);
            //获得播放列表歌曲时间
            musicColumnIndex = songcursor.getColumnIndex("song_time");
            playlist_time = songcursor.getString(musicColumnIndex);
            //获得播放列表歌曲路径
            musicColumnIndex = songcursor.getColumnIndex("song_path");
            playlist_path = songcursor.getString(musicColumnIndex);
            //获得播放列表歌曲演唱者
            musicColumnIndex = songcursor.getColumnIndex("song_artist");
            playlist_artist = songcursor.getString(musicColumnIndex);
            //获得播放列表歌曲专辑
            musicColumnIndex = songcursor.getColumnIndex("song_album");
            playlist_album = songcursor.getString(musicColumnIndex);

            new AlertDialog.Builder(SongActivity.this)
                    .setTitle(getResources().getString(R.string.addtolist))
                    .setMessage(getResources().getString(R.string.add) + playlist_name + getResources().getString(R.string.thissong))
                    .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int i) {

                        }
                    })
                    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            //添加到播放列表数据库中
                            playlistDB.appendPlaylist("playlist", playlist_name, playlist_path, playlist_album, playlist_artist, playlist_time);
                        }
                    })
                    .show();
        }
    };


    //ActionBar的监听
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                overridePendingTransition(R.animator.left_right, 0);
                break;
            }
        }
        return true;
    }

    //关闭数据库
    @Override
    protected void onDestroy() {
        musicSongDB.close();
        playlistDB.close();
        super.onDestroy();
    }
}
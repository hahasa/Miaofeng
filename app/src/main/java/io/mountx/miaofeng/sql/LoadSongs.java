package io.mountx.miaofeng.sql;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;

public class LoadSongs {

    //数据声明
    private ContentResolver musicresolver;
    private int musicColumnIndex;
    private ArrayList<String> name = new ArrayList<String>();
    private ArrayList<String> path = new ArrayList<String>();
    private ArrayList<String> table = new ArrayList<String>();
    private ArrayList<Integer> count = new ArrayList<Integer>();
    private String _path, _name;
    private int _count = 0;
    private Object[] name_1, path_1, count_1, table_1;
    private int position;
    private boolean existable = true;
    private MyDB musicDB = null;
    private int table_num = 1;
    private String song_name, song_path, song_artist, song_album;
    private int song_time_int;
    private String song_time_string;
    private String _table = "tablesong" + table_num, _table_bat;
    private String readableTime = "";

    //构造函数
    public LoadSongs(ContentResolver musicresolver, MyDB musicDB) {
        this.musicresolver = musicresolver;
        this.musicDB = musicDB;
    }


    //加载文件数据库信息
    public void loadFiles() {

        Cursor musicCursor = musicresolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.SIZE + ">80000", null, null);
        if (null != musicCursor && musicCursor.getCount() > 0) {

            for (musicCursor.moveToFirst(); !musicCursor.isAfterLast(); musicCursor.moveToNext()) {
                //获得歌曲路径
                musicColumnIndex = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
                _path = musicCursor.getString(musicColumnIndex);
                song_path = _path;
                _name = sepaFolderName(1, _path);
                _path = sepaFolderName(2, _path);

                //获得歌曲名称
                musicColumnIndex = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
                song_name = musicCursor.getString(musicColumnIndex);

                //获得歌曲演唱者
                musicColumnIndex = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
                song_artist = musicCursor.getString(musicColumnIndex);


                //获得歌曲的专辑名称
                musicColumnIndex = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM);
                song_album = musicCursor.getString(musicColumnIndex);

                //获得歌曲时间
                musicColumnIndex = musicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);
                song_time_int = musicCursor.getInt(musicColumnIndex);
                translateToString(song_time_int);
                song_time_string = readableTime;
                position = prepare();

                if (!existable) {
                    musicDB.appendSong(_table, song_name, song_path, song_album, song_artist, song_time_string);
                    readableTime = "";
                    _count += 1;
                    count.set(position, _count);
                    _table = _table_bat;
                } else {

                    musicDB.createSongTable(_table);
                    musicDB.appendSong(_table, song_name, song_path, song_album, song_artist, song_time_string);
                    readableTime = "";
                    table.add(_table);
                    table_num++;
                    _table = "tablesong" + table_num;
                    _count++;
                    name.add(_name);
                    path.add(_path);
                    count.add(_count);
                }
                existable = true;
                _count = 0;
            }
        }

        name_1 = name.toArray();
        path_1 = path.toArray();
        count_1 = count.toArray();
        table_1 = table.toArray();
        musicDB.createFileTable("tablefile");
        for (int i = 0; i < name_1.length; i++) {
            musicDB.appendFile("tablefile", (String) name_1[i], (Integer) count_1[i], (String) path_1[i], (String) table_1[i]);

        }

    }

    //把时间转换成正常模式
    private void translateToString(int musicTime) {
        int s = musicTime % 60000 / 1000;
        int m = musicTime / 60000;
        if (m >= 60) {
            int o = m / 60;
            m = m % 60;
            if (o == 0) {
                readableTime = "00" + readableTime;
            } else if (o > 0 && o < 10) {
                readableTime = "0" + o + readableTime;
            } else {
                readableTime = o + readableTime;
            }

            readableTime += ":";

        }
        if (m == 0) {
            readableTime = readableTime + "00";
        } else if (0 < m && m < 10) {
            readableTime = readableTime + "0" + m;
        } else {
            readableTime = readableTime + m;
        }
        readableTime += ":";
        if (s < 10) {
            readableTime = readableTime + "0" + s;
        } else {
            readableTime = readableTime + s;
        }

    }


    //提取文件夹名称,改变路径
    private String sepaFolderName(int mode, String _path1) {
        int index1 = 0, index2;
        char char1;
        boolean bool1 = true;
        index2 = _path1.lastIndexOf('/');
        int i;
        for (i = index2 - 1; bool1; i--) {
            char1 = _path1.charAt(i);
            if (char1 == '/') {
                bool1 = false;
                index1 = i;
            }
        }
        if (mode == 1)
            return _path1.substring(index1 + 1, index2);
        else return _path1.substring(0, index1 + 1);
    }

    //把ArrayList转换成数组
    private int prepare() {
        name_1 = name.toArray();
        int i;
        for (i = 0; i < name_1.length && existable; i++) {
            if (name_1[i].equals(_name)) {
                existable = false;
                _count = count.get(i);
                _table_bat = _table;
                _table = table.get(i);
            }
        }
        return (i - 1);
    }
}
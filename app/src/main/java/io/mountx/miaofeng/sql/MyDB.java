package io.mountx.miaofeng.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class MyDB {

    //数据定义
    public SQLiteDatabase db = null;
    private Context mCtx = null;
    //文件夹表的结构
    private final static String ID = "_id";
    private final static String NAME = "file_name";
    private final static String COUNT = "file_count";
    private final static String PATH = "file_path";
    private final static String TABLE = "file_table";
    //歌曲表的结构
    private final static String _NAME = "song_name";
    private final static String _PATH = "song_path";
    private final static String _TIME = "song_time";
    private final static String _ARTIST = "song_artist";
    private final static String _ALBUM = "song_album";
    //播放列表的结构
    private final static String NAME_ = "playlist_name";
    private final static String PATH_ = "playlist_path";
    private final static String TIME_ = "playlist_time";
    private final static String ARTIST_ = "playlist_artist";
    private final static String ALBUM_ = "playlist_album";

    //构造方法
    public MyDB(Context ctx) {
        this.mCtx = ctx;
    }

    //创建或者打开数据库
    public void openOrCreate(String dbname) throws SQLException {
        db = mCtx.openOrCreateDatabase(dbname, 0, null);
    }

    //创建文件夹表格
    public void createFileTable(String tablename) {

        String CREATE_FILE_TABLE =
                "CREATE TABLE " + tablename + " ("
                        + ID + " INTEGER PRIMARY KEY,"
                        + NAME + " TEXT," + PATH + " TEXT," + COUNT + " INTEGER," + TABLE + " TEXT" + ")";
        try {
            db.execSQL(CREATE_FILE_TABLE);

        } catch (Exception e) {

        }
    }

    //创建歌曲表格
    public void createSongTable(String tablename) {

        String CREATE_SONG_TABLE =
                "CREATE TABLE " + tablename + " ("
                        + ID + " INTEGER PRIMARY KEY,"
                        + _NAME + " TEXT," + _PATH + " TEXT," + _TIME + " TEXT,"
                        + _ARTIST + " TEXT," + _ALBUM + " TEXT" + ")";
        try {
            db.execSQL(CREATE_SONG_TABLE);
        } catch (Exception e) {
        }
    }

    //创建播放列表表格
    public void createPlaylistTable(String tablename) {
        String CREATE_PLAYLIST_TABLE =
                "CREATE TABLE " + tablename + " ("
                        + ID + " INTEGER PRIMARY KEY,"
                        + NAME_ + " TEXT," + PATH_ + " TEXT," + TIME_ + " TEXT,"
                        + ARTIST_ + " TEXT," + ALBUM_ + " TEXT" + ")";
        try {
            db.execSQL(CREATE_PLAYLIST_TABLE);
        } catch (Exception e) {
        }
    }

    //关闭数据库
    public void close() {
        db.close();
    }

    //获得全部文件夹信息
    public Cursor getAllFile(String tablename) {
        return db.query(tablename, new String[]{ID, NAME, COUNT, PATH},
                null, null, null, null, null, null);
    }

    //获得播放列表
    public Cursor getPlaylist(String tablename) {
        return db.query(tablename, new String[]{ID, NAME_, PATH_, ARTIST_, TIME_, ALBUM_},
                null, null, null, null, null, null);
    }

    //获得歌曲文件夹名称
    public Cursor getTableName(int rowid) {
        Cursor mCursor = db.query("tablefile", new String[]{ID, TABLE},
                ID + "=" + rowid, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    //获得全部歌曲文件夹的名称
    public Cursor getAllTable() {
        return db.query("tablefile", new String[]{ID, TABLE}, null, null, null, null, null, null);
    }

    //获得一个文件夹全部歌曲信息
    public Cursor getAllSong(String tablename) {
        return db.query(tablename, new String[]{ID, _NAME, _PATH, _ARTIST, _TIME, _ALBUM},
                null, null, null, null, null, null);
    }

    //获得单个文件夹信息
    public Cursor getFile(String tablename, long rowid) throws SQLException {
        Cursor mCursor = db.query(tablename, new String[]{ID, NAME, COUNT, PATH},
                ID + "=" + rowid, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    //获得单个歌曲信息
    public Cursor getSong(String tablename, long rowid) throws SQLException {
        Cursor mCursor = db.query(tablename, new String[]{ID, _NAME, _PATH, _ARTIST, _TIME, _ALBUM},
                ID + "=" + rowid, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    //添加文件夹
    public long appendFile(String tablename, String name, int count, String path, String table) {
        ContentValues args = new ContentValues();
        args.put(NAME, name);
        args.put(COUNT, count);
        args.put(PATH, path);
        args.put(TABLE, table);
        return db.insert(tablename, null, args);
    }

    //添加歌曲
    public long appendSong(String tablename, String name, String path, String album, String artist, String time) {
        ContentValues args = new ContentValues();
        args.put(_NAME, name);
        args.put(_TIME, time);
        args.put(_PATH, path);
        args.put(_ALBUM, album);
        args.put(_ARTIST, artist);
        return db.insert(tablename, null, args);
    }

    //添加一条播放歌曲
    public long appendPlaylist(String tablename, String name, String path, String album, String artist, String time) {
        {
            ContentValues args = new ContentValues();
            args.put(NAME_, name);
            args.put(TIME_, time);
            args.put(PATH_, path);
            args.put(ALBUM_, album);
            args.put(ARTIST_, artist);
            return db.insert(tablename, null, args);
        }
    }

    //删除表格一条数据
    public boolean delete(String tablename, long rowid) {
        return db.delete(tablename, ID + "=" + rowid, null) > 0;
    }

    //删除所有数据
    public boolean deleteAll(String tablename) {
        return db.delete(tablename, null, null) > 0;
    }

    //修改文件夹表格一条数据
    public boolean updateFile(String tablename, long rowid, String name, int count, String path, String table) {
        ContentValues args = new ContentValues();
        args.put(NAME, name);
        args.put(COUNT, count);
        args.put(PATH, path);
        args.put(TABLE, table);
        return db.update(tablename, args, ID + "=" + rowid, null) > 0;
    }

    //修改歌曲表格一条数据
    public boolean updateSong(String tablename, long rowid, String name, String path, String album, String artist, String time) {
        ContentValues args = new ContentValues();
        args.put(_NAME, name);
        args.put(_TIME, time);
        args.put(_PATH, path);
        args.put(_ALBUM, album);
        args.put(_ARTIST, artist);
        return db.update(tablename, args, ID + "=" + rowid, null) > 0;
    }
}






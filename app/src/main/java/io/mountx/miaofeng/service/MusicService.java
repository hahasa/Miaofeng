package io.mountx.miaofeng.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.IOException;

import io.mountx.miaofeng.R;
import io.mountx.miaofeng.ui.MainActivity;

public class MusicService extends Service {

    private MediaPlayer mp;
    private String path;
    private boolean isplaying = false;
    private MainActivity.OnMusicOver over;

    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("onbind");
        path = intent.getStringExtra("path");
        return new MyBinder();
    }

    @Override
    public void onCreate() {
        System.out.println("oncreate");
        super.onCreate();
        try {
            mp = new MediaPlayer();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("onstart");
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        System.out.println("ondestroy");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        System.out.println("onunbind");
        return super.onUnbind(intent);
    }

    // 暂停播放
    public void pause() {
        mp.pause();
    }

    // 继续播放
    public void resume() {

        mp.start();
    }

    public class MyBinder extends Binder {
        private SeekBar seekbar;
        Handler handle = new Handler() {};
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                // 根新进度条
                if (mp.isPlaying()) {
                    seekbar.setProgress(mp.getCurrentPosition());

                }
                handle.postDelayed(runnable, 100);
            }
        };

        public void dopause() {
            pause();
        }

        public void doresume() {
            resume();
        }

        public void doremovethread() {
            handle.removeCallbacks(runnable);
        }

        public void init(SeekBar seekbar, final MainActivity.OnMusicOver _over) {
            this.seekbar = seekbar;
            over = _over;
            mp.reset();
            try {
                mp.setDataSource(path);
                mp.prepare();
                seekbar.setMax(mp.getDuration());
                //设置拖动进度条改变的时候的监听方法
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        if (fromUser) {
                            mp.seekTo(progress);

                        }
                    }
                });
                mp.start();
                isplaying = true;
                handle.post(runnable);
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        over.onMusicOver();
                    }
                });
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.notfile), Toast.LENGTH_SHORT)
                        .show();
                over.countFile();
                if (over.fileCount < 5)
                    over.onMusicOver();
                else
                    over.onMusicDone();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public void dostart() {
            try {
                mp.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void dostop() {
            if (mp != null && isplaying) {
                mp.stop();
                mp.release();
                mp = null;
            }
        }
    }
}


package com.hq.hqmusic.UI;

import android.Manifest;
import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hq.hqmusic.Adapter.MyAdapter;
import com.hq.hqmusic.CustomView.CustomDialog;
import com.hq.hqmusic.CustomView.MyDialog;
import com.hq.hqmusic.Entity.Song;
import com.hq.hqmusic.R;
import com.hq.hqmusic.StatusBar.BaseActivity;
import com.hq.hqmusic.StatusBar.SystemBarTintManager;
import com.hq.hqmusic.Utils.ImageCacheUtil;
import com.hq.hqmusic.Utils.MusicUtils;
import com.hq.hqmusic.Lrc.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;



public class MusicActivity extends BaseActivity {

    public static LrcView lrcView;
    //public LrcList lrcList;

    //跳转至小播放界面
    private ImageView mij;

    private SharedPreferences sharedPreferences;
    //private TextView textView;
    private List<Song> list;


    private MyAdapter adapter;
    //private PopupWindow popupWindow;
    private MediaPlayer mplayer = new MediaPlayer();

    private SeekBar seekBar;
    private TextView textView1, textView2;
    private int screen_width;
    //private Random random = new Random();
    // 用于判断当前的播放顺序，0->单曲循环,1->顺序播放,2->随机播放
    private int play_style = 0;
    // 判断seekbar是否正在滑动
    private boolean ischanging = false;
    private Thread thread;
    // 当前音乐播放位置,从0开始
    private int currentposition;

    // 该字符串用于判断主题
    private String string_theme;
    // 修改顶部状态栏颜色使用
    private SystemBarTintManager mTintManager;
    //歌词显示界面
    public LrcView lv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //获取权限
        getAuthority();

        sharedPreferences = getSharedPreferences("location", MODE_PRIVATE);
        // 主题设置
        string_theme = sharedPreferences.getString("theme_select", "blue");
        if (string_theme.equals("blue")) {
            setTheme(R.style.Theme_blue);
        } else if (string_theme.equals("purple")) {
            setTheme(R.style.Theme_purple);
        } else if (string_theme.equals("green")) {
            setTheme(R.style.Theme_green);
        } else {
            setTheme(R.style.Theme_red);
        }

        setContentView(R.layout.activity_music);

        //跳转至小播放界面
        mij=findViewById(R.id.jump2);
        mij.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MusicActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // 顶部状态栏颜色设置
        mTintManager = new SystemBarTintManager(MusicActivity.this);
        mTintManager.setStatusBarTintEnabled(true);
        if (string_theme.equals("blue")) {
            mTintManager.setStatusBarTintResource(R.color.blue);
        } else if (string_theme.equals("purple")) {
            mTintManager.setStatusBarTintResource(R.color.purple);
        } else if (string_theme.equals("green")) {
            mTintManager.setStatusBarTintResource(R.color.green);
        } else {
            mTintManager.setStatusBarTintResource(R.color.red);
        }

        // 获得屏幕宽度并保存在screen_width中
        init_screen_width();

        // 加载currentposition的初始数据
        currentposition = sharedPreferences.getInt("currentposition", 0);


        // 顶部和 底部操作栏按钮点击事件
        setClick();

        // 给textView1和textView2赋初值
        initText();


        // 设置mediaplayer监听器
        setMediaPlayerListener();



    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == 1) {

            Bitmap bitmap = ImageCacheUtil.getResizedBitmap(null, null,
                    MusicActivity.this, data.getData(), screen_width, true);
            BitmapDrawable drawable = new BitmapDrawable(null, bitmap);

            //listview.setBackground(drawable);
            saveDrawable(drawable);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    // 给屏幕宽度赋值
    private void init_screen_width() {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        screen_width = size.x;
    }




    private void setClick() {


        /*imageview_location.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (currentposition - 3 <= 0) {
                    listview.setSelection(0);
                } else {
                    listview.setSelection(currentposition - 3);
                }

            }
        });*/

        View layout_playbar2 = (View) findViewById(R.id.layout_playbar2);

        textView1 = (TextView) layout_playbar2.findViewById(R.id.name2);
        textView2 = (TextView) layout_playbar2.findViewById(R.id.singer2);
        seekBar = (SeekBar) layout_playbar2.findViewById(R.id.seekbar2);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                ischanging = false;
                mplayer.seekTo(seekBar.getProgress());
                thread = new Thread(new MusicActivity.SeekBarThread());
                thread.start();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                ischanging = true;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                // 可以用来写拖动的时候实时显示时间
            }
        });

    }

    private void initText() {
        LrcParse a =new LrcParse(Environment.getExternalStorageDirectory().getAbsolutePath()+textView1.getText()+".MP3");
        LrcInfo lrcInfo = a.readLrc();

        textView1.setText(sharedPreferences.getString("song_name", "歌曲名").trim());
        textView2.setText(sharedPreferences.getString("song_singer", "歌手").trim());
    }



    private void setMediaPlayerListener() {
        // 监听mediaplayer播放完毕时调用
        // 设置发生错误时调用
        mplayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                // TODO Auto-generated method stub
                mp.reset();
                // Toast.makeText(MainActivity.this, "未发现音乐", 1500).show();
                return false;
            }
        });
    }

    // 自定义的线程,用于下方seekbar的刷新
    class SeekBarThread implements Runnable {

        @Override
        public void run() {
            while (!ischanging && mplayer.isPlaying()) {
                // 将SeekBar位置设置到当前播放位置
                seekBar.setProgress(mplayer.getCurrentPosition());

                try {
                    // 每500毫秒更新一次位置
                    Thread.sleep(500);
                    // 播放进度

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }




    // 切掉音乐名字最后的.mp3
    private String cut_song_name(String name) {
        if (name.length() >= 5
                && name.substring(name.length() - 4, name.length()).equals(
                ".mp3")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }



     //使用sharedPreferences保存listview背景图片
    private void saveDrawable(Drawable drawable) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        Bitmap bitmap = bitmapDrawable.getBitmap();
        // Bitmap bitmap = BitmapFactory.decodeResource(getResources(), id);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        String imageBase64 = new String(Base64.encodeToString(
                baos.toByteArray(), Base64.DEFAULT));
        editor.putString("listbg", imageBase64);
        editor.commit();
    }

    // 加载用sharedPreferences保存的图片
    private Drawable loadDrawable() {
        String temp = sharedPreferences.getString("listbg", "");
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(
                temp.getBytes(), Base64.DEFAULT));
        return Drawable.createFromStream(bais, "");
    }



    private void getAuthority(){
        //适配6.0以上机型请求权限
        PermissionGen.with(MusicActivity.this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .request();
    }

    //以下三个方法用于6.0以上权限申请适配
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @PermissionSuccess(requestCode = 100)
    public void doSomething(){
        //Toast.makeText(this, "相关权限已允许", Toast.LENGTH_SHORT).show();
    }

    @PermissionFail(requestCode = 100)
    public void doFailSomething(){
        //Toast.makeText(this, "相关权限已拒绝", Toast.LENGTH_SHORT).show();
    }

}
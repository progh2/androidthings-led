package kr.hs.emirim.progh2.blinkled;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;

import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.squareup.picasso.Picasso;
import com.tsengvn.typekit.TypekitContextWrapper;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements IVLCVout.Callback, LibVLC.HardwareAccelerationError, TextureView.SurfaceTextureListener {

    private static final String TAG = "투비:MainAct";
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 1000;
    private static final String LED1_PIN = "BCM17";
    private static final String LED2_PIN = "BCM27";
    private static final String LED3_PIN = "BCM22";

    private Handler mHandler = new Handler();
    private Gpio mLed1;
    private Gpio mLed2;
    private Gpio mLed3;

    private int mState;
    TextView mTextView;



    //기본 위젯 설정및 변수 설정
    private LibVLC libvlc;

    private MediaPlayer mMediaPlayer = null;

    //public String mFilePath = "/sdcard/practice/video.mp4";
    public String mFilePath ="android.resource://kr.hs.emirim.progh2.blinkled/" + R.raw.m;
           // "https://and26oo-d34ec.firebaseapp.com/video/m.mp4";
    //파일위치는 /sdcard/practice/video.mp4 다음과 같이 지정한다.

    private int mVideoWidth;
    private int mVideoHeight;


    private SeekBar mSeekBar;
    private TextureView mTexture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.state);


        PeripheralManagerService service = new PeripheralManagerService();
        try {
            mLed1 = service.openGpio(LED1_PIN);
            mLed1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLed2 = service.openGpio(LED2_PIN);
            mLed2.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLed3 = service.openGpio(LED3_PIN);
            mLed3.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

        }catch(Exception e) {
            Log.e(TAG, "뭔가 에러 났어요~ : Error on PeripheralIO API", e);
        }

        mState = 1;
        mHandler.post(mBlinkRunnable);

        WebView w = (WebView) findViewById(R.id.webview);
        // 웹뷰에서 자바스크립트실행가능
        w.getSettings().setJavaScriptEnabled(true);

        w.getSettings().setDefaultTextEncodingName("UTF-8");
        String str = "한글";
        w.loadData(str,  "text/html", "UTF-8");  // Android 4.0 이하 버전
        w.loadData(str,  "text/html; charset=UTF-8", null);  // Android 4.1 이상 버전

        // 구글홈페이지 지정
        w.loadUrl("https://and26oo-d34ec.firebaseapp.com/");


        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        Picasso.with(this)
                .load("https://scontent-icn1-1.xx.fbcdn.net/v/t1.0-0/p480x480/22089270_1992277074378361_3842491535050661487_n.jpg?oh=fdb013f149c87b3eeeb9476c93e0f916&oe=5A52DCB4")
                .resize(100,100)
                .into(imageView);

        // WebViewClient 지정
        w.setWebViewClient(new WebViewClientClass());


        Button mStartStopBtn = (Button) findViewById(R.id.stopbtn);
        mStartStopBtn.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mMediaPlayer == null) {
                            createPlayer(mFilePath);
                            mSeekBar.setMax((int) mMediaPlayer.getLength());
                        } else {
                            int state = mMediaPlayer.getPlayerState();
                            switch (state) {
                                case 4:  // stopping
                                case 3:  // playing
                                    mMediaPlayer.pause();
                            }
                        }
                    }
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},0);
        }

        mSeekBar = (SeekBar)findViewById(R.id.seekBar);
        mSeekBar.setMax(0);
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);

        //Video on View
        mTexture = (TextureView) findViewById(R.id.surface);
        mTexture.setSurfaceTextureListener(this);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mBlinkRunnable);
        // Clean all resources;
        if(mLed1 != null){
            try{
                mLed1.setValue(false);
                mLed1.close();
            }catch (IOException e ){
                Log.e(TAG, "Error on PIO API", e);
            }
        }
        if(mLed2 != null){
            try{
                mLed2.setValue(false);
                mLed2.close();
            }catch (IOException e ){
                Log.e(TAG, "Error on PIO API", e);
            }
        }
        if(mLed3 != null){
            try{
                mLed3.setValue(false);
                mLed3.close();
            }catch (IOException e ){
                Log.e(TAG, "Error on PIO API", e);
            }
        }
        releasePlayer();
    }

    private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {


            Toast.makeText(getApplicationContext(), "효원아 안녕", Toast.LENGTH_LONG).show();

            if(mLed1 == null || mLed2 == null || mLed3 == null) {
                return;
            }

            try{
                mLed1.setValue(false);
                mLed2.setValue(false);
                mLed3.setValue(false);
                switch (mState){
                    case 1:
                        mLed1.setValue(true);
                        break;
                    case 2:
                        mLed2.setValue(true);
                        break;
                    case 3:
                        mLed3.setValue(true);
                        break;
                }
                Log.i(TAG, "와~ LED" + mState + "번이 켜졌어요~? ");
                mTextView.setText("" + mState + "번 LED 켜짐");
                mState ++;
                if(mState > 3 ){
                    mState = 1;
                }
                mHandler.postDelayed( mBlinkRunnable, INTERVAL_BETWEEN_BLINKS_MS);

            }catch (IOException e){
                Log.e(TAG, "으악 쓰레드 돌다 폭파되었어요!", e);
            }
        }
    };

    @Override //VLC 레이아웃 설정
    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;
        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout ivlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout ivlcVout) {

    }

    @Override  //하드웨어 가속 에러시 플레이어 종료
    public void eventHardwareAccelerationError() {
        releasePlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

    }

    @Override  //SurfaceTexture 화면(동영상 해상도 및 사이즈)에 따라 크기 변경
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        setSize(mVideoWidth, mVideoHeight);
    }

    @Override  //SurfaceTexture 화면이 종료되었을때 종료
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if(!(mMediaPlayer == null))
            mMediaPlayer.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    //미디어 플레이어 리스너 클래스
    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        //액티비티 변수를 받아오기 위하여 지정
        private MyPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            MainActivity player = mOwner.get();

            switch(event.type) {
                case MediaPlayer.Event.EndReached:
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                    player.mSeekBar.setMax((int) player.mMediaPlayer.getLength());
                    player.mSeekBar.setOnSeekBarChangeListener(player.mSeekListener);
                    break;
                case MediaPlayer.Event.Paused:
                    break;
                case MediaPlayer.Event.Stopped:
                    break;
                case MediaPlayer.Event.PositionChanged:
                    player.mSeekBar.setProgress((int)player.mMediaPlayer.getTime());
                default:
                    break;
            }
        }
    }

    private void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        if(mTexture == null)
            return;

        //화면사이즈
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

        //Orientation 계산
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        ViewGroup.LayoutParams lp = mTexture.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mTexture.setLayoutParams(lp);
        mTexture.invalidate();
    }

    //VLC 플레이어 실행
    private void createPlayer(String media) {
        releasePlayer();
        try {
            if (media.length() > 0) {
                Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        0);
                toast.show();
            }

            // Create LibVLC
            ArrayList<String> options = new ArrayList<>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            libvlc = new LibVLC(options);
            libvlc.setOnHardwareAccelerationError(this);

            mTexture.setKeepScreenOn(true);

            // Create media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Set up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mTexture);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            Media m = new Media(libvlc, media);
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();

        } catch (Exception e) {
            Toast.makeText(this, "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    //플레이어 종료
    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        libvlc.release();
        libvlc = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser){
                mMediaPlayer.setTime(progress);
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    private class WebViewClientClass extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(TypekitContextWrapper.wrap(newBase));
    }


}

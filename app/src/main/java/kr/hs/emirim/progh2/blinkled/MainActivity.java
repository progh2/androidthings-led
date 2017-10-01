package kr.hs.emirim.progh2.blinkled;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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

import java.io.IOException;

public class MainActivity extends AppCompatActivity  implements   SurfaceHolder.Callback {

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

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    MediaPlayer mediaPlayer;
    Button playButton;



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

        // surfaceView 등록
        surfaceView = (SurfaceView) findViewById(R.id.surface);
        surfaceHolder = surfaceView.getHolder();
        // Activity로 Video Stream 전송 등록
        surfaceHolder.addCallback(this);

        playButton = (Button)findViewById(R.id.play_btn);
        playButton.setOnClickListener(clickListener);

        Button stopButton = (Button) findViewById(R.id.stop_btn);
        stopButton.setOnClickListener(clickListener);


    }

    Button.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.play_btn) {
                Log.i("main", "play");
                startOrPause();
            } else if (v.getId() == R.id.stop_btn) {
                stopNprepare();
            }
        }
    };


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

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

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
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
/**
 * surface 생성
 */
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        } else {
            mediaPlayer.reset();
        }

        try {
// local resource : raw에 포함시켜 놓은 리소스 호출
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/raw/too_cute");
            mediaPlayer.setDataSource(this, uri);

// external URL : 외부 URL이나 path를 지정한 리소스
// String path = "http://techslides.com/demos/sample-videos/small.mp4";
// mediaPlayer.setDataSource(path);

            mediaPlayer.setDisplay(holder); // 화면 호출
            mediaPlayer.prepare(); // 비디오 load 준비
            mediaPlayer.setOnCompletionListener(completionListener); // 비디오 재생 완료 리스너
// mediaPlayer.setOnVideoSizeChangedListener(sizeChangeListener); // 비디오 크기 변경 리스너

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
        /**
         * 비디오가 전부 재생된 상태의 리스너
         * @param mp : 현재 제어하고 있는 MediaPlayer
         */
        @Override
        public void onCompletion(MediaPlayer mp) {
            playButton.setText("Play");
        }
    };

    MediaPlayer.OnVideoSizeChangedListener sizeChangeListener = new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        }
    };

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private void startOrPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playButton.setText("Play");
        } else {
            mediaPlayer.start();
            playButton.setText("Pause");
        }
    }

    private void stopNprepare() {
        mediaPlayer.stop();
        playButton.setText("Play");


        try {
// mediaplayer 재생 준비
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

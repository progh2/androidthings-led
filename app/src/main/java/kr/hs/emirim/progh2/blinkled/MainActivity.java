package kr.hs.emirim.progh2.blinkled;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.squareup.picasso.Picasso;
import com.tsengvn.typekit.TypekitContextWrapper;

import java.io.IOException;

public class MainActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener{

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

    private String DEVELOPER_KEY = "AIzaSyAqZ0YqrUdWLdbP_g7aoCIZcOMr13Tha1U";
    private YouTubePlayerView mYouTubePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG ,"사용가능여부:"+ YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(this)); //SUCCSESS
        //YouTubePlayer를 초기화
        YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        youTubeView.initialize( DEVELOPER_KEY, this);
        mYouTubePlayer = (YouTubePlayerView) findViewById(R.id.youtube_view);
        mYouTubePlayer.initialize(DEVELOPER_KEY,this);
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
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        if (!b) {
            //player.cueVideo("wKJ9KzGQq0w"); //video id.

            youTubePlayer.cueVideo("IA1hox-v0jQ");  //http://www.youtube.com/watch?v=IA1hox-v0jQ

            //cueVideo(String videoId)
            //지정한 동영상의 미리보기 이미지를 로드하고 플레이어가 동영상을 재생하도록 준비하지만
            //play()를 호출하기 전에는 동영상 스트림을 다운로드하지 않습니다.
            //https://developers.google.com/youtube/android/player/reference/com/google/android/youtube/player/YouTubePlayer
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(this, -1).show();
        } else {
            String errorMessage = String.format(
                    getString(R.string.error_player), errorReason.toString());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == -1) {
            // Retry initialization if user performed a recovery action
            mYouTubePlayer.initialize(DEVELOPER_KEY, this);
        }
    }
}

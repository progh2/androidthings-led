package kr.hs.emirim.progh2.blinkled;

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
import com.squareup.picasso.Picasso;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

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

//        WebView w = (WebView) findViewById(R.id.webview);
//        // 웹뷰에서 자바스크립트실행가능
//        w.getSettings().setJavaScriptEnabled(true);
//        // 구글홈페이지 지정
//        w.loadUrl("https://matefirebase.firebaseapp.com");
//
//        Toast.makeText(this, "효원아 안녕", Toast.LENGTH_LONG).show();
//
//        ImageView imageView = (ImageView) findViewById(R.id.imageView);
//        Picasso.with(this)
//                .load("https://scontent-icn1-1.xx.fbcdn.net/v/t1.0-1/21686500_2018763085071977_3164738053449787305_n.jpg?oh=60893c2c9d1ab39319a903643d51d3e9&oe=5A451533")
//                .resize(100,100)
//                .into(imageView);
//
//        // WebViewClient 지정
//        w.setWebViewClient(new WebViewClientClass());


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
    //    private class WebViewClientClass extends WebViewClient {
//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            view.loadUrl(url);
//            return true;
//        }
//    }

}

package kr.hs.emirim.progh2.blinkled;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.naver.speech.clientapi.SpeechRecognitionResult;
import com.squareup.picasso.Picasso;
import com.tsengvn.typekit.TypekitContextWrapper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

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
    private TextView mTextView;

    private Button mButtonTTS;
    TextToSpeech mTTS;
    private ArrayList<String> mDurisays = new ArrayList<>();

    private static final String CLIENT_ID = "0oaUmKeJptvjzk6o9FPU"; // "내 애플리케이션"에서 Client ID를 확인해서 이곳에 적어주세요.
    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;
    private TextView txtResult;
    private Button btnStart;
    private String mResult;
    private AudioWriterPCM writer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.state);
        mButtonTTS = (Button) findViewById(R.id.tts);
        mButtonTTS.setOnClickListener(this);
        Log.e(TAG, "TTS 기능 세팅 시작");
        mDurisays.add("구지원은 바보입니다");
        mDurisays.add("선생님 안녕하세요");
        mDurisays.add("저는 숙제중이에요");
        mDurisays.add("저는 자고 싶어요");
        mDurisays.add("환영합니다");

        mTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.e(TAG, "TTS 기능 초기화 시도!");
                if(status == TextToSpeech.SUCCESS){
                    Log.e(TAG, "TTS 기능 초기화 성공! 한글 언어로 세팅");
                    mTTS.setLanguage(Locale.KOREAN);
                    mTTS.setPitch(0.8f);
                    mTTS.setSpeechRate(1.2f);
                }else{
                    Log.e(TAG, "TTS 초기화 실패! : " +  status );
                    mTTS = null;
                }
            }
        });


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

        txtResult = (TextView) findViewById(R.id.txt_result);
        btnStart = (Button) findViewById(R.id.btn_start);
        handler = new RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, CLIENT_ID);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!naverRecognizer.getSpeechRecognizer().isRunning()) {
                    mResult = "";
                    txtResult.setText("Connecting...");
                    btnStart.setText(R.string.str_stop);
                    naverRecognizer.recognize();
                } else {
                    Log.d(TAG, "stop and wait Final Result");
                    btnStart.setEnabled(false);
                    naverRecognizer.getSpeechRecognizer().stop();
                }
            }
        });
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
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tts:

                Random r = new Random();
                Log.e(TAG, "한글 출력 시작!");
                String utteranceId = this.hashCode() + "";

                mTTS.speak( mDurisays.get(r.nextInt(mDurisays.size())), TextToSpeech.QUEUE_ADD, null, utteranceId);
                break;
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
    protected void onStart() {
        super.onStart();
        // NOTE : initialize() must be called on start time.
        naverRecognizer.getSpeechRecognizer().initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mResult = "";
        txtResult.setText("");
        btnStart.setText(R.string.str_start);
        btnStart.setEnabled(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // NOTE : release() must be called on stop time.
        naverRecognizer.getSpeechRecognizer().release();
    }

    // Declare handler for handling SpeechRecognizer thread's Messages.
    static class RecognitionHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        RecognitionHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }


    // Handle speech recognition Messages.
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.
                txtResult.setText("Connected");
                writer = new AudioWriterPCM(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
                writer.open("Test");
                break;

            case R.id.audioRecording:
                writer.write((short[]) msg.obj);
                break;

            case R.id.partialResult:
                // Extract obj property typed with String.
                mResult = (String) (msg.obj);
                txtResult.setText(mResult);
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                StringBuilder strBuf = new StringBuilder();
                for(String result : results) {
                    strBuf.append(result);
                    strBuf.append("\n");
                }
                mResult = strBuf.toString();
                txtResult.setText(mResult);
                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                txtResult.setText(mResult);
                btnStart.setText(R.string.str_start);
                btnStart.setEnabled(true);
                break;

            case R.id.clientInactive:
                if (writer != null) {
                    writer.close();
                }

                btnStart.setText(R.string.str_start);
                btnStart.setEnabled(true);
                break;
        }
    }

}

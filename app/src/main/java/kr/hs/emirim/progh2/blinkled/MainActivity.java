package kr.hs.emirim.progh2.blinkled;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
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

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;
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

//    private Handler mHandler = new Handler();
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

    private Button btnWifi;

    public static final String PIR_PIN = "BCM4"; //physical pin #11
    private Gpio mPirGpio;
    private TextView textPri;
    private  TextView textTemp;

//    private static final int INTERVAL_BETWEEN_SPI_MS = 2000;
//    private static final String SPI_DEVICE_NAME = "SPI0.0";
//    private Handler mHandler = new Handler();
//    private SpiDevice mDevice;

    private MCP3008 mMCP3008;
    private Handler mHandler;
    private Runnable mReadAdcRunnable = new Runnable() {
        private static final long DELAY_MS = 3000L; // 3 seconds
        @Override
        public void run() {
            int temp;
            double temp2;
            if (mMCP3008 == null) {
                return;
            }

            try {
                Log.e("MCP3008", "ADC 0: " + mMCP3008.readAdc(0x0));
                textPri.setText("ADC 0 : " + mMCP3008.readAdc(0x0)) ;
                Log.e("MCP3008", "ADC 1: " + mMCP3008.readAdc(0x1));
                temp = Integer.parseInt( "" + mMCP3008.readAdc(0x1) );
                temp2 = ( temp * 5 * 100 ) / 1024;
                textTemp.setText("온도 : " + temp2 ) ;
                Log.e("MCP3008", "ADC 2: " + mMCP3008.readAdc(0x2));
                Log.e("MCP3008", "ADC 3: " + mMCP3008.readAdc(0x3));
                Log.e("MCP3008", "ADC 4: " + mMCP3008.readAdc(0x4));
                Log.e("MCP3008", "ADC 5: " + mMCP3008.readAdc(0x5));
                Log.e("MCP3008", "ADC 6: " + mMCP3008.readAdc(0x6));
                Log.e("MCP3008", "ADC 7: " + mMCP3008.readAdc(0x7));
            } catch( IOException e ) {
                Log.e("MCP3008", "Something went wrong while reading from the ADC: " + e.getMessage());
            }

            mHandler.postDelayed(this, DELAY_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // String csPin, String clockPin, String mosiPin, String misoPin
            mMCP3008 = new MCP3008("BCM8", "BCM11", "BCM10", "BCM9");
            mMCP3008.register();
        } catch( IOException e ) {
            Log.e(TAG, "MCP initialization exception occurred: " + e.getMessage());
        }

        mHandler = new Handler();
        mHandler.post(mReadAdcRunnable);

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

        textPri = (TextView) findViewById(R.id.txt_pir);
        textTemp = (TextView) findViewById(R.id.txt_temp);
        PeripheralManagerService service = new PeripheralManagerService();
        try {
//            mLed1 = service.openGpio(LED1_PIN);
//            mLed1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
//            mLed2 = service.openGpio(LED2_PIN);
//            mLed2.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
//            mLed3 = service.openGpio(LED3_PIN);
//            mLed3.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mPirGpio = service.openGpio(PIR_PIN);
            // Configure as an input.
            mPirGpio.setDirection(Gpio.DIRECTION_IN);
            // Enable edge trigger events for both falling and rising edges. This will make it a toggle button.
            mPirGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            // Register an event callback.
            mPirGpio.registerGpioCallback(mSetPRICallback);

//            mDevice = service.openSpiDevice(SPI_DEVICE_NAME);
        }catch(Exception e) {
            Log.e(TAG, "뭔가 에러 났어요~ : Error on PeripheralIO API", e);
        }
//        mHandler.post(mSpiRunaable);

//        mState = 1;
//        mHandler.post(mBlinkRunnable);

//        WebView w = (WebView) findViewById(R.id.webview);
//        // 웹뷰에서 자바스크립트실행가능
//        w.getSettings().setJavaScriptEnabled(true);
//
//        w.getSettings().setDefaultTextEncodingName("UTF-8");
        String str = "한글";
//        w.loadData(str,  "text/html", "UTF-8");  // Android 4.0 이하 버전
//        w.loadData(str,  "text/html; charset=UTF-8", null);  // Android 4.1 이상 버전
//
//        // 구글홈페이지 지정
//        w.loadUrl("https://and26oo-d34ec.firebaseapp.com/");


        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        Picasso.with(this)
                .load("https://scontent-icn1-1.xx.fbcdn.net/v/t1.0-0/p480x480/22089270_1992277074378361_3842491535050661487_n.jpg?oh=fdb013f149c87b3eeeb9476c93e0f916&oe=5A52DCB4")
                .resize(100,100)
                .into(imageView);

        // WebViewClient 지정
        //w.setWebViewClient(new WebViewClientClass());

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

        btnWifi = (Button) findViewById(R.id.wifi);
        btnWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectWifi();
            }
        });


    }

    private void connectWifi(){

        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);

        Log.e(TAG, "WIFI 상태 : " + manager.getWifiState() );
        Log.e(TAG, "WIFI 접속 시도");
        String networkSSID = "WIFI_SSID";   // 2.5 GHz만 연결 가능
        String networkPasskey = "WIFI_PW";
        Log.e(TAG, networkSSID + " / " + networkPasskey );
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + networkSSID + "\"";
        wifiConfiguration.preSharedKey = "\"" + networkPasskey + "\"";

        manager.addNetwork(wifiConfiguration);
        Log.e(TAG, "WIFI 상태 : " + manager.getWifiState() );
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mHandler.removeCallbacks(mBlinkRunnable);
//        // Clean all resources;
//
//        if(mLed1 != null){
//            try{
//                mLed1.setValue(false);
//                mLed1.close();
//            }catch (IOException e ){
//                Log.e(TAG, "Error on PIO API", e);
//            }
//        }
//        if(mLed2 != null){
//            try{
//                mLed2.setValue(false);
//                mLed2.close();
//            }catch (IOException e ){
//                Log.e(TAG, "Error on PIO API", e);
//            }
//        }
//        if(mLed3 != null){
//            try{
//                mLed3.setValue(false);
//                mLed3.close();
//            }catch (IOException e ){
//                Log.e(TAG, "Error on PIO API", e);
//            }
//        }
        // Close the resource
        if (mPirGpio != null) {
            mPirGpio.unregisterGpioCallback(mSetPRICallback);
            try {
                mPirGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }

//        if (mDevice != null) {
//            try {
//                mDevice.close();
//                mDevice = null;
//            } catch (IOException e) {
//                Log.e(TAG, "SPI 장치 닫기 실패", e);
//            }
//        }

        if( mMCP3008 != null ) {
            mMCP3008.unregister();
        }

        if( mHandler != null ) {
            mHandler.removeCallbacks(mReadAdcRunnable);
        }
    }

//    private String byteToHexString(byte[] bytes) {
//        StringBuilder sb = new StringBuilder();
//        for(byte b : bytes){
//            sb.append(String.format("%02x", b&0xff));
//        }
//        return sb.toString();
//    }
//
//    private Runnable mSpiRunaable = new Runnable() {
//        @Override
//        public void run() {
//            if(mDevice == null) return;
//
//            try{
//                byte[] tx = new byte[2];
//                byte[] rx = new byte[2];
//
//                new
//            }
//        }
//    }

        private Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {


            //Toast.makeText(getApplicationContext(), "효원아 안녕", Toast.LENGTH_LONG).show();

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

                String utteranceId = this.hashCode() + "";
                if(results.size() > 0 ) {
                    mTTS.speak("주인님께서 말씀하신 말은", TextToSpeech.QUEUE_ADD, null, utteranceId);
                    mTTS.speak(results.get(0), TextToSpeech.QUEUE_ADD, null, utteranceId);
                    mTTS.speak("라고 생각합니다.", TextToSpeech.QUEUE_ADD, null, utteranceId);
                }
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
    // Register an event callback.
    private GpioCallback mSetPRICallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Log.i(TAG, "GPIO callback ------------");

            try {
                Log.i(TAG, "GPIO callback -->" + gpio.getValue());
                // set the LED state to opposite of input state
                if(gpio.getValue()){
                    textPri.setText("감지됨");
                }else{
                    textPri.setText("감지안됨");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
            // Return true to keep callback active.
            return true;
        }
    };
}

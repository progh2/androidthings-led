package kr.hs.emirim.progh2.blinkled;

import android.app.Application;

import com.tsengvn.typekit.Typekit;

/**
 * Created by progh2 on 2017-10-01.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Typekit.getInstance()
                .addNormal(Typekit.createFromAsset(this, "bmhana11yrs.otf"));
    }
}

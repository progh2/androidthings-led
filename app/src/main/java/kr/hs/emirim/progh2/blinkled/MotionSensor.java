package kr.hs.emirim.progh2.blinkled;

/**
 * Created by progh2 on 2017-10-18.
 */

public interface MotionSensor {

    void startup();

    void shutdown();

    interface Listener {
        void onMovement();
    }

}
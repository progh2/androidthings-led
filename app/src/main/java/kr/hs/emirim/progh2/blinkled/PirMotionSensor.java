package kr.hs.emirim.progh2.blinkled;

import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;

import java.io.IOException;

/**
 * Created by progh2 on 2017-10-18.
 */

public class PirMotionSensor implements MotionSensor{
    private final Gpio bus;

    private final MotionSensor.Listener listener;

    PirMotionSensor(Gpio bus, Listener listener) {
        this.bus = bus;
        this.listener = listener;
    }

    @Override
    public void startup() {
        try {
            bus.setDirection(Gpio.DIRECTION_IN);
            bus.setActiveType(Gpio.ACTIVE_HIGH);
            //bus.setEdgeTriggerType(Gpio.EDGE_BOTH);
            bus.setEdgeTriggerType(Gpio.EDGE_RISING);
            //bus.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            throw new IllegalStateException("Sensor can't start - App is foobar'd", e);
        }
        try {
            bus.registerGpioCallback(callback);
        } catch (IOException e) {
            throw new IllegalStateException("Sensor can't register callback - App is foobar'd", e);
        }
    }

    private final GpioCallback callback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Log.i("아아", "GPIO callback ------------");
            if (bus == null) {
                return true;
            }
            try {
                Log.i("아아", "GPIO callback -->" + gpio.getValue());
                // set the LED state to opposite of input state
                //setValue(gpio.getValue());
                listener.onMovement();
            } catch (IOException e) {
                Log.e("아아", "Error on PeripheralIO API", e);
            }
            // Return true to keep callback active.
            return true;
        }
    };

    @Override
    public void shutdown() {
        bus.unregisterGpioCallback(callback);
        try {
            bus.close();
        } catch (IOException e) {
            Log.e("TUT", "Failed to shut down. You might get errors next time you try to start.", e);
        }
    }

}

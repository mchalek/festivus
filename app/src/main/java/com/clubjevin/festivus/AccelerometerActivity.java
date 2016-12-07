package com.clubjevin.festivus;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Created by kevin on 12/3/16.
 * Test Commit by drew on 12/4/16.
 */


public abstract class AccelerometerActivity extends Activity {
    // copy+paste from: http://stackoverflow.com/questions/2317428/android-i-want-to-shake-it
    private SensorManager mSensorManager;
    protected abstract void shakeAction();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

    }


//Method borrowed from: http://androidexample.com/Accelerometer_Basic_Example_-_Detect_Phone_Shake_Motion/index.php?view=article_discription&aid=109

    private final SensorEventListener mSensorListener = new SensorEventListener() {

                long now = 0;
                long timeDiff = 0;
                long lastUpdate = 0;
                long lastShake = 0;

                float x = 0;
                float y = 0;
                float z = 0;
                float force = 0;
                float curAccel = 0;
                float lastAccel = 0;
                //Sets required force value to trigger shake event.  Default should be ~9.8 (g).
                float threshold = 12.0f;
                //Sets minimum delay time between successive shake events. Effective low-pass filter?
                int interval = 100;

                public synchronized void onSensorChanged(SensorEvent event) {
                    // use the event timestamp as reference
                    // so the manager precision won't depends
                    // on the AccelerometerListener implementation
                    // processing time
                    now = event.timestamp;

                    x = event.values[0];
                    y = event.values[1];
                    z = event.values[2];
                    curAccel = (float) Math.sqrt((double)(x*x + y*y + z*z));

                    if (lastUpdate == 0) {
                        lastUpdate = now;
                        lastShake = now;
                        lastAccel = curAccel;

                    } else {
                        timeDiff = now - lastUpdate;

                        if (timeDiff > 0) {
                                //f=ma, take delta of old and new accel totals)
                            force = Math.abs(curAccel - lastAccel);

                            if (Float.compare(force, threshold) >0 ) {

                                if (now - lastShake >= interval) {
                                    shakeAction();
                                }

                                lastShake = now;
                            }
                            lastAccel = curAccel;
                            lastUpdate = now;
                        }

                    }
                    // trigger change event
                    //listener.onAccelerationChanged(x, y, z);
                }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

    };



    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onPause();
    }
}

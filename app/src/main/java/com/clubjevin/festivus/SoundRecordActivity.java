package com.clubjevin.festivus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by kevin on 12/7/16.
 */

public class SoundRecordActivity extends AppCompatActivity {
    private long MAX_RECORDING_DURATION_MILLIS = 30 * 1000L;
    private long COUNTER_INCREMENT_MILLIS = 43L;

    private AtomicBoolean isRecording = new AtomicBoolean(false);

    private CountDownTimer countDownTimer = null;

    public Button getRecordButton() {
        return (Button) findViewById(R.id.record_button);
    }

    public Button getStopButton() {
        return (Button) findViewById(R.id.stop_button);
    }

    public TextView getCounterText() {
        return (TextView) findViewById(R.id.counter_text);
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_soundrecord);

        getRecordButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRecording.get()) {
                    return;
                }

                startRecording();
            }
        });

        getStopButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isRecording.get()) {
                    return;
                }

                stopRecording();
            }
        });
    }

    public void startRecording() {
        Log.i("SoundRecordActivity", "StartRecording clicked");
        startTimer();

        isRecording.set(true);
    }

    public void startTimer() {
        if(countDownTimer != null) {
            throw new IllegalStateException("Timer is already running!");
        }

        countDownTimer = new CountDownTimer(MAX_RECORDING_DURATION_MILLIS, COUNTER_INCREMENT_MILLIS) {
            public void onTick(long millisUntilFinished) {
                long elapsed_seconds = (MAX_RECORDING_DURATION_MILLIS - millisUntilFinished) / 1000;
                long elapsed_millis = (MAX_RECORDING_DURATION_MILLIS - millisUntilFinished) % 1000;

                String counter = String.format("%02d:%02d", elapsed_seconds, elapsed_millis / 10);

                getCounterText().setText(counter);
            }

            @Override
            public void onFinish() {
                isRecording.set(false);
            }
        };

        countDownTimer.start();
    }


    public void stopRecording() {
        Log.i("SoundRecordActivity", "StopRecording clicked");
        isRecording.set(false);

        File dataFile = new File("fuck.you");
        Uri dataUri = Uri.fromFile(dataFile);

        Intent intent = new Intent();
        intent.setData(dataUri);
        setResult(RESULT_OK, intent);
        finish();
    }
}

package com.clubjevin.festivus;

import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by kevin on 12/7/16.
 */

public class SoundRecordActivity extends AppCompatActivity {
    private long MAX_RECORDING_DURATION_MILLIS = 30 * 1000L;
    private long COUNTER_INCREMENT_MILLIS = 43L;

    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private MediaRecorder recorder = null;
    private File outputFile = null;

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
                startRecording();
            }
        });

        getStopButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });
    }

    private void startRecording() {
        Log.i("SoundRecordActivity", "StartRecording clicked");
        Boolean alreadyRecording = isRecording.getAndSet(true);
        if (alreadyRecording) {
            Log.i("SoundRecordActivity", "Already recording.  Nothing to do.");
            return;
        }

        outputFile = randomFile();
        Log.i("SoundRecordActivity", "Recording sound to file: " + outputFile.getAbsolutePath());
        if (recorder != null) {
            recorder.release();
        }
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(outputFile.getAbsolutePath());
        try {
            recorder.prepare();
            recorder.start();

            startTimer();
        } catch (IOException e) {
            Log.e("giftlist", "io problems while preparing [" +
                    outputFile.getAbsolutePath() + "]: " + e.getMessage());

            recorder.release();
            recorder = null;
            isRecording.set(false);
        }
    }

    private void startTimer() {
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
                stopRecording();
            }
        };

        countDownTimer.start();
    }

    private synchronized void stopRecording() {
        Log.i("SoundRecordActivity", "StopRecording triggered");
        Boolean alreadyRecording = isRecording.getAndSet(false);
        if (!alreadyRecording) {
            Log.i("SoundRecordActivity", "Recording not running.  Nothing to do.");
            return;
        }

        assert(recorder != null);

        recorder.stop();
        recorder.release();
        recorder = null;

        Intent intent = new Intent();
        intent.setData(Uri.fromFile(outputFile));
        setResult(RESULT_OK, intent);
        finish();
    }

    private File randomFile() {
        File sdCardPath = Environment.getExternalStorageDirectory();
        File festivusPath = new File(sdCardPath, "Festivus");

        if(!festivusPath.exists()) {
            Log.i("giftlist", "Making directory: " + festivusPath.getAbsolutePath());
            festivusPath.mkdir();
        } else {
            Log.i("giftlist", "Directory already exists: " + festivusPath.getAbsolutePath());
        }

        return new File(festivusPath, UUID.randomUUID().toString() + ".m4a");
    }
}

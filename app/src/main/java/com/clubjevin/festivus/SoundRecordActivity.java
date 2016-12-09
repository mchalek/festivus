package com.clubjevin.festivus;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by kevin on 12/7/16.
 */

public class SoundRecordActivity extends AppCompatActivity {
    private long MAX_RECORDING_DURATION_MILLIS = 30 * 1000L;
    private long COUNTER_INCREMENT_MILLIS = 43L;

    private MediaFilePlayer mplayer = new MediaFilePlayer();

    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private MediaRecorder recorder = null;

    private File originalFile = null;
    private File disguisedFile = null;

    private CountDownTimer countDownTimer = null;

    public Button getRecordButton() {
        return (Button) findViewById(R.id.record_button);
    }

    public Button getStopButton() {
        return (Button) findViewById(R.id.stop_button);
    }

    public Button getReplayOriginalButton() {
        return (Button) findViewById(R.id.replay_original_button);
    }

    public Button getReplayAlteredButton() {
        return (Button) findViewById(R.id.replay_altered_button);
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

        getStopButton().setVisibility(View.GONE);
        getStopButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

        getReplayOriginalButton().setVisibility(View.GONE);
        getReplayOriginalButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(originalFile != null) {
                    mplayer.play(originalFile);
                }
            }
        });

        getReplayAlteredButton().setVisibility(View.GONE);
        getReplayAlteredButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(disguisedFile != null) {
                    mplayer.play(disguisedFile);
                }
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

        getRecordButton().setVisibility(View.GONE);
        getStopButton().setVisibility(View.VISIBLE);

        originalFile = randomFile();
        Log.i("SoundRecordActivity", "Recording sound to file: " + originalFile.getAbsolutePath());
        if (recorder != null) {
            recorder.release();
        }
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(originalFile.getAbsolutePath());
        try {
            recorder.prepare();
            recorder.start();

            startTimer();
        } catch (IOException e) {
            Log.e("giftlist", "io problems while preparing [" +
                    originalFile.getAbsolutePath() + "]: " + e.getMessage());

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
        if(countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        getStopButton().setVisibility(View.GONE);
        getRecordButton().setVisibility(View.VISIBLE);

        getReplayOriginalButton().setVisibility(View.VISIBLE);
        getReplayAlteredButton().setVisibility(View.VISIBLE);

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

        AsyncTask.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        File recording = getDistortedRecording();
                        if(recording != null) {
                            mplayer.play(recording);
                        }
                    }
                }
        );

        /*
        Intent intent = new Intent();
        intent.setData(Uri.fromFile(originalFile));
        setResult(RESULT_OK, intent);
        finish();
        */
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

        return new File(festivusPath, UUID.randomUUID().toString() + ".ogg");
    }

    private byte[] readSoundFile() {
        byte[] soundFileContents = new byte[(int) originalFile.length()];

        try {
            InputStream input = null;
            input = new FileInputStream(originalFile);
            input.read(soundFileContents, 0, (int) originalFile.length());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return soundFileContents;
    }

    private File getDistortedRecording() {
        URL url = null;
        try {
            url = new URL("http://34.194.97.23:8080/darth");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        Integer responseCode = null;

        Log.i("distort", "Sending recording from " + originalFile.toString() + " to " + url.toString());
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "audio/mp4");
            conn.setRequestProperty("Content-Length", Long.toString(originalFile.length()));
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream out = conn.getOutputStream();
            byte[] bytes = readSoundFile();
            out.write(bytes, 0, (int) originalFile.length());
            out.flush();
            out.close();

            responseCode = conn.getResponseCode();

            InputStream in = conn.getInputStream();
            // Note: this will throw an exception if content-length is not present, or is not a valid integer,
            // and will crash the program
            int inputSize = Integer.parseInt(conn.getHeaderField("content-length"));
            byte[] resultBytes = new byte[inputSize];
            in.read(resultBytes, 0, inputSize);
            in.close();

            disguisedFile = randomFile();
            OutputStream fileOut = new FileOutputStream(disguisedFile);
            fileOut.write(resultBytes, 0, inputSize);
            fileOut.flush();
            fileOut.close();

            Log.i("network", "Wrote " + Integer.toString(inputSize) + " bytes to file: " + disguisedFile.getAbsolutePath());
        } catch(IOException e) {
            Log.v("network", "Failed to connect to url " + url.toString() + ": " + e.toString());
            responseCode = 500;
            disguisedFile = null;
        }

        Log.i("distort", "response code is: " + responseCode.toString());

        return disguisedFile;
    }
}

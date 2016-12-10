package com.clubjevin.festivus;

import android.content.Intent;
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
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kevin on 12/7/16.
 */

public class SoundRecordActivity extends AppCompatActivity {
    private long MAX_RECORDING_DURATION_MILLIS = 30 * 1000L;
    private long COUNTER_INCREMENT_MILLIS = 43L;

    private MediaFilePlayer mplayer;

    private AtomicBoolean isRecording = null;
    private MediaRecorder recorder = null;

    private File originalFile = null;
    private File disguisedFile = null;

    private CountDownTimer countDownTimer = null;

    private AtomicInteger debugModePressCount = new AtomicInteger(0);
    private Integer DEBUG_MODE_UNLOCK_COUNT = 7;

    private Button getRecordButton() {
        return (Button) findViewById(R.id.record_button);
    }

    private Button getStopButton() {
        return (Button) findViewById(R.id.stop_button);
    }

    private Button getAcceptButton() {
        return (Button) findViewById(R.id.accept_button);
    }

    private Button getReplayDisguisedButton() {
        return (Button) findViewById(R.id.replay_altered_button);
    }

    private void hideReplayButtons() {
        getAcceptButton().setVisibility(View.INVISIBLE);
        getReplayDisguisedButton().setVisibility(View.INVISIBLE);
    }

    private void showReplayButtons() {
        getAcceptButton().setVisibility(View.VISIBLE);
        getReplayDisguisedButton().setVisibility(View.VISIBLE);
    }

    private TextView getCounterText() {
        return (TextView) findViewById(R.id.counter_text);
    }

    private TextView getInstructionText() {
        return (TextView) findViewById(R.id.record_instruction_text);
    }

    private void resetInstructionText() {
        getInstructionText().setText("Tap the microphone to record your grievance");
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_soundrecord);

        mplayer = new MediaFilePlayer();
        isRecording = new AtomicBoolean(false);

        getRecordButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(NetworkMonitor.getInstance().isNetworkOk()) {
                    startRecording();
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            "Sorry, could not connect to server for alvin mode",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        resetInstructionText();

        getStopButton().setVisibility(View.INVISIBLE);
        getStopButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

        hideReplayButtons();
        getAcceptButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(originalFile != null && originalFile.exists()) {
                    originalFile.delete();
                }
                Intent intent = new Intent();
                intent.setData(Uri.fromFile(disguisedFile));
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        getCounterText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(debugModePressCount.getAndIncrement() >= DEBUG_MODE_UNLOCK_COUNT) {
                    if (originalFile != null && originalFile.exists()) {
                        mplayer.play(originalFile);
                    }
                }
            }
        });

        getReplayDisguisedButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mplayer.play(disguisedFile);
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

        getInstructionText().setText("Speak your grievance! Press stop button when done");

        getRecordButton().setVisibility(View.INVISIBLE);
        getStopButton().setVisibility(View.VISIBLE);

        hideReplayButtons();

        originalFile = randomFile();
        Log.i("SoundRecordActivity", "Recording sound to file: " + originalFile.getAbsolutePath());
        if (recorder != null) {
            recorder.release();
        }
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioSamplingRate(16000);
        recorder.setAudioChannels(1);
        recorder.setAudioEncodingBitRate(20000);
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

        getStopButton().setVisibility(View.INVISIBLE);
        getRecordButton().setVisibility(View.VISIBLE);
        resetInstructionText();

        AsyncTask.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        File recording = getDistortedRecording();
                        if(recording != null) {
                            mplayer.play(recording);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    showReplayButtons();
                                }
                            });
                        } else {
                            runOnUiThread(
                                    new Runnable() {
                                        public void run() {
                                            Toast.makeText(
                                                    getApplicationContext(),
                                                    "Sorry, could not connect to server for alvin mode",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            );
                        }
                    }
                }
        );
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

    private synchronized File getDistortedRecording() {
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

            {
                OutputStream out = conn.getOutputStream();
                InputStream in = new FileInputStream(originalFile);

                int bytesCopied = IOUtils.copy(in, out);
                out.close();
                in.close();

                Log.i("network", "Uploaded " + Integer.toString(bytesCopied) + " bytes to " + url.toString());
            }

            responseCode = conn.getResponseCode();

            if(responseCode / 100 == 2) {
                InputStream in = conn.getInputStream();

                disguisedFile = randomFile();
                OutputStream out = new FileOutputStream(disguisedFile);

                int bytesCopied = IOUtils.copy(in, out);
                out.close();
                in.close();

                Log.i("network", "Downloaded " + Integer.toString(bytesCopied) + " bytes to file: " + disguisedFile.getAbsolutePath());
            } else {
                Log.i("network", "Received failure success code from server: " + Integer.toString(responseCode));
            }
        } catch(IOException e) {
            Log.v("network", "Failed to connect to url " + url.toString() + ": " + e.toString());
            responseCode = 500;
            disguisedFile = null;
        }

        return disguisedFile;
    }
}

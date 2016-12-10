package com.clubjevin.festivus;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.clubjevin.festivus.data.Grievance;
import com.clubjevin.festivus.data.GrievancesDAO;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//Replaced by android.os.Handler, below.
//import java.util.logging.Handler;

public class MainActivity extends AccelerometerActivity {
    // huge amount of copy+paste from:
    // http://www.androidhive.info/2014/07/android-speech-to-text-tutorial/

    TextToSpeech textToSpeech;
    private Object textToSpeechMutex = new Object();

    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private final int REQ_CODE_SOUND_RECORDING = 101;
    private Handler mHandler = new Handler();
    private GrievancesDAO dao = null;

    private long NETWORK_CHECK_INTERVAL_MILLIS = 5 * 1000L;

    private MediaFilePlayer mplayer = null;

    private SwitchState alvinButton = null;

    public GrievancesDAO getDao() {
        if (dao == null) {
            dao = new GrievancesDAO(this);
        }
        return dao;
    }

    private Boolean getAlvinMode() {
        return alvinButton.getIsChecked();
    }

    private Switch getAlvinSwitch() {
        return (Switch) findViewById(R.id.mode_switch);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getTxtSpeechInput().setText("");

        alvinButton = new SwitchState(false);
        getAlvinSwitch().setOnCheckedChangeListener(alvinButton);

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(getAlvinMode()) {
                    try {
                        promptAlvinInput();
                    } catch(IOException e) {
                        // Not working for some reason, hide alvin mode?
                    }
                }
                else {
                    promptSpeechInput();
                }
            }
        });

        initTextToSpeech();
        mplayer = new MediaFilePlayer();

        setupNetworkMonitor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NetworkMonitor.getInstance().stopScheduler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NetworkMonitor.getInstance().startScheduler();
    }

    private void setupNetworkMonitor() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleWithFixedDelay(
                new Runnable() {
                    public void run() {
                        runOnUiThread( new Runnable() {
                            public void run() {
                                if(NetworkMonitor.getInstance().isNetworkOk()) {
                                    getAlvinSwitch().setVisibility(View.VISIBLE);
                                } else {
                                    getAlvinSwitch().setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }, 0, NETWORK_CHECK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS
        );
    }

    private void initTextToSpeech() {
        synchronized(textToSpeechMutex) {
            assert(textToSpeech == null);

            textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                }
            });
            textToSpeech.setLanguage(Locale.UK); // not sure whether this does anything
        }
    }

    private void stopTextToSpeech() {
        synchronized (textToSpeechMutex) {
            assert(textToSpeech != null);
            textToSpeech.stop();

            textToSpeech = null;
        }
    }

    private TextView getTxtSpeechInput() {
        return (TextView) findViewById(R.id.txtSpeechInput);
    }

    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {
        stopTextToSpeech();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Air your grievance!");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported), Toast.LENGTH_SHORT).show();
        }
    }

    private void promptAlvinInput() throws IOException {
        if(!NetworkMonitor.getInstance().isNetworkOk()) {
            Toast.makeText(
                    getApplicationContext(),
                    "Sorry, could not connect to server for alvin mode",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        stopTextToSpeech();

        Intent intent = new Intent(this, SoundRecordActivity.class);
        try {
            startActivityForResult(intent, REQ_CODE_SOUND_RECORDING);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    a.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        initTextToSpeech();

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT:
                if (resultCode == RESULT_OK && null != data) {

                    String result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
                    verifyTextToSpeech vtts=new verifyTextToSpeech(this, result);
                    vtts.show();

                   //May not be needed, function no longer modifies text.
                    reDrawScreen();
                }
                break;

            case REQ_CODE_SOUND_RECORDING:
                if(resultCode == RESULT_OK && null != data) {
                    Uri uri = data.getData();

                    if(uri != null) {
                        Log.i("dbinsert", "Inserting new grievance with path " + uri.getPath());
                        getDao().insert(new Grievance(System.currentTimeMillis(), null, uri.getPath()));
                    }

                    reDrawScreen();
                }
                break;
        }
    }

    protected void shakeAction() {
        final Grievance grievance = getDao().readRandom();
        if (grievance == null) {
            return;
        }

        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        playGrievance(grievance);
                        //Set screen text to input prompt.
                        reDrawScreen();
                    }
                }
        );
    }

    private void playGrievance(Grievance grievance) {
        synchronized (textToSpeechMutex) {
            switch (grievance.getType()) {
                case TEXT:
                    assert(textToSpeech != null);

                    String grievanceText = grievance.getText();
                    assert(grievanceText != null);

                    textToSpeech.speak(grievanceText, TextToSpeech.QUEUE_ADD, null);
                    getTxtSpeechInput().setText(grievanceText);
                    break;
                case RECORDING:
                    assert(mplayer != null);

                    String recordingPath = grievance.getRecording();
                    assert(recordingPath != null);

                    Log.i("mplayer", "playing recording from file: " + recordingPath);

                    mplayer.play(new File(recordingPath));
                    break;
                default:
                    throw new IllegalArgumentException("Grievance has unrecognized type: " +
                            grievance.getType().name());
            }
        }
    }


    //Waits two seconds then, redraws screen.  Not sure about blocking.
    protected void reDrawScreen() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //setContentView(R.layout.activity_main);;
                                getTxtSpeechInput().setText("Tap George and say\nyour grievance aloud.");
                            }
                        }, 2000);
                    }
                }
        );
    }

}



package com.clubjevin.festivus;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

//Replaced by android.os.Handler, below.
//import java.util.logging.Handler;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
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
import com.clubjevin.festivus.verifyTextToSpeech;

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
                    //getTxtSpeechInput().setText(result);

                    verifyTextToSpeech vtts=new verifyTextToSpeech(this, result);
                    vtts.show();

                    //getDao().insert(new Grievance(System.currentTimeMillis(), result));
                    reDrawScreen();
                }
                break;

            case REQ_CODE_SOUND_RECORDING:
                if(resultCode == RESULT_OK && null != data) {
                    final Uri audioUri = data.getData();
                    Log.v("SOUND_RECORDING", "got audio file URI: " + audioUri.toString());
                    File f = new File(audioUri.getPath());
                    if(f.exists()) {
                        Log.v("SOUND_RECORDING", "GOOD: file exists before deletion: " + f.getAbsolutePath());
                    } else {
                        Log.v("SOUND_RECORDING", "WTF: File does not exist: " + f.getAbsolutePath());
                    }
                    Log.v("SOUND_RECORDING", "deleting file: " + f.getAbsolutePath());
                    //f.delete();
                }
                break;
        }
    }

    protected void shakeAction() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Grievance grievance = getDao().readRandom();
                        if (grievance == null) {
                            return;
                        }

                        String grievanceContent = grievance.getContent();

                        synchronized(textToSpeechMutex) {
                            if(textToSpeech != null) {
                                // TODO: this speak method was deprecated in API 21, so we should check
                                // versions
                                textToSpeech.speak(grievanceContent, TextToSpeech.QUEUE_FLUSH, null);
                            }
                        }
                        getTxtSpeechInput().setText(grievanceContent);
                        //Set screen text to input prompt.
                        reDrawScreen();
                    }
                }
        );
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



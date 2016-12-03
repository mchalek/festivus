package com.clubjevin.festivus;

import android.app.Activity;

import java.util.ArrayList;
import java.util.Locale;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.clubjevin.festivus.data.Grievance;
import com.clubjevin.festivus.data.GrievancesDAO;

public class MainActivity extends AccelerometerActivity {
    // huge amount of copy+paste from:
    // http://www.androidhive.info/2014/07/android-speech-to-text-tutorial/

    TextToSpeech textToSpeech;

    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    private GrievancesDAO dao = null;
    public GrievancesDAO getDao() {
        if(dao == null) {
            dao = new GrievancesDAO(this);
        }
        return dao;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getTxtSpeechInput().setText("");

        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });
        textToSpeech.setLanguage(Locale.UK); // not sure whether this does anything
    }

    private TextView getTxtSpeechInput() {
        return (TextView) findViewById(R.id.txtSpeechInput);
    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    String result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
                    getTxtSpeechInput().setText(result);

                    getDao().insert(new Grievance(System.currentTimeMillis(), result));
                }
                break;
            }

        }
    }

    protected void shakeAction() {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        Grievance grievance = getDao().readRandom();
                        if(grievance == null) {
                            return;
                        }

                        String grievanceContent = grievance.getContent();

                        // TODO: this speak method was deprecated in API 21, so we should check
                        // versions
                        textToSpeech.speak(grievanceContent, TextToSpeech.QUEUE_FLUSH, null);
                        getTxtSpeechInput().setText(grievanceContent);
                    }
                }
        );
    }
}

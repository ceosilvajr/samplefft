package com.fftsample.app;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SampleActivity extends ActionBarActivity {

    private static final int RECORDER_SAMPLE_RATE = 22050;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private boolean isRecording = false;

    private TextView mTxtFrequency;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        Button start = (Button) findViewById(R.id.demo_start_record);
        Button stop = (Button) findViewById(R.id.demo_stop_record);

        TextView mTxtTitle = (TextView) findViewById(R.id.title);
        mTxtTitle.setText("SCORING");

        mTxtFrequency = (TextView) findViewById(R.id.frequency);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });
    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLE_RATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

        recorder.startRecording();

        isRecording = true;

        final short data[] = new short[bufferSize];

        handler = new Handler(Looper.getMainLooper());

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {

                while (isRecording) {
                    recorder.read(data, 0, bufferSize);
                    final int frequency = calculate(RECORDER_SAMPLE_RATE, data);
                    Log.d("FREQUENCY", "--value--" + frequency);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mTxtFrequency.setText("" + frequency);
                        }
                    });
                }
                return null;
            }
        }.execute();

    }

    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
        }

    }

    /**
     * calculate frequency using zero crossings
     */

    private int calculate(int sampleRate, short[] audioData) {

        int numSamples = audioData.length;
        int numCrossing = 0;
        for (int p = 0; p < numSamples - 1; p++) {
            if ((audioData[p] > 0 && audioData[p + 1] <= 0) ||
                    (audioData[p] < 0 && audioData[p + 1] >= 0)) {
                numCrossing++;
            }
        }

        float numSecondsRecorded = (float) numSamples / (float) sampleRate;
        float numCycles = numCrossing / 2;
        final float frequency = numCycles / numSecondsRecorded;

        return (int) frequency;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.menu_main) {
            startActivity(new Intent(this, DevelopmentActivity.class));
            finish();
            return true;
        }
        if (id == R.id.menu_record) {
            startActivity(new Intent(this, DemoActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }
}

package com.fftsample.app;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.silva.audio.WavAudioRecorder;

import java.io.File;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;


public class MainActivity extends ActionBarActivity {

    private int mSampleRate = 44100;
    private int channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
    private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private boolean started = false;

    private TextView textView;

    private double max_index = -1;
    private double max_magnitude = -1;

    private WavAudioRecorder mRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        started = true;

        textView = (TextView) findViewById(R.id.sample_frequency);

        if (mRecorder != null) {
            mRecorder.release();
        }
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                mRecorder = new WavAudioRecorder(true,
                        MediaRecorder.AudioSource.MIC, mSampleRate,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, textView);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {

                File musicDirectory = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "test.wav");

                if (musicDirectory.exists()) {
                    musicDirectory.delete();
                }

                mRecorder.setOutputFile(musicDirectory.getAbsolutePath());

                if (WavAudioRecorder.State.INITIALIZING == mRecorder.getState()) {
                    mRecorder.prepare();
                    mRecorder.start();
                }
            }
        }.execute();

        //new RecordAudio().execute();
    }


    private class RecordAudio extends AsyncTask<Void, double[], Void> {

        @Override
        protected Void doInBackground(Void... params) {

            if (isCancelled()) {
                return null;
            }
            //try {
            final int bufferSize = AudioRecord.getMinBufferSize(mSampleRate,
                    channelConfiguration, audioEncoding);

            AudioRecord audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, mSampleRate,
                    channelConfiguration, audioEncoding, bufferSize);

            final short[] buffer = new short[bufferSize];

            try {
                audioRecord.startRecording();
            } catch (IllegalStateException e) {
                Log.e("Recording failed", e.toString());

            }

            Handler handler = new Handler(getMainLooper());

            while (started) {
                if (isCancelled()) {
                    break;
                }

                audioRecord.read(buffer, 0, bufferSize);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText("" + calculateFFT(buffer, bufferSize));
                    }
                });

            }

            try {
                audioRecord.stop();
            } catch (IllegalStateException e) {
                Log.e("Stop failed", e.toString());

            }

            return null;

        }

    }

    public double calculateFFT(short[] signal, int bufferSize) {

        double[] magnitude = new double[bufferSize / 2];
        DoubleFFT_1D fft = new DoubleFFT_1D(bufferSize);
        double[] fftData = new double[bufferSize * 2];

        double max_index = -1;
        double max_magnitude = -1;

        double frequency;


        for (int i = 0; i < bufferSize; i++) {

            fftData[2 * i] = signal[i];
            fftData[2 * i + 1] = 0;

        }

        fft.complexForward(fftData);

        for (int i = 0, j = 0; i < bufferSize / 2; i += 2, j++) {

            magnitude[j] = Math.sqrt((fftData[2 * i] * fftData[2 * i]) + (fftData[2 * i + 1] * fftData[2 * i + 1]));

        }

        fft.complexInverse(fftData, false);

        for (int i = 0; i < magnitude.length; i++) {
            if (max_magnitude < magnitude[i]) {
                max_magnitude = magnitude[i];
                max_index = i;
            }
        }

        frequency = mSampleRate * max_index / (double) bufferSize;

        Log.d("FFT VALUE", "" + frequency);

        return frequency;

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        return super.onOptionsItemSelected(item);
    }
}

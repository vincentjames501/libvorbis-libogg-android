package com.vorbisdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import org.xiph.vorbis.player.VorbisPlayer;
import org.xiph.vorbis.recorder.VorbisRecorder;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * This activity demonstrates how to use JNI to encode and decode ogg/vorbis audio
 */
public class MainActivity extends Activity {
    /**
     * Logging tag
     */
    private static final String TAG = "MainActivity";

    /**
     * The vorbis player
     */
    private VorbisPlayer vorbisPlayer;

    /**
     * The vorbis recorder
     */
    private VorbisRecorder vorbisRecorder;

    /**
     * Creates and sets our activities layout
     *
     * @param savedInstanceState saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    /**
     * Triggered when the start recording button is pressed
     *
     * @param view the start recording button
     */
    public void startRecording(@SuppressWarnings("unused") View view) {
        //Check if we are already recording
        if (vorbisRecorder == null || vorbisRecorder.isStopped()) {
            //Get location to save to
            File fileToSaveTo = new File(Environment.getExternalStorageDirectory(), "saveTo.ogg");

            //Create our recorder if necessary
            if (vorbisRecorder == null) {
                vorbisRecorder = new VorbisRecorder(fileToSaveTo);
            }

            //Start recording with 44KHz stereo with 0.2 quality
            vorbisRecorder.start(44100, 2, 0.2f);
        }
    }

    /**
     * Stops recording recording
     *
     * @param view the stop recording button
     */
    public void stopRecording(@SuppressWarnings("unused") View view) {
        if (vorbisRecorder != null && vorbisRecorder.isRecording()) {
            vorbisRecorder.stop();
        }
    }

    /**
     * Starts playing the recorded file
     *
     * @param view the start playing button
     */
    public void startPlaying(@SuppressWarnings("unused") View view) {
        //Checks whether the vorbis player is playing
        if (vorbisPlayer == null || vorbisPlayer.isStopped()) {
            //Get file to play
            File fileToPlay = new File(Environment.getExternalStorageDirectory(), "saveTo.ogg");

            //Create the vorbis player if necessary
            if (vorbisPlayer == null) {
                try {
                    vorbisPlayer = new VorbisPlayer(fileToPlay, null);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Failed to find saveTo.ogg", e);
                    Toast.makeText(this, "Failed to find file to play!", 2000).show();
                }
            }

            //Start playing the vorbis audio
            vorbisPlayer.start();
        }
    }

    /**
     * Stop playing the vorbis audio
     *
     * @param view the stop playing button
     */
    public void stopPlaying(@SuppressWarnings("unused") View view) {
        if (vorbisPlayer != null && vorbisPlayer.isPlaying()) {
            vorbisPlayer.stop();
        }
    }
}

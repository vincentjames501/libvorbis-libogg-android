package com.vorbisdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
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
     * The sample rate selection spinner
     */
    private Spinner sampleRateSpinner;

    /**
     * The channel config spinner
     */
    private Spinner chanelConfigSpinner;

    /**
     * Qualities layout to hide when bitrate radio option is selected
     */
    private LinearLayout availableQualitiesLayout;

    /**
     * The quality spinner when with quality radio option is selected
     */
    private Spinner qualitySpinner;

    /**
     * Bitrate layout to hide when quality radio option is selected
     */
    private LinearLayout availableBitratesLayout;

    /**
     * The bitrate spinner when bitrate radio option is selected
     */
    private Spinner bitrateSpinner;

    /**
     * Radio group for encoding types
     */
    private RadioGroup encodingTypeRadioGroup;

    /**
     * Recording handler for callbacks
     */
    private Handler recordingHandler;

    /**
     * Playback handler for callbacks
     */
    private Handler playbackHandler;

    /**
     * Text view to show logged messages
     */
    private TextView logArea;

    /**
     * Creates and sets our activities layout
     *
     * @param savedInstanceState saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        sampleRateSpinner = (Spinner) findViewById(R.id.sample_rate_spinner);
        chanelConfigSpinner = (Spinner) findViewById(R.id.channel_config_spinner);

        encodingTypeRadioGroup = (RadioGroup) findViewById(R.id.encoding_type_radio_group);

        availableQualitiesLayout = (LinearLayout) findViewById(R.id.available_qualities_layout);
        qualitySpinner = (Spinner) findViewById(R.id.quality_spinner);

        availableBitratesLayout = (LinearLayout) findViewById(R.id.available_bitrates_layout);
        bitrateSpinner = (Spinner) findViewById(R.id.bitrate_spinner);

        logArea = (TextView) findViewById(R.id.log_area);

        //Set log area to scroll automatically
        logArea.setMovementMethod(new ScrollingMovementMethod());

        setLoggingHandlers();

        setEncodingTypeCheckListener();

        setDefaultValues();
    }

    /**
     * Sets the recording and playback handlers for message logging
     */
    private void setLoggingHandlers() {
        recordingHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VorbisRecorder.START_ENCODING:
                        logMessage("Starting to encode");
                        break;
                    case VorbisRecorder.STOP_ENCODING:
                        logMessage("Stopping the encoder");
                        break;
                    case VorbisRecorder.UNSUPPORTED_AUDIO_TRACK_RECORD_PARAMETERS:
                        logMessage("You're device does not support this configuration");
                        break;
                    case VorbisRecorder.ERROR_INITIALIZING:
                        logMessage("There was an error initializing.  Try changing the recording configuration");
                        break;
                    case VorbisRecorder.FAILED_FOR_UNKNOWN_REASON:
                        logMessage("The encoder failed for an unknown reason!");
                        break;
                    case VorbisRecorder.FINISHED_SUCCESSFULLY:
                        logMessage("The encoder has finished successfully");
                        break;
                }
            }
        };

        playbackHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VorbisPlayer.PLAYING_FAILED:
                        logMessage("The decoder failed to playback the file, check logs for more details");
                        break;
                    case VorbisPlayer.PLAYING_FINISHED:
                        logMessage("The decoder finished successfully");
                        break;
                    case VorbisPlayer.PLAYING_STARTED:
                        logMessage("Starting to decode");
                        break;
                }
            }
        };
    }

    /**
     * Sets the default values for the input configuration spinners
     */
    private void setDefaultValues() {
        //Set sample rate to '44100'
        sampleRateSpinner.setSelection(4);

        //Set channel configuration to 'Stereo (2 Channels)'
        chanelConfigSpinner.setSelection(1);

        //Set quality to value '1'
        qualitySpinner.setSelection(11);

        //Set bitrate to '128000'
        bitrateSpinner.setSelection(2);
    }

    /**
     * Sets the encoding type check listener to show/hide the quality and bitrate spinners
     */
    private void setEncodingTypeCheckListener() {
        encodingTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId) {
                    case R.id.with_bitrate:
                        availableBitratesLayout.setVisibility(View.VISIBLE);
                        availableQualitiesLayout.setVisibility(View.GONE);
                        break;
                    case R.id.with_quality:
                        availableBitratesLayout.setVisibility(View.GONE);
                        availableQualitiesLayout.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });
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
                vorbisRecorder = new VorbisRecorder(fileToSaveTo, recordingHandler);
            }

            Long sampleRate = Long.parseLong(sampleRateSpinner.getSelectedItem().toString());
            Long channels = (long) (chanelConfigSpinner.getSelectedItemPosition() + 1);

            //Start recording with selected encoding options
            switch (encodingTypeRadioGroup.getCheckedRadioButtonId()) {
                case R.id.with_bitrate:
                    Long bitrate = Long.parseLong(bitrateSpinner.getSelectedItem().toString());
                    vorbisRecorder.start(sampleRate, channels, bitrate);
                    break;
                case R.id.with_quality:
                    Float quality = Float.parseFloat(qualitySpinner.getSelectedItem().toString());
                    vorbisRecorder.start(sampleRate, channels, quality);
                    break;
            }
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
                    vorbisPlayer = new VorbisPlayer(fileToPlay, playbackHandler);
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

    /**
     * Logs a message to the text area and auto scrolls down
     *
     * @param msg the message to log
     */
    private void logMessage(String msg) {
        // append the new string
        logArea.append(msg + "\n");
        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height
        final int scrollAmount = logArea.getLayout().getLineTop(logArea.getLineCount())
                - logArea.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            logArea.scrollTo(0, scrollAmount);
        else
            logArea.scrollTo(0, 0);
    }
}

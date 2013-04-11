package org.xiph.vorbis.recorder;

import android.os.Process;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import org.xiph.vorbis.encoder.EncodeException;
import org.xiph.vorbis.encoder.EncodeFeed;
import org.xiph.vorbis.encoder.VorbisEncoder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The VorbisRecorder is responsible for receiving raw pcm data from the {@link AudioRecord} and feeding that data
 * to the naitive {@link VorbisEncoder}
 * <p/>
 * This class is primarily intended as a demonstration of how to work with the JNI java interface {@link VorbisEncoder}
 * <p/>
 * User: vincent
 * Date: 3/28/13
 * Time: 12:47 PM
 */
public class VorbisRecorder {
    /**
     * The sample rate of the recorder
     */
    private long sampleRate;

    /**
     * The number of channels for the recorder
     */
    private long numberOfChannels;

    /**
     * The output quality of the encoding
     */
    private float quality;

    /**
     * The state of the recorder
     */
    private static enum RecorderState {
        RECORDING, STOPPED
    }

    /**
     * Logging tag
     */
    private static final String TAG = "VorbisRecorder";

    /**
     * The encode feed to feed raw pcm and write vorbis data
     */
    private final EncodeFeed encodeFeed;

    /**
     * The current state of the recorder
     */
    private final AtomicReference<RecorderState> currentState = new AtomicReference<RecorderState>(RecorderState.STOPPED);

    /**
     * Helper class that implements {@link EncodeFeed} that will write the processed vorbis data to a file and will
     * read raw PCM data from an {@link AudioRecord}
     */
    private class FileEncodeFeed implements EncodeFeed {
        /**
         * The file to write to
         */
        private final File fileToSaveTo;

        /**
         * The output stream to write the vorbis data to
         */
        private OutputStream outputStream;

        /**
         * The audio recorder to pull raw pcm data from
         */
        private AudioRecord audioRecorder;

        /**
         * Constructs a file encode feed to write the encoded vorbis output to
         *
         * @param fileToSaveTo the file to save to
         */
        public FileEncodeFeed(File fileToSaveTo) {
            if (fileToSaveTo == null) {
                throw new IllegalArgumentException("File to save to must not be null");
            }
            this.fileToSaveTo = fileToSaveTo;
        }

        @Override
        public long readPCMData(byte[] pcmDataBuffer, int amountToRead) {
            //If we are no longer recording, return 0 to let the native encoder know
            if (isStopped()) {
                return 0;
            }

            //Otherwise read from the audio recorder
            int read = audioRecorder.read(pcmDataBuffer, 0, amountToRead);
            switch (read) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    Log.e(TAG, "Invalid operation on AudioRecord object");
                    return 0;
                case AudioRecord.ERROR_BAD_VALUE:
                    Log.e(TAG, "Invalid value returned from audio recorder");
                    return 0;
                case -1:
                    return 0;
                default:
                    //Successfully read from audio recorder
                    return read;
            }
        }

        @Override
        public int writeVorbisData(byte[] vorbisData, int amountToWrite) {
            //If we have data to write and we are recording, write the data
            if (vorbisData != null && amountToWrite > 0 && outputStream != null && isRecording()) {
                try {
                    //Write the data to the output stream
                    outputStream.write(vorbisData, 0, amountToWrite);
                    return amountToWrite;
                } catch (IOException e) {
                    //Failed to write to the file
                    Log.e(TAG, "Failed to write data to file, stopping recording", e);
                    stop();
                }
            }
            //Otherwise let the native encoder know we are done
            return 0;
        }

        @Override
        public void stop() {
            if (isRecording()) {
                //Set our state to stopped
                currentState.set(RecorderState.STOPPED);

                //Close the output stream
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to close output stream", e);
                    }
                    outputStream = null;
                }

                //Stop and clean up the audio recorder
                if (audioRecorder != null) {
                    audioRecorder.stop();
                    audioRecorder.release();
                }
            }
        }

        @Override
        public void start() {
            if (isStopped()) {
                //Creates the audio recorder
                int channelConfiguration = numberOfChannels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
                int bufferSize = AudioRecord.getMinBufferSize((int) sampleRate, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT);
                audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, (int) sampleRate, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

                //Start recording
                currentState.set(RecorderState.RECORDING);
                audioRecorder.startRecording();

                //Create the output stream
                if (outputStream == null) {
                    try {
                        outputStream = new BufferedOutputStream(new FileOutputStream(fileToSaveTo));
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Failed to write to file", e);
                    }
                }
            }
        }
    }

    /**
     * Helper class that implements {@link EncodeFeed} that will write the processed vorbis data to an output stream
     * and will read raw PCM data from an {@link AudioRecord}
     */
    private class OutputStreamEncodeFeed implements EncodeFeed {
        /**
         * The output stream to write the vorbis data to
         */
        private OutputStream outputStream;

        /**
         * The audio recorder to pull raw pcm data from
         */
        private AudioRecord audioRecorder;

        /**
         * Constructs a file encode feed to write the encoded vorbis output to
         *
         * @param outputStream the {@link OutputStream} to write the encoded information to
         */
        public OutputStreamEncodeFeed(OutputStream outputStream) {
            if (outputStream == null) {
                throw new IllegalArgumentException("The output stream must not be null");
            }
            this.outputStream = outputStream;
        }

        @Override
        public long readPCMData(byte[] pcmDataBuffer, int amountToRead) {
            //If we are no longer recording, return 0 to let the native encoder know
            if (isStopped()) {
                return 0;
            }

            //Otherwise read from the audio recorder
            int read = audioRecorder.read(pcmDataBuffer, 0, amountToRead);
            switch (read) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    Log.e(TAG, "Invalid operation on AudioRecord object");
                    return 0;
                case AudioRecord.ERROR_BAD_VALUE:
                    Log.e(TAG, "Invalid value returned from audio recorder");
                    return 0;
                case -1:
                    return 0;
                default:
                    //Successfully read from audio recorder
                    return read;
            }
        }

        @Override
        public int writeVorbisData(byte[] vorbisData, int amountToWrite) {
            //If we have data to write and we are recording, write the data
            if (vorbisData != null && amountToWrite > 0 && outputStream != null && isRecording()) {
                try {
                    //Write the data to the output stream
                    outputStream.write(vorbisData, 0, amountToWrite);
                    return amountToWrite;
                } catch (IOException e) {
                    //Failed to write to the file
                    Log.e(TAG, "Failed to write data to file, stopping recording", e);
                    stop();
                }
            }
            //Otherwise let the native encoder know we are done
            return 0;
        }

        @Override
        public void stop() {
            if (isRecording()) {
                //Set our state to stopped
                currentState.set(RecorderState.STOPPED);

                //Close the output stream
                if (outputStream != null) {
                    try {
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to close output stream", e);
                    }
                    outputStream = null;
                }

                //Stop and clean up the audio recorder
                if (audioRecorder != null) {
                    audioRecorder.stop();
                    audioRecorder.release();
                }
            }
        }

        @Override
        public void start() {
            if (isStopped()) {
                //Creates the audio recorder
                int channelConfiguration = numberOfChannels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
                int bufferSize = AudioRecord.getMinBufferSize((int) sampleRate, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT);
                audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, (int) sampleRate, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

                //Start recording
                currentState.set(RecorderState.RECORDING);
                audioRecorder.startRecording();
            }
        }
    }

    /**
     * Constructs a recorder that will record an ogg file
     *
     * @param fileToSaveTo the file to save to
     */
    public VorbisRecorder(File fileToSaveTo) {
        if (fileToSaveTo == null) {
            throw new IllegalArgumentException("File to play must not be null.");
        }

        //Delete the file if it exists
        if (fileToSaveTo.exists()) {
            fileToSaveTo.deleteOnExit();
        }

        this.encodeFeed = new FileEncodeFeed(fileToSaveTo);
    }

    /**
     * Constructs a recorder that will record an ogg output stream
     *
     * @param streamToWriteTo the output stream to write the encoded information to
     */
    public VorbisRecorder(OutputStream streamToWriteTo) {
        if (streamToWriteTo == null) {
            throw new IllegalArgumentException("File to play must not be null.");
        }

        this.encodeFeed = new OutputStreamEncodeFeed(streamToWriteTo);
    }

    /**
     * Constructs a vorbis recorder with a custom {@link EncodeFeed}
     *
     * @param encodeFeed the custom {@link EncodeFeed}
     */
    public VorbisRecorder(EncodeFeed encodeFeed) {
        if (encodeFeed == null) {
            throw new IllegalArgumentException("Encode feed must not be null.");
        }

        this.encodeFeed = encodeFeed;
    }

    /**
     * Starts the recording/encoding process
     *
     * @param sampleRate       the rate to sample the audio at, should be greater than <code>0</code>
     * @param numberOfChannels the nubmer of channels, must only be <code>1/code> or <code>2</code>
     * @param quality          the quality at which to encode, must be between <code>-0.1</code> and <code>1.0</code>
     */
    @SuppressWarnings("all")
    public synchronized void start(long sampleRate, long numberOfChannels, float quality) {
        if (isStopped()) {
            if (numberOfChannels != 1 && numberOfChannels != 2) {
                throw new IllegalArgumentException("Channels can only be one or two");
            }
            if (sampleRate <= 0) {
                throw new IllegalArgumentException("Invalid sample rate, must be above 0");
            }
            if (quality < -0.1f || quality > 1.0f) {
                throw new IllegalArgumentException("Quality must be between -0.1 and 1.0");
            }

            this.sampleRate = sampleRate;
            this.numberOfChannels = numberOfChannels;
            this.quality = quality;

            //Starts the recording process
            new Thread(new AsyncEncoding()).start();
        }
    }

    /**
     * Stops the audio recorder and notifies the {@link EncodeFeed}
     */
    public synchronized void stop() {
        encodeFeed.stop();
    }

    /**
     * Starts the encoding process in a background thread
     */
    private class AsyncEncoding implements Runnable {
        @Override
        public void run() {
            //Start the native encoder
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                int result = VorbisEncoder.startEncoding(sampleRate, numberOfChannels, quality, encodeFeed);
                if (result == EncodeFeed.SUCCESS) {
                    Log.d(TAG, "Encoder successfully finished");
                }
            } catch (EncodeException e) {
                switch (e.getErrorCode()) {
                    case EncodeException.ERROR_INITIALIZING:
                        Log.e(TAG, "There was an error initializing the native encoder");
                        break;
                }
            }
        }
    }

    /**
     * Checks whether the recording is currently recording
     *
     * @return <code>true</code> if recording, <code>false</code> otherwise
     */
    public synchronized boolean isRecording() {
        return currentState.get() == RecorderState.RECORDING;
    }

    /**
     * Checks whether the recording is currently stopped (not recording)
     *
     * @return <code>true</code> if stopped, <code>false</code> otherwise
     */
    public synchronized boolean isStopped() {
        return currentState.get() == RecorderState.STOPPED;
    }
}

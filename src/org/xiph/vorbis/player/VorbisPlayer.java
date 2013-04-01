package org.xiph.vorbis.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import org.xiph.vorbis.decoder.DecodeException;
import org.xiph.vorbis.decoder.DecodeFeed;
import org.xiph.vorbis.decoder.DecodeStreamInfo;
import org.xiph.vorbis.decoder.VorbisDecoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The VorbisPlayer is responsible for decoding a vorbis bitsream into raw PCM data to play to an {@link AudioTrack}
 * <p/>
 * <p/>
 * <p/>
 * This class is primarily intended as a demonstration of how to work with the JNI java interface {@link VorbisDecoder}
 * <p/>
 * User: vincent
 * Date: 3/28/13
 * Time: 10:17 AM
 */
public class VorbisPlayer implements Runnable {

    /**
     * Playing state which can either be stopped, playing, or reading the header before playing
     */
    private static enum PlayerState {PLAYING, STOPPED, READING_HEADER}

    /**
     * Logging tag
     */
    private static final String TAG = "VorbisPlayer";

    /**
     * The decode feed to read and write pcm/vorbis data respectively
     */
    private final DecodeFeed decodeFeed;

    /**
     * The audio track to write the raw pcm bytes to
     */
    private AudioTrack audioTrack;

    /**
     * Current state of the vorbis player
     */
    private AtomicReference<PlayerState> currentState = new AtomicReference<PlayerState>(PlayerState.STOPPED);

    /**
     * Custom class to easily decode from a file and write to an {@link AudioTrack}
     */
    private class FileDecodeFeed implements DecodeFeed {
        /**
         * The input stream to decode from
         */
        private InputStream inputStream;

        /**
         * The file to decode ogg/vorbis data from
         */
        private final File fileToDecode;

        /**
         * Creates a decode feed that reads from a file and writes to an {@link AudioTrack}
         *
         * @param fileToDecode the file to decode
         */
        private FileDecodeFeed(File fileToDecode) throws FileNotFoundException {
            if (fileToDecode == null) {
                throw new IllegalArgumentException("File to decode must not be null.");
            }
            this.fileToDecode = fileToDecode;
        }

        @Override
        public synchronized int readVorbisData(byte[] buffer, int amountToWrite) {
            //If the player is not playing or reading the header, return 0 to end the native decode method
            if (currentState.get() == PlayerState.STOPPED) {
                return 0;
            }

            //Otherwise read from the file
            try {
                int read = inputStream.read(buffer, 0, amountToWrite);
                return read == -1 ? 0 : read;
            } catch (IOException e) {
                //There was a problem reading from the file
                Log.e(TAG, "Failed to read vorbis data from file.  Aborting.", e);
                stop();
                return 0;
            }
        }

        @Override
        public synchronized void writePCMData(short[] pcmData, int amountToRead) {
            //If we received data and are playing, write to the audio track
            if (pcmData != null && amountToRead > 0 && audioTrack != null && isPlaying()) {
                audioTrack.write(pcmData, 0, amountToRead);
            }
        }

        @Override
        public void stop() {
            if (isPlaying() || isReadingHeader()) {
                //Closes the file input stream
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to close file input stream", e);
                    }
                    inputStream = null;
                }

                //Stop the audio track
                if (audioTrack != null) {
                    audioTrack.stop();
                    audioTrack.release();
                    audioTrack = null;
                }
            }

            //Set our state to stopped
            currentState.set(PlayerState.STOPPED);
        }

        @Override
        public void start(DecodeStreamInfo decodeStreamInfo) {
            if (currentState.get() != PlayerState.READING_HEADER) {
                throw new IllegalStateException("Must read header first!");
            }
            if (decodeStreamInfo.getChannels() != 1 && decodeStreamInfo.getChannels() != 2) {
                throw new IllegalArgumentException("Channels can only be one or two");
            }
            if (decodeStreamInfo.getSampleRate() <= 0) {
                throw new IllegalArgumentException("Invalid sample rate, must be above 0");
            }

            //Create the audio track
            int channelConfiguration = decodeStreamInfo.getChannels() == 1 ? AudioFormat.CHANNEL_CONFIGURATION_MONO : AudioFormat.CHANNEL_CONFIGURATION_STEREO;
            int minSize = AudioTrack.getMinBufferSize((int) decodeStreamInfo.getSampleRate(), channelConfiguration, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, (int) decodeStreamInfo.getSampleRate(), channelConfiguration, AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);
            audioTrack.play();

            //We're starting to read actual content
            currentState.set(PlayerState.PLAYING);
        }

        @Override
        public void startReadingHeader() {
            if (inputStream == null && isStopped()) {
                try {
                    inputStream = new BufferedInputStream(new FileInputStream(fileToDecode));
                    currentState.set(PlayerState.READING_HEADER);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Failed to find file to decode", e);
                    stop();
                }
            }
        }
    }

    /**
     * Constructs a new instance of the player with default parameters other than it will decode from a file
     *
     * @param fileToPlay the file to play
     * @throws FileNotFoundException thrown if the file could not be located/opened to playing
     */
    public VorbisPlayer(File fileToPlay) throws FileNotFoundException {
        if (fileToPlay == null) {
            throw new IllegalArgumentException("File to play must not be null.");
        }
        this.decodeFeed = new FileDecodeFeed(fileToPlay);
    }

    /**
     * Constructs a player with a custom {@link DecodeFeed}
     *
     * @param decodeFeed the custom decode feed
     */
    public VorbisPlayer(DecodeFeed decodeFeed) {
        if (decodeFeed == null) {
            throw new IllegalArgumentException("Decode feed must not be null.");
        }
        this.decodeFeed = decodeFeed;
    }

    /**
     * Starts the audio recorder with a given sample rate and channels
     */
    @SuppressWarnings("all")
    public synchronized void start() {
        if (isStopped()) {
            new Thread(this).start();
        }
    }

    /**
     * Stops the player and notifies the decode feed
     */
    public synchronized void stop() {
        decodeFeed.stop();
    }

    @Override
    public void run() {
        //Start the native decoder
        try {
            int result = VorbisDecoder.startDecoding(decodeFeed);
            if (result == DecodeFeed.SUCCESS) {
                Log.d(TAG, "Successfully finished decoding");
            }
        } catch (DecodeException e) {
            switch (e.getErrorCode()) {
                case DecodeException.INVALID_OGG_BITSTREAM:
                    Log.e(TAG, "Invalid ogg bitstream error received");
                    break;
                case DecodeException.ERROR_READING_FIRST_PAGE:
                    Log.e(TAG, "Error reading first page error received");
                    break;
                case DecodeException.ERROR_READING_INITIAL_HEADER_PACKET:
                    Log.e(TAG, "Error reading initial header packet error received");
                    break;
                case DecodeException.NOT_VORBIS_HEADER:
                    Log.e(TAG, "Not a vorbis header error received");
                    break;
                case DecodeException.CORRUPT_SECONDARY_HEADER:
                    Log.e(TAG, "Corrupt secondary header error received");
                    break;
                case DecodeException.PREMATURE_END_OF_FILE:
                    Log.e(TAG, "Premature end of file error received");
                    break;
            }
        }
    }

    /**
     * Checks whether the player is currently playing
     *
     * @return <code>true</code> if playing, <code>false</code> otherwise
     */
    public synchronized boolean isPlaying() {
        return currentState.get() == PlayerState.PLAYING;
    }

    /**
     * Checks whether the player is currently stopped (not playing)
     *
     * @return <code>true</code> if playing, <code>false</code> otherwise
     */
    public synchronized boolean isStopped() {
        return currentState.get() == PlayerState.STOPPED;
    }

    /**
     * Checks whether the player is currently reading the header
     *
     * @return <code>true</code> if reading the header, <code>false</code> otherwise
     */
    public synchronized boolean isReadingHeader() {
        return currentState.get() == PlayerState.READING_HEADER;
    }
}

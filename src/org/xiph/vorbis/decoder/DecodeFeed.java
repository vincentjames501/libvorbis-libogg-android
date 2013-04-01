package org.xiph.vorbis.decoder;

/**
 * A feed interface which raw PCM data will be written to and encoded vorbis data will be read from
 * User: vincent
 * Date: 3/27/13
 * Time: 2:11 PM
 */
public interface DecodeFeed {
    /**
     * Everything was a success
     */
    public static final int SUCCESS = 0;

    /**
     * Triggered from the native {@link VorbisDecoder} that is requesting to read the next bit of vorbis data
     *
     * @param buffer        the buffer to write to
     * @param amountToWrite the amount of vorbis data to write
     * @return the amount actually written
     */
    public int readVorbisData(byte[] buffer, int amountToWrite);

    /**
     * Triggered from the native {@link VorbisDecoder} that is requesting to write the next bit of raw PCM data
     *
     * @param pcmData      the raw pcm data
     * @param amountToRead the amount available to read in the buffer
     */
    public void writePCMData(short[] pcmData, int amountToRead);

    /**
     * To be called when decoding has completed
     */
    public void stop();

    /**
     * Puts the decode feed in the reading header state
     */
    public void startReadingHeader();

    /**
     * To be called when decoding has started
     *
     * @param decodeStreamInfo the stream information of what's about to be played
     */
    public void start(DecodeStreamInfo decodeStreamInfo);
}

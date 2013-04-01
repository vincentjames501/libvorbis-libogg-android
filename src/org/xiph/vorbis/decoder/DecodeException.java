package org.xiph.vorbis.decoder;

/**
 * An exception class that is thrown by the native {@link VorbisDecoder} when there is a problem decoding the bitstream
 * User: vincent
 * Date: 4/1/13
 * Time: 8:27 AM
 */
public class DecodeException extends Exception {
    /**
     * The bitstream is not ogg
     */
    public static final int INVALID_OGG_BITSTREAM = -21;

    /**
     * Failed to read first page
     */
    public static final int ERROR_READING_FIRST_PAGE = -22;

    /**
     * Failed reading the initial header packet
     */
    public static final int ERROR_READING_INITIAL_HEADER_PACKET = -23;

    /**
     * The data is not a vorbis header
     */
    public static final int NOT_VORBIS_HEADER = -24;

    /**
     * The secondary header is corrupt
     */
    public static final int CORRUPT_SECONDARY_HEADER = -25;

    /**
     * Reached a premature end of file
     */
    public static final int PREMATURE_END_OF_FILE = -26;



    /**
     * The thrown error code type
     */
    private final int errorCode;

    /**
     * Decode exception constructor thrown by the native {@link VorbisDecoder}
     *
     * @param errorCode the message error code
     */
    public DecodeException(int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code that should match an above constant
     *
     * @return the error code thrown by the native decoder
     */
    public int getErrorCode() {
        return errorCode;
    }
}

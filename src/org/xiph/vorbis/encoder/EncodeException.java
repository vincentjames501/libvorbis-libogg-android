package org.xiph.vorbis.encoder;

/**
 * An exception class that is thrown by the native {@link VorbisEncoder} when there is a problem encoding
 * User: vincent
 * Date: 4/1/13
 * Time: 8:27 AM
 */
public class EncodeException extends Exception {
    /**
     * If there was an error initializing the encoder
     */
    public static final int ERROR_INITIALIZING = -44;

    /**
     * The thrown error code type
     */
    private final int errorCode;

    /**
     * Encode exception constructor thrown by the native {@link VorbisEncoder}
     *
     * @param errorCode the message error code
     */
    public EncodeException(int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code that should match an above constant
     *
     * @return the error code thrown by the native encoder
     */
    public int getErrorCode() {
        return errorCode;
    }
}

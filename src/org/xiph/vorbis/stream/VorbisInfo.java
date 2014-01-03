package org.xiph.vorbis.stream;

/**
 * A class used to pass vorbis file info to the encoder and find out what kind of file was received by the decoder.
 * 
 * Currently, the vorbis file encoder/decoder only supports 16-bit samples. There are no plans by me to ever add 8-bit. Defaults are set to whatever I
 * wanted to use for the project i wrote this for.
 * 
 */
public class VorbisInfo {
	/**
	 * The number of channels to be encoded. For your sake, here are the official channel positions for the first five according to Xiph.org. one
	 * channel - the stream is monophonic two channels - the stream is stereo. channel order: left, right three channels - the stream is a 1d-surround
	 * encoding. channel order: left, center, right four channels - the stream is quadraphonic surround. channel order: front left, front right, rear
	 * left, rear right five channels - the stream is five-channel surround. channel order: front left, center, front right, rear left, rear right
	 */
	public int		channels	= 1;

	/**
	 * The number of samples per second of pcm data.
	 */
	public int		sampleRate	= 44100;

	/**
	 * The recording quality of the encoding. This is not currently set for the decoder. The range goes from -.1 (worst) to 1 (best)
	 */
	public float	quality		= 0.4f;

	/**
	 * the total number of samples from the recording. This field means nothing to the encoder.
	 */
	public long		length;

}

package org.xiph.vorbis.stream;

import java.io.IOException;

/**
 * Converts incoming PCM Audio Data into OGG data into a file. This will be implemented using the open source BSD-licensed stuff from Xiph.org.
 * 
 * NOTE: This implementation has a limitation of MAX_STREAMS concurrent output streams. When i wrote this, it was set to 8. Check in
 * vorbis-fileoutputstream.c to see what it is set to.
 * 
 */
public class VorbisFileOutputStream extends AudioOutputStream {
	// The index into native memory where the ogg stream info is stored.
	private final int			oggStreamIdx;
	private VorbisInfo			info;
	private static final int	VORBIS_BLOCK_SIZE	= 1024;

	static {
		System.loadLibrary("ogg");
		System.loadLibrary("vorbis");
		System.loadLibrary("vorbis-stream");
	}

	public VorbisFileOutputStream(String fname, VorbisInfo s) throws IOException {
		info = s;
		oggStreamIdx = this.create(fname, s);
	}

	public VorbisFileOutputStream(String fname) throws IOException {
		oggStreamIdx = this.create(fname, new VorbisInfo());
	}

	@Override
	public void close() throws IOException {
		this.closeStreamIdx(this.oggStreamIdx);
	}

	/**
	 * Write PCM data to ogg. This assumes that you pass your streams in interleaved.
	 * 
	 * @param buffer
	 * @param offset
	 * @param length
	 * @return
	 * @throws IOException
	 */
	@Override
	public void write(final short[] buffer, int offset, int length) throws IOException {
		this.writeStreamIdx(this.oggStreamIdx, buffer, offset, length);
	}

	private native int writeStreamIdx(int idx, short[] pcmdata, int offset, int size) throws IOException;

	private native void closeStreamIdx(int idx) throws IOException;

	private native int create(String path, VorbisInfo s) throws IOException;

	@Override
	public int getSampleRate() {
		return info.sampleRate;
	}
}

package org.xiph.vorbis.stream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public abstract class AudioOutputStream extends OutputStream implements Closeable {

	@Override
	public void write(final int buf) throws IOException {
		short[] buffer = new short[1];
		buffer[0] = (short) buf;
		this.write(buffer, 0, buffer.length);
	}

	public void write(final short[] buffer) throws IOException {
		this.write(buffer, 0, buffer.length);
	}

	public abstract void write(final short[] buffer, int offset, int length)
			throws IOException;

	public abstract int getSampleRate();

	@Override
	public abstract void close() throws IOException;
}

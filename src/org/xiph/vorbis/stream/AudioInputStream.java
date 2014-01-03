package org.xiph.vorbis.stream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public abstract class AudioInputStream extends InputStream implements Closeable {
	public abstract void close() throws IOException;
	
	public int read(short[] pcmBuffer) throws IOException {
		return this.read(pcmBuffer, 0, pcmBuffer.length);
	}
	
	public abstract int read(short[] pcmBuffer, int offset, int length) throws IOException;
	

	@Override
	public int read() throws IOException {
		short buf[] = new short[1];
		if ( this.read(buf, 0, 1) == -1) return -1;
		else return buf[0];	
	}
}

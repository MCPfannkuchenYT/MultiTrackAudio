package multitrackaudio.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * This is a slightly modified ByteArrayOutputStream which compresses the final array into a .wav file
 * @author Pancake
 */
public class WAVEOutputStream extends ByteArrayOutputStream {

	/**
	 * Output File
	 */
	private final File file;
	
	/**
	 * Audio format
	 */
	private final AudioFormat format;
	
	/**
	 * Initializes a new WAVEOutputStream with a given file as output file
	 * @param file WAV File
	 */
	public WAVEOutputStream(String path, AudioFormat format) {
		this.file = new File(path);
		this.format = format;
	}
	
	/**
	 * Writes audio data to the stream and resamples it
	 * @param data Audio Data
	 * @throws IOException IO Exceptions
	 */
	public void writeShortArray(short[] data) throws IOException {
		ByteBuffer temp = ByteBuffer.allocate(data.length*2);
		temp.asShortBuffer().put(data);
		this.write(temp.array());
	}
	
	@Override
	public void close() throws IOException {
		super.close();
		// Create new audio input stream from data
		final byte[] data = this.toByteArray();
		final AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(data), this.format, data.length/(this.format.getSampleSizeInBits()/8));
		// Write file
		AudioSystem.write(stream, Type.WAVE, this.file);
	}
	
}

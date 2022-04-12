package multitrackaudio.audio;

import javax.sound.sampled.AudioFormat;

import multitrackaudio.audio.AudioDeviceDiscovery.AudioDevice;
import multitrackaudio.io.WAVEOutputStream;
import xt.audio.Enums.XtSample;
import xt.audio.Structs.XtBuffer;
import xt.audio.Structs.XtBufferSize;
import xt.audio.Structs.XtChannels;
import xt.audio.Structs.XtDeviceStreamParams;
import xt.audio.Structs.XtFormat;
import xt.audio.Structs.XtMix;
import xt.audio.Structs.XtStreamParams;
import xt.audio.XtDevice;
import xt.audio.XtSafeBuffer;
import xt.audio.XtStream;

/**
 * Resamples bits and samples to 44100hz, 16 bit int, stereo, 
 * @author Pancake
 */
public class AudioResampler {

	private XtDevice device;
	private XtMix mix;
	private XtChannels channels = new XtChannels(2, 0, 0, 0);
	private XtFormat format;
	private WAVEOutputStream destination;
	private XtBufferSize size;
	private XtStreamParams streamParams;
	private XtDeviceStreamParams deviceParams;
	private XtStream stream;
	private XtSafeBuffer buffer;
	private AudioFormat javaformat;
	
	/**
	 * Prepares the resampler
	 * @param source Source to mix
	 * @param filename Destination of the mix
	 */
	public AudioResampler(AudioDevice source, String filename) {
		this.device = source.device();
		this.mix = this.device.getMix().get();
		this.format = new XtFormat(this.mix, this.channels);
		this.javaformat = new AudioFormat(this.mix.rate, 16, this.channels.inputs, true, true);
		this.destination = new WAVEOutputStream(filename, this.javaformat);
		this.size = this.device.getBufferSize(this.format);
		this.streamParams = new XtStreamParams(true, this::onBuffer, null, null);
		this.deviceParams = new XtDeviceStreamParams(this.streamParams, this.format, this.size.current);
		this.stream = this.device.openStream(this.deviceParams, null);
		this.buffer = XtSafeBuffer.register(this.stream);
		System.out.println("Successfully initialized \"" + source.name() + "\" with " + this.mix.rate + " samples on " + this.mix.sample);
	}
	
	/**
	 * Resamples incoming buffer data into the output stream
	 * @param stream Input Stream
	 * @param buffer Input Buffer
	 * @param user null
	 * @return Status
	 * @throws Exception IO Exception
	 */
	private int onBuffer(XtStream stream, XtBuffer buffer, Object user) throws Exception {
		XtSafeBuffer safe = XtSafeBuffer.get(stream);
		safe.lock(buffer);

		int len = this.channels.inputs * buffer.frames;
		if (this.mix.sample == XtSample.FLOAT32) {
			float[] input = (float[]) safe.getInput();
			
			// Sample to 16 bit audio
			short[] output = new short[len];
			for (int i = 0; i < len; i++)
				output[i] = (short) (input[i] * ((float) Short.MAX_VALUE));
				
			this.destination.writeShortArray(output);
		} else if (this.mix.sample == XtSample.INT32) {
			int[] input = (int[]) safe.getInput();
			
			// Sample to 16 bit audio
			short[] output = new short[len];
			for (int i = 0; i < len; i++)
				output[i] = (short) (input[i] / Short.MAX_VALUE);
			
			this.destination.writeShortArray(output);
		} else if (this.mix.sample == XtSample.INT16) {
			this.destination.writeShortArray((short[]) safe.getInput());
		} else if (this.mix.sample == XtSample.UINT8) {
			byte[] input = (byte[]) safe.getInput();
			
			// Sample to 16 bit audio
			short[] output = new short[len];
			for (int i = 1; i < len; i++)
				output[i] = (short) (((short) input[i] - 128) << 8);
			
			this.destination.writeShortArray(output);
		} else if (this.mix.sample == XtSample.INT24) {
			byte[] input = (byte[]) safe.getInput();
			
			byte[] output = new byte[len*2];
			int y = 0;
			for (int i = 0; i < len*3; i+=3) {
				output[y++] = input[i+2];
				output[y++] = input[i+1];
			}
			
			this.destination.write(output);
		} 
		
		safe.unlock(buffer);
		return 0;
	}
	
	/**
	 * Launches the audio resampler asynchronously
	 */
	public void launch() {
		this.stream.start();
	}
	
	/**
	 * Shuts down the audio resampler
	 * @throws Exception Exception closing the streams
	 */
	public void shutdown() throws Exception {
		this.stream.stop();
		this.buffer.close();
		this.stream.close();
		this.destination.flush();
		this.destination.close();
	}
	
}

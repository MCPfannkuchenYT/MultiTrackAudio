package multitrackaudio;


import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.sound.sampled.SourceDataLine;

import com.synthbot.jasiohost.AsioChannel;
import com.synthbot.jasiohost.AsioDriver;
import com.synthbot.jasiohost.AsioDriverListener;

/**
 * Broadcasts my ASIO Keyboard to any output stream.
 * Expected Format: Signed 16 bit PCM Little-endian 1 channel 44100hz
 * @author Pancake
 */
public class ASIOHandler {

	private SourceDataLine stream;
	private OutputStream stream2;
	private AsioDriver asioDriver;
	private long time;
	
	/**
	 * Initializes a new ASIO Writer to write keyboard audio data to the OutputStream
	 * @param stream
	 * @param stream2 
	 * @param time 
	 */
	public ASIOHandler(SourceDataLine stream, OutputStream stream2, long time) {
		this.stream = stream;
		this.stream2 = stream2;
		this.time = time;
	}
	
	public void start() {
		// Prepare ASIO Device
		asioDriver = AsioDriver.getDriver(AsioDriver.getDriverNames().get(1));
		int bufferSize = asioDriver.getBufferPreferredSize();
		
		// Add listener for ASIO packets
		asioDriver.addAsioDriverListener(new AsioDriverListener() {
			@Override public void sampleRateDidChange(double sampleRate) {}
			@Override public void resyncRequest() {}
			@Override public void resetRequest() {}
			@Override public void latenciesChanged(int inputLatency, int outputLatency) {}
			@Override public void bufferSizeChanged(int bufferSize) {}

			@Override
			public void bufferSwitch(long sampleTime, long samplePosition, Set<AsioChannel> activeChannels) {
				Iterator<AsioChannel> it = activeChannels.iterator();
				AsioChannel channel = it.next();
				AsioChannel channel1 = it.next();
				
				// Read audio from channels
				int[] data = new int[bufferSize];
				channel.getByteBuffer().asIntBuffer().get(data);
				int[] data2 = new int[bufferSize];
				channel1.getByteBuffer().asIntBuffer().get(data2);
				
				// Convert 32 bit audio into 16 bit audio
				ByteBuffer b = ByteBuffer.allocate(bufferSize*4);
				for (int i = 0; i < data.length; i++) {
					b.putShort(Short.reverseBytes((short) (data[i] >> 16)));
					b.putShort(Short.reverseBytes((short) (data2[i] >> 16)));
				}
				
				// Write audio data
				stream.write(b.array(), 0, bufferSize*4);
				try {
					stream2.write(b.array(), 0, bufferSize*4);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		});
		
		// Setup ASIO Channels
		Set<AsioChannel> activeChannels = new HashSet<AsioChannel>();
		activeChannels.add(asioDriver.getChannelInput(0));
		activeChannels.add(asioDriver.getChannelInput(1));
		asioDriver.createBuffers(activeChannels);

		// Launch ASIO Device Writer when it's supposed to
		while (System.currentTimeMillis() < time) {}
		asioDriver.start();
	}
	
	/**
	 * Closes the stream
	 * @throws Exception
	 */
	public void stop() throws Exception {
		stream.close();
		stream2.close();
		asioDriver.shutdownAndUnloadDriver();
	}
	
}

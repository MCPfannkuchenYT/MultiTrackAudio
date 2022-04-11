package multitrackaudio;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;

public class SimpleRedirect {

	public static final AudioFormat FORMAT = new AudioFormat(44100.0f, 16, 2, true, false);
	
	public static void main(String[] args) throws Exception {
		// Add new audio track
		SourceDataLine keyboard_line = AudioSystem.getSourceDataLine(FORMAT);
		keyboard_line.open(FORMAT);
		keyboard_line.start();
		
		// Small byte array output stream for later WAV file
		ByteArrayOutputStream keyboard_out = new ByteArrayOutputStream();

		// Create keyboard listener
		ASIOHandler keyboard_handler = new ASIOHandler(keyboard_line, keyboard_out, System.currentTimeMillis());
		keyboard_handler.start();
		
		// Open a window
		JFrame window = new JFrame();
		window.setSize(200, 200);
		window.setTitle("Keyboard Window");
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.setVisible(true);
		window.addWindowListener(new WindowListener() {
			@Override public void windowOpened(WindowEvent e) { }
			@Override public void windowIconified(WindowEvent e) { }
			@Override public void windowDeiconified(WindowEvent e) { }
			@Override public void windowDeactivated(WindowEvent e) {}
			@Override public void windowClosing(WindowEvent e) { }
			@Override public void windowActivated(WindowEvent e) {}
			
			@Override 
			public void windowClosed(WindowEvent e) {
				try {
					System.out.println("Saving .wav file and exiting...");
					keyboard_handler.stop();
					byte[] bytes = keyboard_out.toByteArray();
					AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(bytes), FORMAT, bytes.length / (FORMAT.getSampleSizeInBits()/8)), Type.WAVE, new File("audio-" + System.currentTimeMillis() + ".wav"));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			
		});
	}
	
}

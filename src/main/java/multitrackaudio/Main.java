package multitrackaudio;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;

import com.zakgof.velvetvideo.IDemuxer;
import com.zakgof.velvetvideo.IVelvetVideoLib;
import com.zakgof.velvetvideo.IVideoDecoderStream;
import com.zakgof.velvetvideo.IVideoFrame;
import com.zakgof.velvetvideo.impl.VelvetVideoLib;

public class Main {

	public static final AudioFormat FORMAT = new AudioFormat(44100.0f, 16, 2, true, false);
	public static List<Clip> launch = new ArrayList<>();
	
	public static void main(String[] args) throws Exception {
		// Prepare Video
		IVelvetVideoLib lib = VelvetVideoLib.getInstance();
		IDemuxer demuxer = lib.demuxer(new File("mta/video.mp4"));
		IVideoDecoderStream videoStream = demuxer.videoStream(0);
		
		// Load all audio tracks
		for (File track : new File("mta/tracks/").listFiles())
			if (!track.getName().endsWith(".wav"))
				playAudio(track);
			else
				playWav(track);
		
		// Add new audio track
		SourceDataLine keyboard_line = AudioSystem.getSourceDataLine(FORMAT);
		keyboard_line.open(FORMAT);
		keyboard_line.start();
		FileOutputStream keyboard_out = new FileOutputStream(new File("mta/recordings/track_" + System.currentTimeMillis() + ".raw"));
		
		// Launch all audio track
		long time = System.currentTimeMillis() + 4000;
		
		ASIOHandler keyboard_handler = new ASIOHandler(keyboard_line, keyboard_out, time);
		keyboard_handler.start();
		
		// Play tracks at the same time
		launch.forEach(c -> {
			new Thread(() -> {
				while (System.currentTimeMillis() < time) {}
				c.start();
			}).start();
		});
		
		// Open a window
		JFrame window = new JFrame() {
			public void paint(Graphics g) {
				long renderNextFrameAt = time;
				while (true) {
					IVideoFrame videoFrame = videoStream.nextFrame();
					BufferedImage image = videoFrame.image();
					while (System.currentTimeMillis() < renderNextFrameAt+100) {}
					g.drawImage(image, 0, 0, 1920, 1080, null);
					renderNextFrameAt = time + (videoFrame.nanostamp() / 1000000);
				}
			};
		};
		window.setBounds(1920, 0, 1920, 1080);
		window.setTitle("Multi Track Audio!");
		window.setUndecorated(true);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	/**
	 * Plays a RAW Audio File
	 * @param raw Audio File
	 * @throws Exception File Not Found Exception
	 */
	private static void playAudio(File raw) throws Exception {
		byte[] data = Files.readAllBytes(raw.toPath());
		Clip clip = AudioSystem.getClip();
		clip.open(new AudioInputStream(new ByteArrayInputStream(data), FORMAT, data.length/2/2));
		
		launch.add(clip);
	}
	
	/**
	 * Plays a WAV File
	 * @param track Audio File
	 * @throws Exception File Not Found Exception
	 */
	private static void playWav(File track) throws Exception {
		Clip clip = AudioSystem.getClip();
		clip.open(AudioSystem.getAudioInputStream(track));
		
		launch.add(clip);
	}
	
}

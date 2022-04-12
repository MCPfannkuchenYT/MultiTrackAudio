package multitrackaudio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import multitrackaudio.audio.AudioDeviceDiscovery;
import multitrackaudio.audio.AudioResampler;
import multitrackaudio.audio.AudioDeviceDiscovery.AudioDevice;
import xt.audio.Structs.XtMix;
import xt.audio.XtAudio;
import xt.audio.XtPlatform;

public class Main {

	// Program Attributes
	public static final XtPlatform XT = XtAudio.init(null, null);
	
	public static void main(String[] args) throws Exception {
		// Prepare Console in
		Scanner sc = new Scanner(System.in);
		
		// Print audio devices with a given index that support the audio format
		System.out.println("Supported audio devices:");
		// Loop through all audio devices
		List<AudioDevice> devices = AudioDeviceDiscovery.getAudioDevices();
		for (int i = 0; i < devices.size(); i++) {
			AudioDevice device = devices.get(i);
			
			// Check availability
			Optional<XtMix> deviceMixOptional = device.device().getMix();
			if (!deviceMixOptional.isPresent())
				continue;
			
			// Print audio device
			System.out.println(i + ") " + device.name() + " (" + device.audiosystem() + ")");
		}
		
		// Ask for a list of input devices
		System.out.println("\nSelect input devices (0-" + (devices.size() - 1) + "):");
		System.out.print("> ");
		List<AudioDevice> selectedDevices = new ArrayList<>();
		for (String segment : sc.nextLine().split(","))
			selectedDevices.add(devices.get(Integer.parseInt(segment.trim())));
		System.out.println("\nMixing following audio devices into one stream:");
		for (AudioDevice audioDevice : selectedDevices)
			System.out.println(audioDevice.name());
		System.out.println();

		// Prepare Audio Resamplers
		List<AudioResampler> resamplers = new ArrayList<>();
		for (AudioDevice audioDevice : selectedDevices)
			resamplers.add(new AudioResampler(audioDevice, audioDevice.name().replace(' ', '_').replace('-', '_').replace('/', '_').replace('\\', '_') + ".wav"));
		
		// Launch Audio Resamplers
		for (AudioResampler audioResampler : resamplers)
			audioResampler.launch();
		
		// Let it run for 30 seconds
		Thread.sleep(30000);
		
		// Launch Audio Resamplers
		for (AudioResampler audioResampler : resamplers)
			audioResampler.shutdown();
		
		// Close Scanner
		sc.close();
	}
	
}

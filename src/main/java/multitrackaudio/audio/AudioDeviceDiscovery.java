package multitrackaudio.audio;

import static multitrackaudio.Main.XT;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import xt.audio.Enums.XtEnumFlags;
import xt.audio.Enums.XtSystem;
import xt.audio.XtDevice;
import xt.audio.XtDeviceList;
import xt.audio.XtService;

/**
 * Discovers audio devices from all audio systems
 * @author Pancake
 */
public class AudioDeviceDiscovery {

	// List of audio devices
	private static List<AudioDevice> devices;
	
	// Audio Device record
	public record AudioDevice(XtSystem audiosystem, String id, String name, XtDevice device) {};
	
	/**
	 * Returns the list of audio devices
	 * @return Audio Device List
	 */
	public static List<AudioDevice> getAudioDevices() {
		// Check whether a search has been done before;
		if (devices == null)
			discoverAudioDevices();
		
		return devices;
	}
	
	/**
	 * Discovers all audio devices for all available APIs
	 */
	private static void discoverAudioDevices() {
		devices = new ArrayList<>();
		
		// Loop through all available audio systems
		for (XtSystem audiosystem : XT.getSystems()) {
			if (audiosystem == XtSystem.DIRECT_SOUND) continue; // (skip direct sound)
			XtService audioservice = XT.getService(audiosystem);
			
			// Loop through all available audio devices
			XtDeviceList audiodevices = audioservice.openDeviceList(EnumSet.of(XtEnumFlags.INPUT));
			for (int i = 0; i < audiodevices.getCount(); i++) {
				String id = audiodevices.getId(i);
				
				// Hide Loopback and Exclusive
				String name = audiodevices.getName(id);
				if (name.contains("Loopback") || name.contains("Exclusive"))
					continue;
				
				// Register the audio device to the list of devices
				devices.add(new AudioDevice(audiosystem, id, name, audioservice.openDevice(id)));
			}
		}
	}
	
}

# KWP Logger for Android

This project is intended to provide a KWP2000 data logger for Android.

You can access a very primitive dialog similar to the Measurements dialog in VCDS by visiting the ... menu.

The application connects to an ELM327 device over Bluetooth, but the hardware interface is completely abstracted and so a class conforming to the KWP2000IO protocol could allow it to work with a USB cable or other type of device.

Right now, it's hardcoded at start to log into the transmission controller (0x02 init, 0x1A comm) and fetch the first part of Measurement Block 6, which is assumed to be the ATF temp.

This is because this project is currently intended to provide a quick shop tool for flushing the transmission fluid in a Touraeg or 955 Cayenne.

However, the basic library (once all measurement types are added) should be easily usable to log any KWP measurement blocks. I've tested logging various blocks from the ME7 in my Cayenne Turbo and it works well. Combined with the ability to alter measurement blocks in the flash ( http://nefariousmotorsports.com/forum/index.php?topic=2349.0 ) , this could provide a good mobile datalogging solution.

I have also experimented with the various "fast" mechanisms to log ME7 ECUs (DynamicDefineLocalIdentifier, using WriteMem to patch the identifier table), but my Cayenne's ME7 doesn't let me write memory with a Diagnostic session and returns a negative response when I try to open a Development session, so I need to do more reversing work before I can proceed further. 

If you find this useful or have improvements, please send me a pull request!

This code is released under the BSD License.

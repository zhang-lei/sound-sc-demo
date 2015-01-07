package com.android.soundcommunicate;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;


public class PowerSupply {

	public boolean isPowerIsSupplying() {
		return powerIsSupplying;
	}

	public void setPowerIsSupplying(boolean powerIsSupplying) {
		this.powerIsSupplying = powerIsSupplying;
	}

	private boolean powerIsSupplying = false;

	public static int pwMinBufferSize = AudioTrack.getMinBufferSize(20000,
					AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);
	AudioTrack pwAT;

	public boolean powerIsSupplying(){
		return powerIsSupplying;
	}

	public void pwStart(short[] carrierSignal) {
		if(powerIsSupplying)
			pwStop();

		pwAT = new AudioTrack(AudioManager.STREAM_MUSIC,
									 20000,
									 AudioFormat.CHANNEL_OUT_MONO,
									 AudioFormat.ENCODING_PCM_16BIT,
									 pwMinBufferSize,
									 AudioTrack.MODE_STREAM);

		powerIsSupplying = true;

		pwAT.setStereoVolume(1, 0);
		//pwAT.setLoopPoints(0, carrierSignal.length, -1);
		pwAT.play();

		new powerThread(pwAT, carrierSignal).start();
	}

	class powerThread extends Thread {
		AudioTrack pwAT;
		short[] data;
		powerThread(AudioTrack pwAT, short[] data) {
			this.pwAT = pwAT;
			this.data = data;
		}

		@Override
		public void run() {
			while(powerIsSupplying) {
				pwAT.write(data, 0, data.length);
				pwAT.flush();
			}
		}
	}

	public void pwStop() {
		if(pwAT != null)
		{
			pwAT.release();
			pwAT = null;
		}
		powerIsSupplying = false;
	}
}//end class

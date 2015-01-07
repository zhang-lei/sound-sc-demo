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

	public static int pwMinBufferSize = AudioTrack.getMinBufferSize(10000,
					AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_8BIT);
	AudioTrack pwAT;

	public boolean powerIsSupplying(){
		return powerIsSupplying;
	}

	public void pwStart(byte[] carrierSignal) {
		if(powerIsSupplying)
			pwStop();

		pwAT = new AudioTrack(AudioManager.STREAM_MUSIC,
									 10000,
									 AudioFormat.CHANNEL_OUT_MONO,
									 AudioFormat.ENCODING_PCM_8BIT,
									 pwMinBufferSize*2,
									 AudioTrack.MODE_STATIC);

		powerIsSupplying = true;
		pwAT.write(carrierSignal, 0, carrierSignal.length);
		pwAT.flush();
		pwAT.setStereoVolume(1, 0);
		pwAT.setLoopPoints(0, carrierSignal.length, -1);
		pwAT.play();
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

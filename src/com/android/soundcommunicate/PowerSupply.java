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

	public static int pwMinBufferSize = AudioTrack.getMinBufferSize(16000,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT);
	AudioTrack pwAT;

	public boolean powerIsSupplying(){
		return powerIsSupplying;
	}

	public void pwStart(short[] carrierSignal) {
		if(powerIsSupplying)
			pwStop();

		pwAT = new AudioTrack(AudioManager.STREAM_MUSIC,
									 16000,
									 AudioFormat.CHANNEL_OUT_MONO,
									 AudioFormat.ENCODING_PCM_16BIT,
									 pwMinBufferSize,
									 AudioTrack.MODE_STREAM);

		powerIsSupplying = true;

		pwAT.setStereoVolume(1, 0);
		//pwAT.setLoopPoints(0, carrierSignal.length, -1);
		pwAT.play();
		//}
		// 如果需要供电 需要设置循环点
		// audioplayer.setLoopPoints(0, minSize, -1);

		new powerThread(pwAT).start();
	}

	class powerThread extends Thread {
		AudioTrack pwAT;
		short[] data;
		powerThread(AudioTrack pwAT) {
			this.pwAT = pwAT;
			// 左声道提供电源支持
			StringBuffer tdata = new StringBuffer();

			for (int i = 0; i < 1000; i++) {
				tdata.append("11111111");
			}

			this.data = WaveUtil.package2wave(tdata.toString());
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

		powerIsSupplying = false;
		if(pwAT != null)
		{
			pwAT.stop();
			pwAT.release();
			pwAT = null;
		}

	}
}//end class

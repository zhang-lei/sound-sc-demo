package com.android.soundcommunicate;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class MessageOut {
	private boolean msgIsSending = false;
	private final String TAG = "SendTest";

	private boolean isPlay = false;

	
	public boolean msgIsSending(){
		return msgIsSending;
	}
	
	public void msgStart(String carrierSignalMsg) {

		if(msgIsSending) {
			msgStop();
		}
		
		play(carrierSignalMsg);

	}

	static int baudRate = 16000;

	AudioTrack audioplayer;
	static int  minSize ;

	public void play (String str) {


		short[] send = convert2Byte(str);


		minSize = AudioTrack.getMinBufferSize(baudRate,
					AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		audioplayer = new AudioTrack(AudioManager.STREAM_MUSIC, baudRate, AudioFormat.CHANNEL_OUT_MONO,
											AudioFormat.ENCODING_PCM_16BIT, minSize * 2, AudioTrack.MODE_STREAM);

		audioplayer.play();
		audioplayer.setStereoVolume(0, 1);                       // 设置左右声道播放音量
		int count = audioplayer.write(send, 0, send.length);
		audioplayer.flush();
		//}
		// 如果需要供电 需要设置循环点
		// audioplayer.setLoopPoints(0, minSize, -1);
		audioplayer.stop();
		audioplayer.release();

	}

	public void msgStop() {
		if(audioplayer != null)
		{
			audioplayer.release();
			audioplayer = null;
		}
		msgIsSending = false;
	}

	/**
	 * 将传递过来的数字转换成波形
	 * @param msg
	 * @return
	 */
	public static short[] convert2Byte(final String msg) {

		int data = 0;
		if (msg.contains("0x")) {
			// 处理16进制
			data = Integer.parseInt(msg.substring(2),16);
		} else {
			data = Integer.parseInt(msg);
		}

		if (data >= 256 || data < -256) {
			return null;
		}

		byte[] bData = new byte[64];
		for (int i = 0; i < 64; i++) {
			bData[i] = (byte)(data + i);
		}

		return WaveUtil.byte2wave(bData, 64, 8);
	}
}

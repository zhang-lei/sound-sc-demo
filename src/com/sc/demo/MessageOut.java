package com.sc.demo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class MessageOut {

	private final String TAG = "SendTest";

	static int baudRate = 16000;

	AudioTrack audioplayer;
	static int  minSize = 0;

	boolean isConnect = false;

	// 发送线程
	SendThread sendThread = null;

	// 初始化播放器
	public void connect() {

		minSize = AudioTrack.getMinBufferSize(baudRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

		audioplayer = new AudioTrack(AudioManager.STREAM_MUSIC, baudRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minSize * 2, AudioTrack.MODE_STREAM);

		audioplayer.play();
		audioplayer.setStereoVolume(0, 1);                       // 设置左右声道播放音量

		isConnect = true;
		sendThread = new SendThread(audioplayer);
		sendThread.start();
	}

	public void play (String str) {

		sendThread.setData(convert2Byte(str));
	}

	public void disconnect() {
		isConnect = false;
	}

	/**
	 * 发送线程
	 */
	class SendThread extends Thread {
		AudioTrack player;

		short[] data = null;

		short[] nodata = null;
		SendThread(AudioTrack player) {
			this.player = player;

			// 空数据初始化
			StringBuffer tdata = new StringBuffer();

			for (int i = 0; i < 1000; i++) {
				tdata.append("11111111");
			}

			this.nodata = WaveUtil.package2wave(tdata.toString(), 8);
		}

		@Override
		public void run() {

			while(isConnect) {

				if (data != null) {
					player.write(data, 0, data.length);
					player.flush();
					data = null;
				} else {
					sendNoData();
				}
			}

			player.stop();
			player.release();
			player = null;
		}

		private void sendNoData() {
			player.write(nodata, 0, nodata.length);
			player.flush();
		}

		public void setData(short[] data) {
			this.data = data;
		}
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

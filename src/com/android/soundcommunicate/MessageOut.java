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
	static int maxRate = 48000;
	static int delayBit = 0;
	private static byte ihigh = (byte) (-128);
	private static byte ilow = (byte) (127);

	AudioTrack audioplayer;
	static int  minSize ;

	public void play (String str) {

		// byte[] send = getBuffer(str);
		short[] send = convert2Byte(str);

		// short[] send = getShortData(str.getBytes(), str.getBytes().length);
		minSize = AudioTrack.getMinBufferSize(baudRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		audioplayer = new AudioTrack(AudioManager.STREAM_MUSIC, baudRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);

		audioplayer.play();

		audioplayer.write(send, 0, send.length);
		audioplayer.setStereoVolume(0, 1);// 设置左右声道播放音量
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

		// 如果不足 补0
		String txt = Integer.toBinaryString(data);

		// 偶校验
		int evenCheckCnt = 0;
		char even = '0';
		for (int k = 0; k < txt.length(); k++) {
			char c = txt.charAt(k);
			if (c == '1') {
				evenCheckCnt ++;
			}
		}
		if (evenCheckCnt % 2 == 1) {
			even = '1';
		}

		int count = txt.length();
		for (int i = 0; i < 8 - count; i++) {
			txt = "0" + txt;
		}

		String tmpHead = "1111111111111111";

		StringBuffer sdata = new StringBuffer();
		sdata.append(txt);
		sdata.reverse();
		txt = "0" + sdata.toString() + even + "111";   // 前补充起始位  后面添加校验位 和 停止位  前后都补充一些空位

		StringBuffer stringBuffer = new StringBuffer();
		// 重复64次
		for (int i= 0; i < 64; i++) {
			stringBuffer.append(txt);
		}
		txt = tmpHead + stringBuffer.toString() + tmpHead;

		txt = txt.replaceAll("0", "lh");
		txt = txt.replaceAll("1", "hl");

		txt = txt.replaceAll("h", "1");
		txt = txt.replaceAll("l", "0");

		// 转换
		int cnt = txt.length();  // 长度

		/*
		int bufLength = maxlenth / 8;
		char[] buf = new char[bufLength];
		for (int i = 0; i < bufLength; i++) {
			if (i < cnt) {
				buf[i] = txt.charAt(i);
			}  else {
				buf[i] = '1';
				buf[++i] = '0';
			}
		}*/

		short[] newbuf = new short[cnt * 8];         // 记录最大数字
		int bufindex = 0;
		for (int i = 0; i < cnt; i++) {
			// char c = buf[i];
			char c = txt.charAt(i);
			if ( c == '0') {
				for (int k = 0; k < 8; k ++) {
					newbuf[bufindex++] = (short) (Math.pow(2, 15) - 1);
					// buf[i] = 0;
				}
			} else {
				// buf[i] = (byte) (Math.pow(2, 16) - 1);
				// buf[i] = 1;
				for (int k = 0; k < 8; k ++) {

					newbuf[bufindex++] = (short) (-1 * Math.pow(2, 15));
					// buf[i] = 0;
				}
			}
		}
		return newbuf;
	}

	// 通过byteDate转为short型 的声音数据
	private short[] getShortData(byte[] data, int lenght) {
		int j = 0;
		String strBinary = getstrBinary(data, lenght);
		int len = strBinary.length();
		int m_bitDateSize = len * baudRate;
		short[] m_pRightDate = new short[m_bitDateSize];
		for (int i = 0; i < len; i++) {
			int ct = baudRate;
			if (strBinary.charAt(i) == '1') {
				while (ct-- > 0) {
					// m_pRightDate[j++] = min_iAmp;
					m_pRightDate[j++] = 32767;
				}
			} else {
				while (ct-- > 0) {
					// m_pRightDate[j++] = max_iAmp;
					m_pRightDate[j++] = -32767;
				}
			}
		}
		return m_pRightDate;
	}

	// 将一个字节编码转为2进制字符串
	private String getstrBinary(byte[] data, int lenght) {
		StringBuffer strDate = new StringBuffer(lenght * 8);
		for (int i = 0; i < lenght; i++) {
			String str = Integer.toBinaryString(data[i]);
			// System.out.println("str:"+str);
			while (str.length() < 8) {
				str = '0' + str;
			}
			strDate.append(str);
		}
		Log.i("strDate: " ,strDate+"");
		return strDate.toString();
	}
}

 package com.sc.demo;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

 /**
  * 接收硬件传输过来的信息
  */
@SuppressLint("HandlerLeak")
public class MessageRecv {
	private static final String TAG = "MyAudioRecord";
	//private final int recSampleRate = DecoderCore.getRecordSampleRate(); 

	private final int recSampleRate = 8000;
	private final int recChannel = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private final int recAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
	private final int audioSource = MediaRecorder.AudioSource.MIC;

	private boolean recordFlag = false;
	
	AudioRecord audioRecord;//录音
	RecordThread recThread;
	// DecoderCore dc;
	
	Context context;   //上下文对象
	
	public int minRecBufSize = AudioRecord.getMinBufferSize(recSampleRate, recChannel, recAudioFormat);
	
	public boolean isRecording(){
		return recordFlag;
	}
	
	public MessageRecv(){
		audioRecord = new AudioRecord(audioSource, recSampleRate, recChannel, recAudioFormat, minRecBufSize);
		// dc = new DecoderCore();
	}

	public void start(){
		recThread = new RecordThread(audioRecord, minRecBufSize);
		
		recordFlag = true;
		recThread.start();
	}
	
	public void stop(){
		recordFlag = false; 
		recThread = null;
	}
	
	public class RecordThread extends Thread{
		private AudioRecord ar;
		private int bufSize;
		
		public RecordThread(AudioRecord audioRecord, int bufferSize){
			this.ar = audioRecord;
			this.bufSize = bufferSize;
		}
		
		public void run(){
			try{

				File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.txt");
				if (file.exists()) {
					file.delete();
				}

				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}

				OutputStream os = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(os);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bos));

				short[] buffer = new short[bufSize];
				//int 
				
				ar.startRecording();

				/**
				 * 接收的信息流
				 * 持续接收 保持连续性 不间断
				 * 然后解析出来后 删除解析后的部分
				 * 保持该字段中只保留未解析的部分
				 */
				StringBuffer recvMsg = new StringBuffer();

				while(recordFlag){
					int ret = ar.read(buffer, 0, bufSize);
					
					if(ret == AudioRecord.ERROR_BAD_VALUE){
						recordFlag = false;
					}
					else{
						

						 short[] tmpBuf = new short[ret];
						 System.arraycopy(buffer, 0, tmpBuf, 0, ret);

						/**
						 * 接收信息
						 */
						for (int i = 0; i < ret; i++) {

							String code = "0";
							if (tmpBuf[i] > 0) {
								code = "1";
							}
							writer.write(code);
							recvMsg.append(code);
						}

						// 解码规则
						List<String> checklist = new ArrayList<String>();
						List<Integer> value = getByteValue(recvMsg, checklist);

						/**
						 * 解析信息
						 */
						 StringBuffer dataBuf = new StringBuffer();
						for (int i = 0; i < value.size(); i++) {
							String _data = String.format("%02x%s ", value.get(i), checklist.get(i));
							dataBuf.append(_data);
						}

						// 有数据的时候才发送
						if (!"".equals(dataBuf.toString())) {
							Message message = new Message();
							message.obj = dataBuf.toString();
							handler.sendMessage(message);
						}
						
					}
					
				}//while end   
				ar.stop();
				writer.flush();
				writer.close();
			}//try end
			catch (Exception e) {
				Log.e("Receive message E",e.toString());
			}
		}//run end
	}//RecordThread end

	public void msgHandOut(String msgToken) {
		Log.i(TAG, "+++>" + msgToken);
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}
	
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			
			((Main)context).setMsg(msg.obj.toString());
			//super.handleMessage(msg);
		}
	};


	/**
	 * 定义解析的格式
	 */
	static String startPattern = "(0{7,9}1{3,5})";
	static String bitPattern = "(1{4,5}0{4,5}|0{4,5}1{4,5})";
	static String stopPattern = "(1{4,5}0{4,5}1{4,5}0{4,5}1{4,5})";

	static StringBuffer pattern = new StringBuffer();

	static {
		pattern.append(startPattern);

		for (int i = 0; i < 9; i ++) {
			pattern.append(bitPattern);
		}

		pattern.append(stopPattern);

	}


	public  List<Integer> getByteValue(StringBuffer msg, List<String> checklist) {
		List<Integer> value = new ArrayList<Integer>();
		if (checklist == null) {
			checklist = new ArrayList<String>();
		}
		Matcher matcher = Pattern.compile(pattern.toString()).matcher(msg);

		int offset = -1;
		while (matcher.find()) {

			char[] b = new char[8];
			b[7] = getBitValue(matcher.group(2));
			b[6] = getBitValue(matcher.group(3));
			b[5] = getBitValue(matcher.group(4));
			b[4] = getBitValue(matcher.group(5));
			b[3] = getBitValue(matcher.group(6));
			b[2] = getBitValue(matcher.group(7));
			b[1] = getBitValue(matcher.group(8));
			b[0] = getBitValue(matcher.group(9));



			String t = String.valueOf(b);
			value.add(Integer.parseInt(t, 2));

			char check = getBitValue(matcher.group(10));

			// 处理校验结果
			int checkCnt = 0;
			for (char c : b) {
				if (c == '1') {
					checkCnt++;
				}
			}

			if (checkCnt % 2 == 1 && check == '1' || checkCnt % 2 == 0 && check == '0') {
				checklist.add("+");
			} else {
				checklist.add("-");
			}

			offset = matcher.end();
		}

		if (offset != -1) {
			msg.delete(0, offset);
		}

		return value;
	}

	public  char getBitValue(String bit) {
		if (bit.startsWith("1")) {
			return '1';
		} else if (bit.startsWith("0")) {
			return '0';
		}
		return '0';
	}

}//MyAudioRecord end

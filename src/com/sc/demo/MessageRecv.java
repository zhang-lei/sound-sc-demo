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
						List<Integer> value = WaveUtil.getByteValue(recvMsg, checklist);

						/**
						 * 解析信息
						 */
						 StringBuffer dataBuf = new StringBuffer();
						for (int i = 0; i < value.size(); i++) {

							/**
							 * 转换成16进制
							 */
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

}//MessageRecv end

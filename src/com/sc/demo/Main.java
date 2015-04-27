package com.sc.demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Main extends Activity {
	public final String TAG = "SoundCommunicate";
	public final static int maxDataBufSize = 256;
	public final static int POWERSUPPLY = 0;
	public final static int MESSAGEOUT = 1;
	private final int minFrequency = 500;
	private final int powerSupplyFreq = 10000;
	private int oldFrequency = 0;
	private int latestFrequency = 1000;

	// 判断是否连接电路板
	private boolean isLinked = false;

	// 是否电源开启
	private boolean powerOnOff = false;
	MessageOut msg;
	PowerSupply power;
	MessageRecv myRec;
	ToggleButton powerOnOffTB;
	ToggleButton recordTB;
	ToggleButton linkTB;
	Button sendMsgBT;
	SeekBar freqSB;
	TextView freqTV;
	TextView recMsgTV;
	EditText msgSendET;

	/*
	 * 判断是否有耳机
	 */
	boolean isHeadSet = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sound_communicate);
		powerOnOffTB = (ToggleButton) findViewById(R.id.audioPlayButton);
		sendMsgBT = (Button) findViewById(R.id.sendMsgBtuuon);
		linkTB = (ToggleButton) findViewById(R.id.linkButton);
		msgSendET = (EditText) findViewById(R.id.et_MsgSend);
		recordTB = (ToggleButton) findViewById(R.id.recordButton);
		freqSB = (SeekBar) findViewById(R.id.sendFreqSeekBar);
		freqTV = (TextView) findViewById(R.id.sendTV);
		recMsgTV = (TextView)findViewById(R.id.tv_recMsg);
		power = new PowerSupply();
		msg = new MessageOut(); 
		myRec = new MessageRecv();
		myRec.setContext(this);
		
		freqTV.setText(Main.this.getString(R.string.sendFreq) + String.valueOf(latestFrequency) + "Hz");
		
		powerOnOffButtonProcess();

		linkButtonProcess();
		
		sendMsgButtionProcess();
		
		recordButtonProcess();

		registerHeadsetPlugReceiver();
	}
	
	private void recordButtonProcess(){
		recordTB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					myRec.start();
					recMsgTV.setText("");
				}
				else{
					myRec.stop();
					//recMsgTV.setText(SoundCommunicate.this.getString(R.string.havdRecdMsgs));
				}
			}
		});
	}

	private void linkButtonProcess(){
		linkTB.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
				if (isChecked) {
					Toast.makeText(Main.this, "开始连接", Toast.LENGTH_SHORT).show();
					msg.connect();

					isLinked = true;

					// 开启频率调整
					freqSeekBarProcess();
				} else {

					isLinked = false;
					msg.disconnect();
					Toast.makeText(Main.this, "关闭连接", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	private void powerOnOffButtonProcess(){
		powerOnOffTB.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
				if (isChecked) {			
					powerOnOff = true;

					power.setPowerIsSupplying(true);
					power.pwStart();
				} else {
					power.setPowerIsSupplying(false);
					power.pwStop();
					powerOnOff = false;
				}
			}
		});
	}

	private void sendMsgButtionProcess(){
		sendMsgBT.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// byte[] bts = cc.soundCording(msgSendET.getText().toString().getBytes());

				// String txt = msgSendET.getText().toString();
				
				
				
				short[] bytes = new short[64];
				bytes[0] = (short) 0x81;
				bytes[1] = (short) 0x00;
				bytes[2] = (short) 0x00;
				bytes[3] = (short) 0xff;
				bytes[4] = (short) 0x3a;
				for(int i = 0x01 ; i <= 0x3a ; i++){
					bytes[4 + i] = (short) i;
				}
				
				short[] tmp = new short[63];
				System.arraycopy(bytes, 0, tmp, 0, 63);
				bytes[63] = CRC8.calcCrc8(tmp);
				msg.play(bytes);
			}
		});
	}

	private void freqSeekBarProcess(){
		freqSB.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onProgressChanged(SeekBar seekBar,int progress, boolean fromUser) {
				if (progress < minFrequency) {
					seekBar.setProgress(minFrequency);
					return;
				}
				
				latestFrequency = progress;
				if (oldFrequency != latestFrequency) {
					oldFrequency = latestFrequency;
					freqTV.setText(Main.this.getString(R.string.sendFreq) + String.valueOf(latestFrequency) + "Hz");

					if (isLinked) {
						// 设置发送的频率
						msg.setFrequency( (float)latestFrequency / 1000);
					}
				} 
		}
		});
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		if(power != null){
			power.pwStop();
			power = null;
		}
		if (msg != null) {
			msg.disconnect();
			msg = null;
		}
		//android.os.Process.killProcess(android.os.Process.myPid());
		unregisterReceiver(headsetPlugReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_sound_communicate, menu);
		return true;
	}
	
	/**
	 * 设置接收到的消息
	 * @param msg
	 */
	public void setMsg(String msg) {

		if (msg == null || "".equals(msg)) {
			return;
		}
		recMsgTV.setText(recMsgTV.getText() + msg);
	}

	private HeadsetPlugReceiver headsetPlugReceiver;

	public class HeadsetPlugReceiver extends BroadcastReceiver {

		private static final String TAG = "HeadsetPlugReceiver";

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("state")){
				if (intent.getIntExtra("state", 0) == 0){
					Toast.makeText(context, "headset not connected", Toast.LENGTH_LONG).show();
					// msg.invalidMsgStop();
					isHeadSet = false;
				}
				else if (intent.getIntExtra("state", 0) == 1){
					Toast.makeText(context, "headset connected", Toast.LENGTH_LONG).show();
					// msg.invalidMsgStart();
					isHeadSet = true;
				}
			}
		}

	}

	private void registerHeadsetPlugReceiver() {
		headsetPlugReceiver = new HeadsetPlugReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("android.intent.action.HEADSET_PLUG");
		registerReceiver(headsetPlugReceiver, intentFilter);
	}
}

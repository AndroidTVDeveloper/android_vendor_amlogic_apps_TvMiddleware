package com.amlogic.tvtest;

import android.util.Log;
import android.os.Bundle;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvactivity.TVActivity;
import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVConst;

public class TVTest extends TVActivity{
	private static final String TAG="TVTest";

	public void onCreate(Bundle savedInstanceState){
		Log.d(TAG, "onCreate");

		super.onCreate(savedInstanceState);

		setContentView(R.layout.test);

		openVideo();
	}

	public void onConnected(){
		Log.d(TAG, "connected");

		TVScanParams sp = TVScanParams.dtvAllbandScanParams(0, TVChannelParams.MODE_QAM);
		setInputSource(TVConst.SourceType.SOURCE_TYPE_DTV);
		Log.d(TAG, "Start Scan...");
		startScan(sp);
		

		TVProgram prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(1));
		if(prog!=null){
			playProgram(new TVProgramNumber(1));
		}
	}

	public void onDisconnected(){
		stopScan(true);
		Log.d(TAG, "disconnected");
	}

	public void onMessage(TVMessage msg){
		Log.d(TAG, "message "+msg.getType());
	}
}


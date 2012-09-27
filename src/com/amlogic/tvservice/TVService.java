package com.amlogic.tvservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.os.Message;
import android.os.Looper;
import android.os.Handler;
import android.database.Cursor;
import java.util.Date;
import android.os.RemoteCallbackList;
import android.util.Log;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvutil.TVPlayParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.ITVCallback;
import com.amlogic.tvutil.TVConfigValue;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVChannel;
import com.amlogic.tvdataprovider.TVDataProvider;
//import com.amlogic.tvservice.TVScanner.TVScannerParams;

public class TVService extends Service{
	private static final String TAG = "TVService";

	/*Message types*/
	private static final int MSG_SET_SOURCE    = 1949;
	private static final int MSG_PLAY_PROGRAM  = 1950;
	private static final int MSG_STOP_PLAYING  = 1951;
	private static final int MSG_START_TIMESHIFTING = 1952;
	private static final int MSG_START_PLAYBACK  = 1953;
	private static final int MSG_START_SCAN      = 1954;
	private static final int MSG_STOP_SCAN       = 1955;
	private static final int MSG_START_RECORDING = 1956;
	private static final int MSG_STOP_RECORDING  = 1957;
	private static final int MSG_PAUSE           = 1958;
	private static final int MSG_RESUME          = 1959;
	private static final int MSG_FAST_FORWARD    = 1960;
	private static final int MSG_FAST_BACKWARD   = 1961;
	private static final int MSG_SEEK_TO         = 1962;
	private static final int MSG_DEVICE_EVENT    = 1963;
	private static final int MSG_EPG_EVENT       = 1964;
	private static final int MSG_SCAN_EVENT      = 1965;


	final RemoteCallbackList<ITVCallback> callbacks
	                        = new RemoteCallbackList<ITVCallback>();

	private final ITVService.Stub mBinder = new ITVService.Stub(){
		public synchronized void registerCallback(ITVCallback cb){
			if(cb !=null){
				callbacks.register(cb);
			}
		}

		public synchronized void unregisterCallback(ITVCallback cb){
			if(cb != null){
				callbacks.unregister(cb);
			}
		}

		public void setConfig(String name, TVConfigValue value){
			try{
				config.set(name, value);
			}catch(Exception e){
				Log.e(TAG, "failed to set config "+name);
			}
		}

		public TVConfigValue getConfig(String name){
			TVConfigValue v = null;

			try{
				v = config.get(name);
			}catch(Exception e){
				Log.e(TAG, "failed to get config "+name);
			}

			return v;
		}

		public void registerConfigCallback(String name, ITVCallback cb){
			try{
				config.registerRemoteCallback(name, cb);
			}catch(Exception e){
				Log.d(TAG, "registerRemoteCallback "+name+" failed");
			}
		}

		public void unregisterConfigCallback(String name, ITVCallback cb){
			try{
				config.unregisterRemoteCallback(name, cb);
			}catch(Exception e){
				Log.e(TAG, "unregisterRemoteCallback "+name+" failed");
			}
		}

		public long getTime(){
			return time.getTime();
		}

		public void setInputSource(int source){
			Message msg = handler.obtainMessage(MSG_SET_SOURCE, new Integer(source));
			handler.sendMessage(msg);
		}

		public void playProgram(TVPlayParams tp){
			Message msg = handler.obtainMessage(MSG_PLAY_PROGRAM, tp);
			handler.sendMessage(msg);
		}

		public void stopPlaying(){
			Message msg = handler.obtainMessage(MSG_STOP_PLAYING);
			handler.sendMessage(msg);
		}

		public void startTimeshifting(){
			Message msg = handler.obtainMessage(MSG_START_TIMESHIFTING);
                        handler.sendMessage(msg);
		}

		public void startRecording(int bookingID){
			Message msg = handler.obtainMessage(MSG_START_RECORDING, new Integer(bookingID));
                        handler.sendMessage(msg);
		}

		public void stopRecording(){
			Message msg = handler.obtainMessage(MSG_STOP_RECORDING);
                        handler.sendMessage(msg);
		}

		public void startPlayback(int bookingID){
			Message msg = handler.obtainMessage(MSG_START_PLAYBACK);
                        handler.sendMessage(msg);
		}

		public void startScan(TVScanParams sp){
			Message msg = handler.obtainMessage(MSG_START_SCAN);
                        handler.sendMessage(msg);
		}

		public void stopScan(boolean store){
			Message msg = handler.obtainMessage(MSG_STOP_SCAN, new Boolean(store));
                        handler.sendMessage(msg);
		}

		public void pause(){
			Message msg = handler.obtainMessage(MSG_PAUSE);
                        handler.sendMessage(msg);
		}

		public void resume(){
			Message msg = handler.obtainMessage(MSG_RESUME);
                        handler.sendMessage(msg);
		}

		public void fastForward(int speed){
			Message msg = handler.obtainMessage(MSG_FAST_FORWARD, new Integer(speed));
                        handler.sendMessage(msg);
		}

		public void fastBackward(int speed){
			Message msg = handler.obtainMessage(MSG_FAST_BACKWARD, new Integer(speed));
                        handler.sendMessage(msg);
		}

		public void seekTo(int pos){
			Message msg = handler.obtainMessage(MSG_SEEK_TO, new Integer(pos));
                        handler.sendMessage(msg);
		}
	};

	/*Message handler*/
	private Handler handler = new Handler(){
		public void handleMessage(Message msg){
			switch(msg.what){
				case MSG_SET_SOURCE:
					int val = (Integer)msg.obj;
					TVConst.SourceType type = TVConst.SourceType.values()[val];

					resolveSetInputSource(type);
					break;
				case MSG_PLAY_PROGRAM:
					resolvePlayProgram((TVPlayParams)msg.obj);
					break;
				case MSG_STOP_PLAYING:
					resolveStopPlaying();
					break;
				case MSG_START_TIMESHIFTING:
					resolveStartTimeshifting();
					break;
				case MSG_START_PLAYBACK:
					resolveStartPlayback((Integer)msg.obj);
					break;
				case MSG_START_SCAN:
					resolveStartScan((TVScanParams)msg.obj);
					break;
				case MSG_STOP_SCAN:
					resolveStopScan((Boolean)msg.obj);
					break;
				case MSG_START_RECORDING:
					resolveStartRecording((Integer)msg.obj);
					break;
				case MSG_STOP_RECORDING:
					resolveStopRecording();
					break;
				case MSG_PAUSE:
					resolvePause();
					break;
				case MSG_RESUME:
					resolveResume();
					break;
				case MSG_FAST_FORWARD:
					resolveFastForward((Integer)msg.obj);
					break;
				case MSG_FAST_BACKWARD:
					resolveFastBackward((Integer)msg.obj);
					break;
				case MSG_SEEK_TO:
					resolveSeekTo((Integer)msg.obj);
					break;
				case MSG_DEVICE_EVENT:
					resolveDeviceEvent((TVDevice.Event)msg.obj);
					break;
				case MSG_EPG_EVENT:
					resolveEpgEvent((TVEpgScanner.Event)msg.obj);
					break;
				case MSG_SCAN_EVENT:
					resolveScanEvent((TVScanner.Event)msg.obj);
					break;
			}
		}
	};

	private TVTime time = new TVTime();
	private TVConfig config;
	private TVDevice device = new TVDevice(){
		/*Device event handler*/
		public void onEvent(TVDevice.Event event){
			Message msg = handler.obtainMessage(MSG_DEVICE_EVENT, event);
			handler.sendMessage(msg);
		}
	};

	private TVScanner scanner = new TVScanner(){
		/*Scanner event handler*/
		public void onEvent(TVScanner.Event event){
			Message msg = handler.obtainMessage(MSG_SCAN_EVENT, event);
			handler.sendMessage(msg);
		}
	};

	private TVEpgScanner epgScanner = new TVEpgScanner(){
		/*EPG event handler*/
		public void onEvent(TVEpgScanner.Event event){
			switch(event.type){
				case TVEpgScanner.Event.EVENT_TDT_END:
					time.setTime(event.time);
					return;
			}

			Message msg = handler.obtainMessage(MSG_EPG_EVENT, event);
			handler.sendMessage(msg);
		}
	};

	private enum TVStatus{
		STATUS_SET_INPUT_SOURCE,
		STATUS_SET_FRONTEND,
		STATUS_PLAY_ATV,
		STATUS_PLAY_DTV,
		STATUS_TIMESHIFTING,
		STATUS_PLAYBACK,
		STATUS_STOPPED,
		STATUS_SCAN
	}

	public TVService(){
		Log.d(TAG, "XXXXXXXXXXXXXXXXXX");
	}

	private TVStatus status;
	private TVConst.SourceType inputSource;
	private TVConst.SourceType reqInputSource;
	private TVPlayParams atvPlayParams;
	private TVPlayParams dtvPlayParams;
	private int dtvProgramType = TVProgram.TYPE_TV;
	private TVChannelParams channelParams;
	private boolean recording = false;

	private void stopPlaying(){
		if(status == TVStatus.STATUS_PLAY_ATV){
			device.stopATV();
			status = TVStatus.STATUS_STOPPED;
		}else if(status == TVStatus.STATUS_PLAY_DTV){
			device.stopDTV();
			status = TVStatus.STATUS_STOPPED;
		}else if(status == TVStatus.STATUS_TIMESHIFTING){
			device.stopTimeshifting();
			status = TVStatus.STATUS_STOPPED;
		}else if(status == TVStatus.STATUS_PLAYBACK){
			device.stopPlayback();
			status = TVStatus.STATUS_STOPPED;
		}
	}

	private void stopScan(boolean store){
		if(status == TVStatus.STATUS_SCAN){
			scanner.stop(store);
			TVDataProvider.syncToFile();

			status = TVStatus.STATUS_STOPPED;
		}
	}

	private void stopRecording(){
		if(recording){
			TVDevice.DTVRecordParams params;

			params = device.stopRecording();
			recording = false;
		}
	}

	private boolean isInTVMode(){
		if((inputSource == TVConst.SourceType.SOURCE_TYPE_ATV) ||
				(inputSource == TVConst.SourceType.SOURCE_TYPE_DTV))
			return true;
		return false;
	}

	private boolean isInFileMode(){
		if(!isInTVMode())
			return false;

		if((status == TVStatus.STATUS_TIMESHIFTING) ||
				(status == TVStatus.STATUS_PLAYBACK))
			return true;

		return false;
	}

	private TVProgram playParamsToProgram(TVPlayParams params){
		TVProgram p = null;

		try{
			switch(params.getType()){
				case TVPlayParams.PLAY_PROGRAM_NUMBER:
					int type;

					if(inputSource == TVConst.SourceType.SOURCE_TYPE_DTV)
						type = dtvProgramType;
					else
						type = TVProgram.TYPE_ATV;

					p = TVProgram.selectByNumber(this, type, params.getProgramNumber());
					break;
				case TVPlayParams.PLAY_PROGRAM_ID:
					p = TVProgram.selectByID(this, params.getProgramID());
					break;
			}
		}catch(Exception e){
			Log.e(TAG, "playParamsToProgram failed");
		}

		return p;
	}

	private void playCurrentProgramAV(){
		if(inputSource == TVConst.SourceType.SOURCE_TYPE_ATV){
			device.playATV();
			status = TVStatus.STATUS_PLAY_ATV;
		}else if(inputSource == TVConst.SourceType.SOURCE_TYPE_DTV){
			TVProgram p;
			TVProgram.Video video;
			TVProgram.Audio audio;
			int vpid = 0x1fff, apid = 0x1fff, vfmt = -1, afmt = -1;

			p = playParamsToProgram(dtvPlayParams);
			video = p.getVideo();

			try{
				String lang = config.getString("player:audio:language");
				audio = p.getAudio(lang);
				apid = audio.getPID();
				afmt = audio.getFormat();
			}catch(Exception e){
				audio = p.getAudio();
				if(audio != null){
					apid = audio.getPID();
					afmt = audio.getFormat();
				}
			}

			if(video != null){
				vpid = video.getPID();
				vfmt = video.getFormat();
			}

			device.playDTV(vpid, vfmt, apid, vfmt);
			status = TVStatus.STATUS_PLAY_DTV;
		}
	}

	private void playCurrentProgram(){
		TVProgram p = null;
		TVChannelParams fe_params;

		if(inputSource == TVConst.SourceType.SOURCE_TYPE_ATV){
			p = playParamsToProgram(atvPlayParams);
		}else if(inputSource == TVConst.SourceType.SOURCE_TYPE_DTV){
			p = playParamsToProgram(dtvPlayParams);
		}

		fe_params = p.getChannel().getParams();

		if((channelParams == null) || !channelParams.equals(fe_params)){
			channelParams = fe_params;

			device.setFrontend(fe_params);
			status = TVStatus.STATUS_SET_FRONTEND;
			return;
		}

		playCurrentProgramAV();
	}

	/*Reset the input source.*/
	private void resolveSetInputSource(TVConst.SourceType src){
		if(src == reqInputSource)
			return;

		reqInputSource = src;

		if(isInTVMode()){
			stopPlaying();
			stopRecording();
			stopScan(false);
		}

		//device.setInputSource(src);

		status = TVStatus.STATUS_SET_INPUT_SOURCE;
	}

	/*Play a program.*/
	private void resolvePlayProgram(TVPlayParams tp){
		if(!isInTVMode())
			return;
	}

	/*Stop playing.*/
	private void resolveStopPlaying(){
		if(!isInTVMode())
			return;

		stopPlaying();
	}

	/*Start timeshifting.*/
	private void resolveStartTimeshifting(){
		if(!isInTVMode())
			return;
	}

	/*Start DVR playback.*/
	private void resolveStartPlayback(int bookingID){
		if(!isInTVMode())
			return;
	}

	/*Start channel scanning.*/
	private void resolveStartScan(TVScanParams sp){
		Log.d(TAG, "resolveStart");
		/*if(!isInTVMode())
			return;*/
		Log.d(TAG, "resolveStart 1111");
		stopPlaying();
		stopRecording();
		stopScan(false);

		channelParams = null;
		Log.d(TAG, "resolveStart 222222");
		/** Configure scan */
		TVScanner.TVScannerParams tsp = new TVScanner.TVScannerParams(sp);
		Log.d(TAG, "resolveStart sdsdsdssdssds");
		/*tsp.minFreq = config.getInt("scan:atv:minfreq");
		tsp.maxFreq = config.getInt("scan:atv:maxfreq");
		tsp.startFreq = config.getInt("scan:atv:startfreq");
		tsp.direction = config.getInt("scan:atv:direction");
		tsp.videoStd = config.getInt("scan:atv:vidstd");
		tsp.audioStd = config.getInt("scan:atv:audstd");*/
		tsp.minFreq = 100000000;
		tsp.maxFreq = 200000000;
		tsp.videoStd = 0;
		tsp.audioStd = 0;
		tsp.dbNativeHandle = TVDataProvider.getDatabaseNativeHandle();
		tsp.demuxId = 0;
		/** use the frequency given by startParams in sp */
		tsp.frequencyList = null;
		if (sp.dtvMode == TVScanParams.DTV_MODE_ALLBAND) {
			Log.d(TAG, "resolveStart 333333");
			/** load the frequency list */
			String region = "Default Allband";//config.getString("scan:dtv:region");
			Cursor c = getContentResolver().query(TVDataProvider.RD_URL,
					null,
					"select * from region_table where name=" + region + " and source=" + sp.tsSourceID,
					null, null);
			Log.d(TAG, "resolveStart 44444");
			if(c != null){
				if(c.moveToFirst()){
					int col = c.getColumnIndex("frequencies");
					String freqs = c.getString(col);
					if (freqs != null && freqs.length() > 0) {
						String[] flist = freqs.split(" ");
						if (flist !=null && flist.length > 0) {
							tsp.frequencyList = new int[flist.length];
							/** get each frequency */
							for (int i=0; i<tsp.frequencyList.length; i++) {
								tsp.frequencyList[i] = Integer.parseInt(flist[i]);
								Log.d(TAG, ""+tsp.frequencyList[i]);
							}
						}
					}
				}
				c.close();
			}
		} 
		Log.d(TAG, "resolveStart 5555555");
		scanner.scan(tsp);

		status = TVStatus.STATUS_SCAN;
	}

	/*Stop scanning process.*/
	private void resolveStopScan(boolean store){
		if(!isInTVMode())
			return;

		stopScan(store);

		playCurrentProgram();
	}

	/*Start recording.*/
	private void resolveStartRecording(int bookingID){
		stopRecording();
	}

	/*Stop recording.*/
	private void resolveStopRecording(){
	}

	/*Pause.*/
	private void resolvePause(){
		if(!isInFileMode())
			return;

		device.pause();
	}

	/*Resume.*/
	private void resolveResume(){
		if(!isInFileMode())
			return;

		device.resume();
	}

	/*Fast forward.*/
	private void resolveFastForward(int speed){
		if(!isInFileMode())
			return;

		device.fastForward(speed);
	}

	/*Fast backward.*/
	private void resolveFastBackward(int speed){
		if(!isInFileMode())
			return;

		device.fastBackward(speed);
	}

	/*Seek to a new position.*/
	private void resolveSeekTo(int pos){
		if(!isInFileMode())
			return;

		device.seekTo(pos);
	}

	/*Solve the events from the device.*/
	private void resolveDeviceEvent(TVDevice.Event event){
		switch(event.type){
			case TVDevice.Event.EVENT_SET_INPUT_SOURCE_OK:
				inputSource = reqInputSource;
				if(isInTVMode()){
					playCurrentProgram();
				}
				break;
			case TVDevice.Event.EVENT_SET_INPUT_SOURCE_FAILED:
				Log.e(TAG, "set input source to "+status.name()+" failed");
				break;
			case TVDevice.Event.EVENT_FRONTEND:
				if(isInTVMode()){
					if((status == TVStatus.STATUS_SET_FRONTEND) &&
							event.feParams.equals(channelParams) &&
							(event.feStatus & TVChannelParams.FE_HAS_LOCK)!=0){
						playCurrentProgramAV();
					}
				}
				break;
		}
	}

	/*Solve the events from the EPG scanner.*/
	private void resolveEpgEvent(TVEpgScanner.Event event){
	}

	/*Solve the events from the channel scanner.*/
	private void resolveScanEvent(TVScanner.Event event){
		Log.d(TAG, "Channel scan event: " + event.type);
		switch (event.type) {
			case TVScanner.Event.EVENT_SCAN_PROGRESS:
				Log.d(TAG, "Progress: " + event.percent + "%" + ", channel no. "+event.channelNumber);
				if (event.programName != null) {
					Log.d(TAG, "New Program : "+ event.programName + ", type "+ event.programType);
				}
				break;
			case TVScanner.Event.EVENT_STORE_BEGIN:
				Log.d(TAG, "Store begin...");
				break;
			case TVScanner.Event.EVENT_STORE_END:
				Log.d(TAG, "Store end");
				break;
			default:
				break;
				
		}
	}

	public IBinder onBind (Intent intent){
		return mBinder;
	}

	public void onCreate(){
		Log.d(TAG, "onCreate");
		super.onCreate();
		Log.d(TAG, "openDatabase");
		TVDataProvider.openDatabase(this);
		Log.d(TAG, "TVConfig");
		config = new TVConfig(this);
		Log.d(TAG, "OK");
	}

	public void onDestroy(){
		callbacks.kill();
		TVDataProvider.closeDatabase(this);
		super.onDestroy();
	}
}


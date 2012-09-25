package com.amlogic.tvservice;

import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVConst;
import java.io.File;

abstract public class TVDevice{

	public class DTVRecordParams{
		public File file;
		public TVProgram.Video video;
		public TVProgram.Audio audio[];
		public TVProgram.Subtitle subtitle[];
		public TVProgram.Teletext teletext[];
		public int currAudio;
		public int currSubtitle;
		public int currTeletext;
		public int recTotalSize;
		public int recTotalTime;
		public int recCurrPos;
	}

	public class Event{
		public static final int EVENT_SET_INPUT_SOURCE_OK     = 0;
		public static final int EVENT_SET_INPUT_SOURCE_FAILED = 1;
		public static final int EVENT_VCHIP_BLOCKED           = 2;
		public static final int EVENT_VCHIP_UNBLOCKED         = 3;
		public static final int EVENT_FRONTEND                = 4;
		public static final int EVENT_DTV_NO_DATA             = 5;
		public static final int EVENT_DTV_CANNOT_DESCRAMLE    = 6;
		public static final int EVENT_RECORD                  = 7;

		public int             type;
		public TVChannelParams feParams;
		public int             feStatus;
		public DTVRecordParams recParams;

		public void Event(int type){
			this.type = type;
		}
	}

	private int native_handle;
	private boolean destroy;

	private native void native_device_init();
	private native void native_device_destroy();
	private native void native_set_input_source(int src);
	private native void native_set_frontend(TVChannelParams params);
	private native TVChannelParams native_get_frontend();
	private native int  native_get_frontend_status();
	private native int  native_get_frontend_signal_strength();
	private native int  native_get_frontend_snr();
	private native int  native_get_frontend_ber();
	private native void native_free_frontend();
	private native void native_start_vbi(int flags);
	private native void native_stop_vbi(int flags);
	private native void native_play_atv();
	private native void native_stop_atv();
	private native void native_play_dtv(int vpid, int vfmt, int apid, int afmt);
	private native void native_stop_dtv();
	private native void native_start_recording(DTVRecordParams params);
	private native DTVRecordParams native_stop_recording();
	private native void native_start_timeshifting(DTVRecordParams params);
	private native DTVRecordParams native_stop_timeshifting();
	private native void native_start_playback(DTVRecordParams params);
	private native void native_stop_playback();
	private native void native_fast_forward(int speed);
	private native void native_fast_backward(int speed);
	private native void native_pause();
	private native void native_resume();
	private native void native_seek_to(int pos);

	static{
		System.loadLibrary("am_adp");
		System.loadLibrary("am_mw");
		System.loadLibrary("zvbi");
		System.loadLibrary("jnitvdevice");
	}

	public TVDevice(){
		destroy = false;
		native_device_init();
	}

	public void setInputSource(TVConst.SourceType source){
		native_set_input_source(source.ordinal());
	}

	public void setFrontend(TVChannelParams params){
		native_set_frontend(params);
	}

	public TVChannelParams getFrontend(){
		return native_get_frontend();
	}

	public int getFrontendStatus(){
		return native_get_frontend_status();
	}

	public int getFrontendSignalStrength(){
		return native_get_frontend_signal_strength();
	}

	public int getFrontendSNR(){
		return native_get_frontend_snr();
	}

	public int getFrontendBER(){
		return native_get_frontend_ber();
	}

	public void freeFrontend(){
		native_free_frontend();
	}

	public void startVBI(int flags){
		native_start_vbi(flags);
	}

	public void stopVBI(int flags){
		native_stop_vbi(flags);
	}

	public void playATV(){
		native_play_atv();
	}

	public void stopATV(){
		native_stop_atv();
	}

	public void playDTV(int vpid, int vfmt, int apid, int afmt){
		native_play_dtv(vpid, vfmt, apid, afmt);
	}

	public void stopDTV(){
		native_stop_dtv();
	}

	public void startRecording(DTVRecordParams params){
		native_start_recording(params);
	}

	public DTVRecordParams stopRecording(){
		return native_stop_recording();
	}

	public void startTimeshifting(DTVRecordParams params){
	}

	public DTVRecordParams stopTimeshifting(){
		return native_stop_timeshifting();
	}

	public void startPlayback(DTVRecordParams params){
		native_start_playback(params);
	}

	public void stopPlayback(){
		native_stop_playback();
	}

	public void pause(){
		native_pause();
	}

	public void resume(){
		native_resume();
	}

	public void fastForward(int speed){
		native_fast_forward(speed);
	}

	public void fastBackward(int speed){
		native_fast_backward(speed);
	}

	public void seekTo(int pos){
		native_seek_to(pos);
	}

	abstract public void onEvent(Event event);
	
	protected void finalize() throws Throwable {
		if(!destroy){
			destroy = false;
			native_device_destroy();
		}
	}
}


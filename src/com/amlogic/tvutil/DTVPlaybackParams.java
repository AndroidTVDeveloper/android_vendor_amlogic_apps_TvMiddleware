package com.amlogic.tvutil;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *TV回放参数
 */
public class DTVPlaybackParams implements Parcelable {
	public static final int PLAYBACK_ST_STOPPED  = 0;
	public static final int PLAYBACK_ST_PLAYING  = 1;
	public static final int PLAYBACK_ST_PAUSED   = 2;
	public static final int PLAYBACK_ST_FFFB     = 3;
	public static final int PLAYBACK_ST_EXIT     = 4;

	private String filePath;
	private int vPid;
	private int aPid;
	private int vFmt;
	private int aFmt;
	private int status;
	private long currentTime;
	private long totalTime;

	public static final Parcelable.Creator<DTVPlaybackParams> CREATOR = new Parcelable.Creator<DTVPlaybackParams>(){
		public DTVPlaybackParams createFromParcel(Parcel in) {
			return new DTVPlaybackParams(in);
		}
		public DTVPlaybackParams[] newArray(int size) {
			return new DTVPlaybackParams[size];
		}
	};

	public void readFromParcel(Parcel in){
		status = in.readInt();
		currentTime = in.readLong();
		totalTime = in.readLong();
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(status);
		dest.writeLong(currentTime);
		dest.writeLong(totalTime);
	}

	public DTVPlaybackParams(Parcel in){
		readFromParcel(in);
	}
	
	public DTVPlaybackParams(){

	}

	public DTVPlaybackParams(String filePath, long totalTime, TVProgram.Video video, TVProgram.Audio audio){
		status = PLAYBACK_ST_STOPPED;
		currentTime = 0;
		this.totalTime = totalTime;
		vPid = (video!=null) ? video.getPID() : 0x1fff;
		aPid = (audio!=null) ? audio.getPID() : 0x1fff;
		vFmt = (video!=null) ? video.getFormat() : -1;
		aFmt = (audio!=null) ? audio.getFormat() : -1;
		this.filePath = filePath;
	}

	/*public boolean equals(DTVPlaybackParams tp){
		if(tp.status != status ||
			tp.currentTime != currentTime ||
			tp.totalTime != totalTime)
			return false;

		return true;
	}*/

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<DTVPlaybackParams> getCreator() {
		return CREATOR;
	}

	public int getStatus(){
		return status;
	}

	public long getCurrentTime(){
		return currentTime;
	}

	public long getTotalTime(){
		return totalTime;
	}
}


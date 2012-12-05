package com.amlogic.tvactivity;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.Bundle;
import android.widget.VideoView;
import android.view.View;
import android.view.ViewGroup;
import android.view.SurfaceHolder;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.util.Log;
import java.lang.StringBuilder;
import com.amlogic.tvclient.TVClient;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVPlayParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConfigValue;
import com.amlogic.tvsubtitle.TVSubtitleView;

/**
 *TV Activity
 */
abstract public class TVActivity extends Activity
{
    private static final String TAG = "TVActivity";
    private static final int MSG_CONNECTED    = 1949;
    private static final int MSG_DISCONNECTED = 1950;
    private static final int MSG_MESSAGE      = 1951;

    private VideoView videoView;
    private TVSubtitleView subtitleView;
    private boolean connected = false;
    private int currProgramID = -1;
    private int currSubtitlePID = -1;
    private int currSubtitleID1 = -1;
    private int currSubtitleID2 = -1;

    private TVClient client = new TVClient() {
        public void onConnected() {
            Message msg = handler.obtainMessage(MSG_CONNECTED);
            handler.sendMessage(msg);
        }

        public void onDisconnected() {
            Message msg = handler.obtainMessage(MSG_DISCONNECTED);
            handler.sendMessage(msg);
        }

        public void onMessage(TVMessage m) {
            Message msg = handler.obtainMessage(MSG_MESSAGE, m);
            handler.sendMessage(msg);
        }
    };

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG, "handle message "+msg.what);
            switch(msg.what) {
            case MSG_CONNECTED:
            	connected = true;
            	initSubtitle();
                onConnected();
                break;
            case MSG_DISCONNECTED:
            	connected = false;
                onDisconnected();
                break;
            case MSG_MESSAGE:
            	solveMessage((TVMessage)msg.obj);
                onMessage((TVMessage)msg.obj);
                break;
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        client.connect(this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        if(subtitleView == null) {
        	unregisterConfigCallback("tv:subtitle:enable");
        	unregisterConfigCallback("tv:subtitle:language");
        	unregisterConfigCallback("tv:teletext:language");
        }

        client.disconnect(this);
        super.onDestroy();
    }

    private void restartSubtitle(){
		if(subtitleView == null)
			return;

		TVProgram prog = TVProgram.selectByID(this, currProgramID);

		/*Start subtitle*/
       	TVProgram.Subtitle sub;

       	sub = prog.getSubtitle(getStringConfig("tv:subtitle:language"));
       	if((sub != null) && (sub.getPID() != currSubtitlePID)){
       		boolean restart = false;

       		switch(sub.getType()){
				case TVProgram.Subtitle.TYPE_DVB_SUBTITLE:
					if(sub.getCompositionPageID() != currSubtitleID1 || sub.getAncillaryPageID() != currSubtitleID2){
						subtitleView.setSubParams(new TVSubtitleView.DVBSubParams(0, sub.getPID(), sub.getCompositionPageID(), sub.getAncillaryPageID()));
						currSubtitleID1 = sub.getCompositionPageID();
						currSubtitleID2 = sub.getAncillaryPageID();
						restart = true;
					}
					break;
				case TVProgram.Subtitle.TYPE_DTV_TELETEXT:
					int mag, pg, pgno;

					mag = sub.getMagazineNumber();
					pg  = sub.getPageNumber();

					if(mag != currSubtitleID1 || pg != currSubtitleID2){
						pgno = (mag==0) ? 800 : mag*100;
						pgno += pg;
						subtitleView.setSubParams(new TVSubtitleView.DTVTTParams(0, sub.getPID(), pgno, 0x3F7F));
						currSubtitleID1 = mag;
						currSubtitleID2 = pg;
						restart = true;
					}
					break;
			}

			if(restart){
				if(getBooleanConfig("tv:subtitle:enable"))
					subtitleView.show();
				else
					subtitleView.hide();

				currSubtitlePID = sub.getPID();
				subtitleView.startSub();
			}
		}
	}

	private void stopSubtitle(){
		if(subtitleView == null)
			return;

		currSubtitlePID = -1;
		subtitleView.stop();
    	subtitleView.hide();
	}

	/*On program started*/
    private void onProgramStart(int prog_id){
    	Log.d(TAG, "onProgramStart");

    	currProgramID = prog_id;

		/*Start subtitle*/
		restartSubtitle();
	}

	/*On program stopped*/
	private void onProgramStop(int prog_id){
		Log.d(TAG, "onProgramStop");

		currProgramID = -1;

    	/*Stop subtitle.*/
    	stopSubtitle();
	}

	/*On configure entry changed*/
	private void onConfigChanged(String name, TVConfigValue val) throws Exception{
		Log.d(TAG, "config "+name+" changed");

		if(name.equals("tv:subtitle:enable")){
			boolean v = val.getBoolean();

			Log.d(TAG, "tv:subtitle:enable changed -> "+v);
			if(subtitleView != null){
				if(v){
					subtitleView.show();
				}else{
					subtitleView.hide();
				}
			}
		}else if(name.equals("tv:subtitle:language")){
			String lang = val.getString();

			Log.d(TAG, "tv:subtitle:language changed -> "+lang);
			restartSubtitle();
		}else if(name.equals("tv:teletext:language")){
			String lang = val.getString();

			Log.d(TAG, "tv:teletext:language changed -> "+lang);
		}
	}

	/*Solve the TV message*/
    private void solveMessage(TVMessage msg){
    	switch(msg.getType()){
			case TVMessage.TYPE_PROGRAM_START:
				onProgramStart(msg.getProgramID());
				break;
			case TVMessage.TYPE_PROGRAM_STOP:
				onProgramStop(msg.getProgramID());
				break;
			case TVMessage.TYPE_CONFIG_CHANGED:
				try{
					onConfigChanged(msg.getConfigName(), msg.getConfigValue());
				}catch(Exception e){
					Log.e(TAG, "error in onConfigChanged");
				}
				break;
		}
	}

    /**
     *在到TVService的连接建立成功后被调用，子类中重载
     */
    abstract public void onConnected();

    /**
     *在到TVService的连接断开后被调用，子类中重载
     */
    abstract public void onDisconnected();

    /**
     *当接收到TVService发送的消息时被调用，子类中重载
     *@param msg TVService 发送的消息
     */
    abstract public void onMessage(TVMessage msg);

    SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            Log.d(TAG, "surfaceChanged");
            //initSurface(holder);
        }
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            try {
                initSurface(holder);
            } catch(Exception e) {
            }
        }
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
        }
        private void initSurface(SurfaceHolder h) {
            Canvas c = null;
            try {
                Log.d(TAG, "initSurface");
                c = h.lockCanvas();
            }
            finally {
                if (c != null)
                    h.unlockCanvasAndPost(c);
            }
        }
    };

    private void initSubtitle(){
    	if(subtitleView == null)
    		return;

    	if(!connected)
    		return;

		subtitleView.setMargin(
				getIntConfig("tv:subtitle:margin_left"),
				getIntConfig("tv:subtitle:margin_top"),
				getIntConfig("tv:subtitle:margin_right"),
				getIntConfig("tv:subtitle:margin_bottom"));

		Log.d(TAG, "register subtitle/teletext config callbacks");
		registerConfigCallback("tv:subtitle:enable");
		registerConfigCallback("tv:subtitle:language");
		registerConfigCallback("tv:teletext:language");
	}

    /**
     *在Activity上创建VideoView和SubtitleView
     */
    public void openVideo() {
        Log.d(TAG, "openVideo");

        ViewGroup root = (ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content);

        if(subtitleView == null) {
            Log.d(TAG, "create subtitle view");
            subtitleView = new TVSubtitleView(this);
            root.addView(subtitleView, 0);

            initSubtitle();
        }

        if(videoView == null) {
            Log.d(TAG, "create video view");
            videoView = new VideoView(this);
            root.addView(videoView, 0);
            videoView.getHolder().addCallback(surfaceHolderCallback);
            videoView.getHolder().setFormat(PixelFormat.VIDEO_HOLE);
        }
    }

    /**
	 *计算本地时间
	 *@param utc UTC时间
	 *@return 返回本地时间
	 */
    public long getLocalTime(long utc){
    	return client.getLocalTime(utc);
	}

	/**
	 *取得当前本地时间
	 *@return 返回本地时间
	 */
    public long getLocalTime(){
    	return client.getLocalTime();
	}

	/**
	 *计算UTC时间
	 *@param local 本地时间
	 *@return 返回UTC时间
	 */
	public long getUTCTime(long local){
    	return client.getUTCTime(local);
	}

	/**
	 *取得当前UTC时间
	 *@return 返回UTC时间
	 */
	public long getUTCTime(){
		return client.getUTCTime();
	}

    /**
     *设定TV输入源
     *@param source 输入源(TVStatus.SOURCE_XXXX)
     */
    public void setInputSource(TVConst.SourceInput source) {
        client.setInputSource(source.ordinal());
    }
    
    /**
     *得到当前信号源
     *@return 返回当前信号源
     */
    public TVConst.SourceInput   getCurInputSource(){
    	return client.getCurInputSource();
    }

	/**
	 *在数字电视模式下，设定节目类型是电视或广播
	 *@param type 节目类型TVProgram.TYPE_TV/TVProgram.TYPE_RADIO
	 */
    public void setProgramType(int type){
    	client.setProgramType(type);
	}
    		
    /**
     *停止播放节目
     */
    public void stopPlaying() {
        client.stopPlaying();
    }

    /**
     *开始时移播放
     */
    public void startTimeshifting() {
        client.startTimeshifting();
    }

    /**
     *开始录制
     *@param bookingID 要录制节目的预约记录ID
     */
    public void startRecording(int bookingID) {
        client.startRecording(bookingID);
    }

    /**
     *开始录制当前节目
     */
    public void startRecording() {
        client.startRecording(-1);
    }

    /**
     *停止当前节目录制
     */
    public void stopRecording() {
        client.stopRecording();
    }

    /**
     *开始录制节目回放
     *@param bookingID 录制节目的预约记录ID
     */
    public void startPlayback(int bookingID) {
        client.startPlayback(bookingID);
    }

    /**
     *开始搜索频道
     *@param sp 搜索参数
     */
    public void startScan(TVScanParams sp) {
        client.startScan(sp);
    }

    /**
     *停止频道搜索
     *@param store true表示保存搜索结果，false表示不保存直接退出
     */
    public void stopScan(boolean store) {
        client.stopScan(store);
    }

    /**
     *播放下一频道节目
     */
    public void channelUp() {
        client.channelUp();
    }

    /**
     *播放上一频道节目
     */
    public void channelDown() {
        client.channelDown();
    }

    /**
     *根据频道号播放节目
     *@param no 频道号
     */
    public void playProgram(TVProgramNumber no) {
        client.playProgram(no);
    }
    
    /**
     *根据节目ID播放
     *@param id 节目ID
     */
    public void playProgram(int id) {
        client.playProgram(id);
    }

    /**
     *在回放和时移模式下暂停播放
     */
    public void pause() {
        client.pause();
    }

    /**
     *在回放和时移模式下恢复播放
     */
    public void resume() {
        client.resume();
    }

    /**
     *在回放和时移模式下快进播放
     *@param speed 播放速度，1为正常速度，2为2倍速播放
     */
    public void fastForward(int speed) {
    	client.fastForward(speed);
    }

    /**
     *在回放和时移模式下快退播放
     *@param speed 播放速度，1为正常速度，2为2倍速播放
     */
    public void fastBackward(int speed) {
    	client.fastBackward(speed);
    }

    /**
     *在回放和时移模式下移动到指定位置
     *@param pos 移动位置，从文件头开始的秒数
     */
    public void seekTo(int pos) {
    	client.seekTo(pos);
    }

    /**
     *显示Teletext
     */
    public void ttShow() {
        if(subtitleView == null)
            return;
    }

    /**
     *隐藏Teletext
     */
    public void ttHide() {
        if(subtitleView == null)
            return;
    }

    /**
     *跳到Teletext的下一页
     */
    public void ttGotoNextPage() {
        if(subtitleView == null)
            return;

        subtitleView.nextPage();
    }

    /**
     *跳到Teletext的上一页
     */
    public void ttGotoPreviousPage() {
        if(subtitleView == null)
            return;

        subtitleView.previousPage();
    }

    /**
     *跳到Teletext的指定页
     *@param page 页号
     */
    public void ttGotoPage(int page) {
        if(subtitleView == null)
            return;

        subtitleView.gotoPage(page);
    }

    /**
     *跳转到Teletext home页
     */
    public void ttGoHome() {
        if(subtitleView == null)
            return;

        subtitleView.goHome();
    }

    /**
     *根据颜色进行Teletext跳转
     *@param color 0:红 1:绿 2:黄 3:蓝
     */
    public void ttGotoColorLink(int color) {
        if(subtitleView == null)
            return;

        subtitleView.colorLink(color);
    }

    /**
     *设定Teletext搜索匹配字符串
     *@param pattern 匹配字符串
     *@param casefold 分否区分大小写
     */
    public void ttSetSearchPattern(String pattern, boolean casefold) {
        if(subtitleView == null)
            return;

        subtitleView.setSearchPattern(pattern, casefold);
    }

    /**
     *根据设定的匹配字符串搜索下一个Teletext页
     */
    public void ttSearchNext() {
        if(subtitleView == null)
            return;

        subtitleView.searchNext();
    }

    /**
     *根据设定的匹配字符串搜索上一个Teletext页
     */
    public void ttSearchPrevious() {
        if(subtitleView == null)
            return;

        subtitleView.searchPrevious();
    }

    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
    public void setConfig(String name, TVConfigValue value) {
        client.setConfig(name, value);
    }
    
    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
    public void setConfig(String name, boolean value) {
        client.setConfig(name, value);
    }
    
    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
    public void setConfig(String name, int value) {
        client.setConfig(name, value);
    }
    
    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
    public void setConfig(String name, String value) {
        client.setConfig(name, value);
    }

	/**
	 *取得布尔型配置项值
	 *@param name 配置项名称
	 *@return 返回配置项值
	 */
	public boolean getBooleanConfig(String name){
		return client.getBooleanConfig(name);
	}

	/**
	 *取得整型配置项值
	 *@param name 配置项名称
	 *@return 返回配置项值
	 */
	public int getIntConfig(String name){
		return client.getIntConfig(name);
	}

	/**
	 *取得字符串型配置项值
	 *@param name 配置项名称
	 *@return 返回配置项值
	 */
	public String getStringConfig(String name){
		return client.getStringConfig(name);
	}

    /**
     *读取配置选项
     *@param name 配置选项名
     *@return 返回配置选项值
     */
    public TVConfigValue getConfig(String name) {
        return client.getConfig(name);
    }

    /**
     *注册配置选项回调，当选项修改时，onMessage会被调用
     *@param name 配置选项名
     */
    public void registerConfigCallback(String name) {
        client.registerConfigCallback(name);
    }

    /**
     *注销配置选项
     *@param name 配置选项名
     */
    public void unregisterConfigCallback(String name) {
        client.unregisterConfigCallback(name);
    }

    /**
     *取得前端锁定状态
     *@return 返回锁定状态
     */
    public int getFrontendStatus() {
        return client.getFrontendStatus();
    }

    /**
     *取得前端信号强度
     *@return 返回信号强度
     */
    public int getFrontendSignalStrength() {
        return client.getFrontendSignalStrength();
    }

    /**
     *取得前端SNR值
     *@return 返回SNR值
     */
    public int getFrontendSNR() {
        return client.getFrontendSNR();
    }

    /**
     *取得前端BER值
     *@return 返回BER值
     */
    public int getFrontendBER() {
        return client.getFrontendBER();
    }
}


package com.brianledbetter.kwplogger;

/**
 * Created by b3d on 12/19/15.
 */
// Android = facepalm
public class StateSingleton {
    private static StateSingleton mInstance = null;
    private boolean mIsConnected = false;
    private boolean mIsConnecting = false;

    public static synchronized StateSingleton getInstance(){
        if(mInstance == null)
        {
            mInstance = new StateSingleton();
        }
        return mInstance;
    }

    public boolean getIsConnected() {
        return this.mIsConnected;
    }

    public void setIsConnected(boolean value){
        mIsConnected = value;
    }

    public boolean getIsConnecting() {
        return this.mIsConnecting;
    }

    public void setIsConnecting(boolean value){
        mIsConnecting = value;
    }
}

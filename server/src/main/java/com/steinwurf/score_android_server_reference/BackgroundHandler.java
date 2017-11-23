package com.steinwurf.score_android_server_reference;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class BackgroundHandler {

    interface OnPostFinishedListener {
        void finished();
    }

    private static final String TAG = BackgroundHandler.class.getSimpleName();
    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * Starts a background thread and its {@link Handler}.
     */
    void start() {
        mBackgroundThread = new HandlerThread(TAG);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    void post(final Runnable runnable, final OnPostFinishedListener onPostFinishedListener)
    {
        post(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                onPostFinishedListener.finished();
            }
        });
    }

    void post(Runnable runnable)
    {
        mBackgroundHandler.post(runnable);
    }

    Handler getHandler()
    {
        return mBackgroundHandler;
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    void stop() {
        Log.d(TAG, "stopBackgroundThread");
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

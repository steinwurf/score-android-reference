package com.steinwurf.score.shared;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

public class BackgroundHandler {

    public interface OnPostFinishedListener {
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
    public void start() {
        mBackgroundThread = new HandlerThread(TAG);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    public void post(final Runnable runnable, final OnPostFinishedListener onPostFinishedListener)
    {
        if (mBackgroundHandler == null)
            throw new IllegalStateException();
        post(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                onPostFinishedListener.finished();
            }
        });
    }

    public void post(Runnable runnable)
    {
        if (mBackgroundHandler == null)
            throw new IllegalStateException();
        mBackgroundHandler.post(runnable);
    }

    public Handler getHandler()
    {
        if (mBackgroundHandler == null)
            throw new IllegalStateException();
        return mBackgroundHandler;
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    public void stop() {
        if (mBackgroundThread == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBackgroundThread.quitSafely();
        }
        else
        {
            mBackgroundThread.quit();
        }
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
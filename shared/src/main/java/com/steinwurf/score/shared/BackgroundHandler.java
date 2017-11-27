package com.steinwurf.score.shared;
/*-
 * Copyright (c) 2017 Steinwurf ApS
 * All Rights Reserved
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;

/**
 * A wrapper of a Handler and a Handler thread.
 */
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
     * Start a background thread and its {@link Handler}.
     */
    public void start() {
        mBackgroundThread = new HandlerThread(TAG);
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Post a @link {@link Runnable} and have the @{@link OnPostFinishedListener} be called
     * afterwards.
     * @param runnable The runnable to run on the background thread.
     * @param onPostFinishedListener The callback to call once the runnable has finished executing.
     */
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

    /**
     * Post a @link {@link Runnable} to run on the background thread.
     * @param runnable The runnable to run on the background thread.
     */
    public void post(Runnable runnable)
    {
        if (mBackgroundHandler == null)
            throw new IllegalStateException();
        mBackgroundHandler.post(runnable);
    }

    /**
     * Get the handler. This must be called after start.
     * @return The handler
     */
    public Handler getHandler()
    {
        if (mBackgroundHandler == null)
            throw new IllegalStateException();
        return mBackgroundHandler;
    }

    /**
     * Stop the background thread and {@link Handler}.
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

package com.steinwurf.score.client_reference;
/*-
 * Copyright (c) 2017 Steinwurf ApS
 * All Rights Reserved
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class KeepAlive
{
    private static final String TAG = KeepAlive.class.getSimpleName();
    private final InetAddress host;
    private final int port;
    private int mInterval;
    private Thread mThread = null;
    private boolean mRunning = false;

    public static KeepAlive createKeepAlive(WifiManager wm, int keepAliveInterval) {
        try
        {
            String gatewayIP = "192.168.0.1";
            DhcpInfo dhcp = wm.getDhcpInfo();
            if (dhcp != null)
            {
                //noinspection deprecation
                gatewayIP = Formatter.formatIpAddress(dhcp.gateway);
            }
            return new KeepAlive(InetAddress.getByName(gatewayIP), 13337, keepAliveInterval);
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private KeepAlive(InetAddress host, int port, int interval)
    {
        this.host = host;
        this.port = port;
        mInterval = interval;
    }

    public void setInterval(int interval)
    {
        mInterval = interval;
    }

    public void start()
    {
        Log.d(TAG, "started: " + host + ":" + port);
        mThread = new Thread(() -> {
            DatagramSocket socket = null;
            try
            {
                socket = new DatagramSocket(null);
                mRunning = true;
                while (mRunning) {
                    byte[] buffer = {0x66};
                    DatagramPacket out = new DatagramPacket(buffer, buffer.length, host, port);
                    socket.send(out);
                    Thread.sleep(mInterval);
                }
            }
            catch (IOException | InterruptedException e)
            {
                e.printStackTrace();
            }
            finally {
                if (socket != null)
                    socket.close();
            }
        });
        mThread.start();
    }

    public void stop()
    {
        Log.d(TAG, "stopped");
        mRunning = false;
        if (mThread != null) {
            try {
                mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mThread = null;
        }
    }
}

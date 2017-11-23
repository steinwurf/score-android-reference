package com.steinwurf.score_android_server_reference;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

class Server {

    private static final String TAG = Server.class.getSimpleName();

    interface OnStateChangeListener
    {
        void onServerStarted();
        void onServerError(String reason);
    }

    private final byte[] receiveBuffer = new byte[4096];

    final private OnStateChangeListener onStateChangeListener;

    private Thread connectionThread = null;
    private MulticastSocket socket;

    private Integer port = null;
    private InetAddress ip = null;

    Server(@NotNull OnStateChangeListener onStateChangeListener)
    {
        this.onStateChangeListener = onStateChangeListener;
    }

    void start(final String ipString, final String portString) {
        connectionThread = new Thread(new Runnable()
        {
            @Override
            public void run() {
                try {
                    port = Integer.parseInt(portString);
                    socket = new MulticastSocket(port);
                    ip = InetAddress.getByName(ipString);
                    socket.setLoopbackMode(/*disabled=*/ true);
                    socket.joinGroup(ip);

                    Log.d(TAG, "started");
                    onStateChangeListener.onServerStarted();

                    // Read
                    while (!socket.isClosed())
                    {
                        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(packet);
                        handleSnack(packet);
                    }

                } catch (IOException | NumberFormatException e) {
                    if (isRunning())
                        onStateChangeListener.onServerError(e.toString());
                }
            }
        });
        connectionThread.start();
        Log.d(TAG, "started");
    }

    void stop() {
        if (socket != null) {
            socket.close();
        }

        if (connectionThread != null && Thread.currentThread() != connectionThread) {
            try {
                connectionThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "stopped");
    }

    private void handleSnack(DatagramPacket packet) {
    }

    boolean isRunning()
    {
        return socket != null && !socket.isClosed();
    }

    public void sendData(byte[] data) {
        if (isRunning())
        {
            DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

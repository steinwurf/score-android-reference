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

    void start(String ipString, String portString) {
        try {
            port = Integer.parseInt(portString);
            socket = new MulticastSocket(port);
            ip = InetAddress.getByName(ipString);
            socket.setLoopbackMode(/*disabled=*/ true);
            socket.joinGroup(ip);
        } catch (IOException e) {
            e.printStackTrace();
        }

        connectionThread = new Thread(new Runnable()
        {
            @Override
            public void run() {
                try {
                    // Read
                    while (isRunning())
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
    }

    private boolean isRunning()
    {
        return socket != null && !socket.isClosed();
    }

    private void handleSnack(DatagramPacket packet) {
    }

    void sendData(byte[] data) {
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

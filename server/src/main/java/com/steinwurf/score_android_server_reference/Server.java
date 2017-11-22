package com.steinwurf.score_android_server_reference;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

class Server {

    private static final String TAG = Server.class.getSimpleName();

    interface IServerHandler
    {
        void onServerStarted();
        void onServerError(String reason);
    }

    private final byte[] receiveBuffer = new byte[4096];

    final private IServerHandler handler;

    private Thread connectionThread = null;
    private MulticastSocket socket;

    private Integer port = null;
    private InetAddress ip = null;

    Server(IServerHandler handler)
    {
        this.handler = handler;
    }

    void start(final String ipString, final String portString) {
        connectionThread = new Thread(new Runnable()
        {
            @Override
            public void run() {
                try {
                    port = Integer.parseInt(portString);
                    ip = InetAddress.getByName(ipString);
                    socket = new MulticastSocket(port);
                    socket.setLoopbackMode(/*disabled=*/ true);
                    socket.joinGroup(ip);

                    Log.d(TAG, "started");
                    if (handler != null)
                        handler.onServerStarted();

                    // Read
                    while (!socket.isClosed())
                    {
                        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(packet);
                        handleSnack(packet);
                    }

                } catch (IOException | NumberFormatException e) {
                    e.printStackTrace();
                    if (handler != null) {
                        handler.onServerError(e.toString());
                    }
                }
            }
        });
        connectionThread.start();
    }

    public void stop() {

        if (socket != null) {
            socket.close();
        }

        if (Thread.currentThread() != connectionThread) {
            try {
                connectionThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSnack(DatagramPacket packet) {
    }

    public void sendData(byte[] data) {

        if (socket != null)
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

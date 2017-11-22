package com.steinwurf.score_android_client_reference;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

class Client {

    private static final String TAG = Client.class.getSimpleName();

    interface IClientHandler
    {
        void onClientStarted();
        void onClientError(String reason);
    }

    private final byte[] receiveBuffer = new byte[4096];

    private final IClientHandler handler;

    private Thread connectionThread = null;
    private MulticastSocket socket;

    Client(IClientHandler handler)
    {
        this.handler = handler;
    }

    void start(final String ipString, final String portString) {
        connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int port = Integer.parseInt(portString);
                    InetAddress ip = InetAddress.getByName(ipString);
                    socket = new MulticastSocket(port);
                    socket.joinGroup(ip);

                    Log.d(TAG, "started");
                    if (handler != null)
                        handler.onClientStarted();

                    // Read
                    while(!socket.isClosed())
                    {
                        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(packet);
                        handleData(packet);
                        sendSnacks(packet.getSocketAddress());
                    }

                } catch (IOException | NumberFormatException e) {
                    e.printStackTrace();
                    if (handler != null)
                        handler.onClientError(e.toString());
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

    private void sendSnacks(SocketAddress socketAddress) throws IOException {
        ArrayList<byte[]> snackPackets = new ArrayList<>();
        for (byte[] snackPacket : snackPackets) {
            socket.send(new DatagramPacket(snackPacket, snackPacket.length, socketAddress));
        }
    }

    private void handleData(DatagramPacket packet) {
        ByteBuffer buffer = ByteBuffer.wrap(
                packet.getData(),
                packet.getOffset(),
                packet.getLength());
    }

}

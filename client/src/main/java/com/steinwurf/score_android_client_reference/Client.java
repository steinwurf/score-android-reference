package com.steinwurf.score_android_client_reference;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

class Client {

    private static final String TAG = Client.class.getSimpleName();

    interface OnStateChangeListener
    {
        void onClientError(String reason);
        void onData(ByteBuffer data);
    }

    private final byte[] receiveBuffer = new byte[64*1024];

    private final OnStateChangeListener onStateChangeListener;

    private Thread connectionThread = null;
    private MulticastSocket socket;

    Client(@NotNull OnStateChangeListener onStateChangeListener)
    {
        this.onStateChangeListener = onStateChangeListener;
    }

    void start(final String ipString, final String portString) {
        try {
            int port = Integer.parseInt(portString);
            socket = new MulticastSocket(port);
            InetAddress ip = InetAddress.getByName(ipString);
            socket.setLoopbackMode(/*disabled=*/ true);
            socket.joinGroup(ip);
        } catch (IOException e) {
            e.printStackTrace();
        }

        connectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Read
                    while(isRunning())
                    {
                        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(packet);
                        handleData(packet);
                        sendSnacks(packet.getSocketAddress());
                    }

                } catch (IOException | NumberFormatException e) {
                    if (isRunning())
                        onStateChangeListener.onClientError(e.toString());
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
        onStateChangeListener.onData(buffer);
    }

}

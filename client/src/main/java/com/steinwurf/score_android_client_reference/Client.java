package com.steinwurf.score_android_client_reference;

import com.steinwurf.score.shared.BackgroundHandler;
import com.steinwurf.score.sink.Sink;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

class Client {

    private static final String TAG = Client.class.getSimpleName();

    interface OnStateChangeListener
    {
        void onError(String reason);
        void onData(ByteBuffer data);
    }

    private final byte[] receiveBuffer = new byte[64*1024];

    private final OnStateChangeListener onStateChangeListener;

    private final BackgroundHandler backgroundHandler = new BackgroundHandler();
    private MulticastSocket socket;
    private Sink sink;

    Client(@NotNull OnStateChangeListener onStateChangeListener)
    {
        this.onStateChangeListener = onStateChangeListener;
    }

    void start(final String ipString, final String portString) {
        sink = new Sink();
        try {
            int port = Integer.parseInt(portString);
            socket = new MulticastSocket(port);
            InetAddress ip = InetAddress.getByName(ipString);
            socket.setLoopbackMode(/*disabled=*/ true);
            socket.joinGroup(ip);
        } catch (IOException e) {
            e.printStackTrace();
        }
        backgroundHandler.start();
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    // Read
                    DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(packet);
                    handleData(ByteBuffer.wrap(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength()));
                    sendSnacks(packet.getSocketAddress());
                    if (isRunning())
                        backgroundHandler.post(this);
                } catch (IOException | NumberFormatException e) {
                    if (isRunning())
                        onStateChangeListener.onError(e.toString());
                }
            }
        });
    }

    void stop() {
        if (socket != null) {
            socket.close();
        }
        backgroundHandler.stop();
    }

    private boolean isRunning()
    {
        return socket != null && !socket.isClosed();
    }

    private void sendSnacks(SocketAddress socketAddress) throws IOException {
        ArrayList<byte[]> snackPackets = new ArrayList<>();
        while (sink.hasSnackPacket())
        {
            byte[] snackPacket = sink.getSnackPacket();
            snackPackets.add(snackPacket);
        }

        for (byte[] snackPacket : snackPackets) {
            socket.send(new DatagramPacket(snackPacket, snackPacket.length, socketAddress));
        }
    }

    private void handleData(ByteBuffer buffer) {
        try {
            sink.readDataPacket(buffer.array(), buffer.position(), buffer.remaining());
        } catch (Sink.InvalidDataPacketException e) {
            e.printStackTrace();
        }
        while (sink.hasMessage())
        {
            try {
                byte[] message = sink.getMessage();
                onStateChangeListener.onData(ByteBuffer.wrap(message));
            } catch (Sink.InvalidChecksumException e) {
                e.printStackTrace();
            }
        }
    }
}

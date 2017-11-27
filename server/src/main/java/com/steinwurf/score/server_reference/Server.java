package com.steinwurf.score.server_reference;

import com.steinwurf.score.shared.BackgroundHandler;
import com.steinwurf.score.source.Source;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

class Server {

    private static final String TAG = Server.class.getSimpleName();

    interface OnStateChangeListener
    {
        void onError(String reason);
    }

    private final byte[] receiveBuffer = new byte[64*1024];

    final private OnStateChangeListener onStateChangeListener;

    private final BackgroundHandler backgroundHandler = new BackgroundHandler();
    private MulticastSocket socket;
    private Source source;

    private Integer port = null;
    private InetAddress ip = null;

    Server(@NotNull OnStateChangeListener onStateChangeListener)
    {
        this.onStateChangeListener = onStateChangeListener;
    }

    void start(String ipString, String portString) {
        source = new Source();
        try {
            port = Integer.parseInt(portString);
            socket = new MulticastSocket(port);
            ip = InetAddress.getByName(ipString);
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
                    handleSnack(packet);
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

    synchronized private void handleSnack(DatagramPacket packet) {
        try {
            source.readSnackPacket(packet.getData(), packet.getOffset(), packet.getLength());
        } catch (Source.InvalidSnackPacketException e) {
            e.printStackTrace();
        }
    }

    synchronized void sendMessage(byte[] message) {
        if (isRunning())
        {
            source.readMessage(message);
            while(source.hasDataPacket())
            {
                byte[] data = source.getDataPacket();
                DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

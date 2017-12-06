package com.steinwurf.score.client_reference;
/*-
 * Copyright (c) 2017 Steinwurf ApS
 * All Rights Reserved
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

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

/**
 * Client for handling incoming score packets
 */
public class Client {

    private static final String TAG = Client.class.getSimpleName();

    /**
     * Interface for managing the various events
     */
    public interface OnEventListener
    {
        void onError(String reason);
        void onData(ByteBuffer data);
    }

    /**
     * Buffer for holding the incoming data packets
     */
    private final byte[] receiveBuffer = new byte[64*1024];

    /**
     * The event listener
     */
    private final OnEventListener onEventListener;

    /**
     * Handler for the work to be done in the background
     */
    private final BackgroundHandler backgroundHandler = new BackgroundHandler();

    /**
     * The multicast socket used for receiving multicast packets
     */
    private MulticastSocket socket;

    /**
     * The score sink which transforms data packets into messages
     */
    private Sink sink;

    /**
     * Construct a Client
     * @param onEventListener The event listener for handling the events caused by this Client
     */
    public Client(@NotNull OnEventListener onEventListener)
    {
        this.onEventListener = onEventListener;
    }

    /**
     * Start the client and connect to the given ip and port
     * @param ipString The ip to listen to.
     * @param portString The port to listen to.
     */
    public void start(final String ipString, final String portString) {
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
                        onEventListener.onError(e.toString());
                }
            }
        });
    }

    /**
     * Stop the client.
     */
    public void stop() {
        if (socket != null) {
            socket.close();
        }
        backgroundHandler.stop();
    }

    /**
     * Determine if the client's socket is available and open
     * @return if the client's socket is open true, otherwise false
     */
    private boolean isRunning()
    {
        return socket != null && !socket.isClosed();
    }

    /**
     * Send the Score sink's snack packets, if any.
     * @param socketAddress The address to send the snack packets to.
     * @throws IOException Throws if the socket was unable to send a snack packet.
     */
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

    /**
     * Handle an incoming data packet by feeding it to the sink. If this causes a message to become
     * available the {@link Client.OnEventListener#onData(ByteBuffer)} is triggered.
     * @param buffer the data packet.
     */
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
                onEventListener.onData(ByteBuffer.wrap(message));
            } catch (Sink.InvalidChecksumException e) {
                e.printStackTrace();
            }
        }
    }
}

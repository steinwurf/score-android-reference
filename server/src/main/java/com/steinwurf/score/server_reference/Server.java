package com.steinwurf.score.server_reference;
/*-
 * Copyright (c) 2017 Steinwurf ApS
 * All Rights Reserved
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

import android.app.ActivityManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;

import com.steinwurf.score.shared.BackgroundHandler;
import com.steinwurf.score.source.Source;
import com.steinwurf.score.source.AutoSource;
import com.steinwurf.score.source.InvalidSnackPacketException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Server for sending score packets
 */
class Server {

    private static final String TAG = Server.class.getSimpleName();

    /**
     * Interface for managing the various events
     */
    interface OnEventListener
    {
        void onError(String reason);
    }

    /**
     * Buffer for holding the incoming data packets
     */
    private final byte[] receiveBuffer = new byte[64*1024];

    /**
     * The event listener
     */
    final private OnEventListener onEventListener;

    /**
     * Handler for the work to be done in the background
     */
    private final BackgroundHandler backgroundHandler = new BackgroundHandler();

    /**
     * The multicast socket used for sending multicast packets
     */
    private MulticastSocket socket;

    /**
     * The score source which transforms message into data packets
     */
    private Source source;

    /**
     * The port to send data packets to.
     */
    private Integer port = null;

    /**
     * The IP to send data packets to.
     */
    private InetAddress ip = null;

    /**
     * Construct a Server
     * @param onEventListener The event listener for handling the events caused by this Server
     */
    Server(@NotNull OnEventListener onEventListener)
    {
        this.onEventListener = onEventListener;
    }

    /**
     * Start the server and connect to the given ip and port
     * @param ipString The ip to send to.
     * @param portString The port to send to.
     */
    void start(String ipString, String portString) {
        AutoSource autoSource = new AutoSource();
        autoSource.setSymbolSize(750);
        autoSource.setGenerationSize(50);

        source = autoSource;
        port = Integer.parseInt(portString);

        try {
            socket = new MulticastSocket(port);
            ip = InetAddress.getByName(ipString);
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
                        onEventListener.onError(e.toString());
                }

            }
        });
    }

    /**
     * Stop the server.
     */
    void stop() {
        if (socket != null) {
            socket.close();
        }

        backgroundHandler.stop();
    }

    /**
     * Determine if the server's socket is available and open
     * @return if the server's socket is open true, otherwise false
     */
    private boolean isRunning()
    {
        return socket != null && !socket.isClosed();
    }

    /**
     * Handle incoming snack packets. This must be done synchronously as this may change the state
     * of the source.
     * @param packet The snack packet to read.
     */
    synchronized private void handleSnack(DatagramPacket packet) {
        try {
            source.readSnackPacket(packet.getData(), packet.getOffset(), packet.getLength());
        } catch (InvalidSnackPacketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a given message by letting it become consumed by the source and then let it be converted
     * to data packets.
     * @param message The message to end to the clients.
     */
    synchronized void sendMessage(byte[] message) {
        if (isRunning())
        {
            source.readMessage(message);
            emptySource();
        }
    }

    private void emptySource() {
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

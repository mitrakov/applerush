package ru.mitrakov.self.rush.net;

import java.net.*;
import java.util.Arrays;
import java.io.IOException;


/**
 * Created by mitrakov on 23.02.2017
 */

public class Network extends Thread {
    public interface IHandler {
        void handle(int[] data);
    }

    private static final int BUF_SIZ = 1024;
    private static final int HEADER_SIZ = 7;

    // on Android don't forget to add "<uses-permission android:name="android.permission.INTERNET"/>" to manifest
    // otherwise new DatagramSocket() throws PermissionDeniedException
    private final DatagramSocket socket = new DatagramSocket();
    private final IHandler handler;
    private final UncaughtExceptionHandler errorHandler;

    private int sid = 0;
    private long token = 0;

    public Network(IHandler handler, UncaughtExceptionHandler errorHandler) throws IOException {
        assert handler != null;
        this.handler = handler;
        this.errorHandler = errorHandler;
        setDaemon(true);
        setName("Network thread");
        setUncaughtExceptionHandler(errorHandler);
    }

    @Override
    public void run() {
        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                DatagramPacket datagram = new DatagramPacket(new byte[BUF_SIZ], BUF_SIZ);
                socket.receive(datagram);
                int[] data = new int[datagram.getLength()];
                for (int i = 0; i < datagram.getLength(); i++) {
                    data[i] = datagram.getData()[i] >= 0 ? datagram.getData()[i] : datagram.getData()[i] + 256;
                }
                if (data.length > HEADER_SIZ) {
                    sid = data[0] * 256 + data[1];
                    token = (data[2] << 24) | (data[3] << 16) | (data[4] << 8) | data[5];
                    // @mitrakov: on Android copyOfRange requires minSdkVersion=9
                    handler.handle(Arrays.copyOfRange(data, HEADER_SIZ, data.length));
                }
            } catch (Exception e) {
                errorHandler.uncaughtException(this, e);
            }
        }
    }

    public void send(byte[] data) throws IOException {
        // concatenate a header and data
        byte[] msg = new byte[data.length + HEADER_SIZ];
        msg[0] = (byte) (sid / 256);
        msg[1] = (byte) (sid % 256);
        msg[2] = (byte) ((token >> 24) & 0xFF);
        msg[3] = (byte) ((token >> 16) & 0xFF);
        msg[4] = (byte) ((token >> 8) & 0xFF);
        msg[5] = (byte) (token & 0xFF);
        msg[6] = 0; // flags
        System.arraycopy(data, 0, msg, HEADER_SIZ, data.length);

        // sending
        socket.send(new DatagramPacket(msg, msg.length, InetAddress.getByName("192.168.1.2"), 33996));
    }

    public void reset() {
        sid = 0;
        token = 0;
    }
}

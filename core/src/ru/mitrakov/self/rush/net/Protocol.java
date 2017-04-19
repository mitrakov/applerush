package ru.mitrakov.self.rush.net;

import java.net.*;
import java.io.IOException;

import static ru.mitrakov.self.rush.utils.Utils.*;

/**
 * Created by mitrakov on 31.03.2017
 */
public class Protocol implements IProtocol {
    final static int N = 256;
    final static int SYN = 0;
    final static int MAX_ATTEMPTS = 12;
    final static int PERIOD = 10;
    final static int DEFAULT_SRTT = 2;
    final static float RC = .8f;
    final static int AC = 3;

    static class Item {
        boolean ack = false;
        int startRtt = 0;
        int ticks = 0;
        int attempt = 0;
        int nextRepeat = 0;
        int[] msg;

        Item(int[] msg) {
            this.msg = msg;
        }

        Item(int[] msg, int startRtt) {
            this.msg = msg;
            this.startRtt = startRtt;
        }
    }

    static int next(int n) {
        int result = (n + 1) % N;
        return result != SYN ? result : next(result);
    }

    static boolean after(int x, int y) {
        return (y - x + N) % N > N / 2;
    }

    private final Sender sender;
    private final Receiver receiver;
    private final IHandler handler;

    public Protocol(DatagramSocket socket, InetAddress addr, int port, IHandler handler) {
        assert socket != null && addr != null && handler != null && 0 < port && port < 65536;
        this.handler = handler;
        sender = new Sender(socket, addr, port, this);
        receiver = new Receiver(socket, addr, port, handler, this);
    }

    @Override
    public void connect() throws IOException {
        sender.connect();
    }

    @Override
    public void send(int[] data) throws IOException {
        sender.send(data);
    }

    @Override
    public void onReceived(int[] data) throws IOException {
        assert data != null && data.length > 0;
        if (data.length == 1) // Ack
            sender.onAck(data[0]);
        else receiver.onMsg(data[data.length - 1], copyOfRange(data, 0, data.length - 1));
    }

    @Override
    public void onSenderConnected() throws IOException {
        System.out.println("Sender connected!");
    }

    @Override
    public void onReceiverConnected() throws IOException {
        System.out.println("Receiver connected!");
        handler.onChanged(true);
    }

    @Override
    public void connectionFailed() throws IOException {
        System.out.println("Connection failed!");
        handler.onChanged(false);
    }

    @Override
    public void close() {
        sender.close();
    }

    @Override
    public boolean isConnected() {
        return sender.connected && receiver.connected;
    }
}

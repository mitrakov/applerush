package ru.mitrakov.self.rush;

import java.util.Arrays;

import ru.mitrakov.self.rush.model.Model;
import ru.mitrakov.self.rush.net.Network;
import ru.mitrakov.self.rush.utils.collections.IIntArray;

import static ru.mitrakov.self.rush.utils.Utils.getBytes;
import static ru.mitrakov.self.rush.net.Network.BUF_SIZ_SEND;

/**
 * Message Sender is used for sending messages from the Model (Loose Coupling Principle)
 * This class is intended to have a single instance
 * @author mitrakov
 */
class MsgSender implements Model.ISender {
    private final Network network;
    private final Thread.UncaughtExceptionHandler errorHandler;
    private final IIntArray sendBuf = new GcResistantIntArray(BUF_SIZ_SEND);

    /**
     * Creates a new instance of Message Sender
     * @param network - network (NON-NULL)
     * @param errorHandler - error handler to process IO exceptions (NON-NULL)
     */
    MsgSender(Network network, Thread.UncaughtExceptionHandler errorHandler) {
        assert network != null && errorHandler != null;
        this.network = network;
        this.errorHandler = errorHandler;
    }

    @Override
    public void send(Model.Cmd cmd) {
        send(Arrays.binarySearch(Model.cmdValues, cmd)); // don't use "cmd.ordinal()" (GC pressure)
    }

    @Override
    public void send(int cmd) {
        try {
            network.send(sendBuf.clear().add(cmd));
        } catch (Exception e) {
            errorHandler.uncaughtException(Thread.currentThread(), e);
        }
    }

    @Override
    public void send(Model.Cmd cmd, int... arg) {
        send(Arrays.binarySearch(Model.cmdValues, cmd), arg);
    }

    @Override
    public void send(int cmd, int... arg) {
        try {
            sendBuf.clear().add(cmd);
            for (int i : arg) {
                sendBuf.add(i);
            }
            network.send(sendBuf);
        } catch (Exception e) {
            errorHandler.uncaughtException(Thread.currentThread(), e);
        }
    }

    @Override
    public void send(Model.Cmd cmd, String arg) {
        send(Arrays.binarySearch(Model.cmdValues, cmd), arg);
    }

    @Override
    public void send(int cmd, String arg) {
        try {
            network.send(sendBuf.fromByteArray(getBytes(arg), arg.length()).prepend(cmd));
        } catch (Exception e) {
            errorHandler.uncaughtException(Thread.currentThread(), e);
        }
    }

    @Override
    public void reset() {
        network.reset(0, 0);
    }
}

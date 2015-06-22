package network;

import util.SBApplication;

import java.util.Date;
import java.util.UUID;
import java.util.Vector;

/**
 * An interface for both the server and the client socket managers.
 * Created by milan on 28.3.15.
 */
public abstract class SBSocketManager {

    public static final int PING_SENDER_INTERVAL = 3*1000; // in milliseconds
    public static final int PING_MESSAGE_TIMEOUT = 8*1000; // in milliseconds
    protected int port = 0;
    protected SBApplication parent;
    SBProtocolManager protocolManager;
    private Date lastPingAnswered;

    /**
     * Create a new socket manager for application parent.
     * @param parent The application to create this socket manager for.
     * @param protocolManager The protocol parameter for this socket manager.
     */
    public SBSocketManager(SBApplication parent, SBProtocolManager protocolManager) {
        this.parent = parent;
        this.protocolManager = protocolManager;
        this.lastPingAnswered = new Date(System.currentTimeMillis());
    }

    /**
     * Get the parent application of this socket manager.
     * @return The parent application of this socket manager.
     */
    public SBApplication getParent() {
        return parent;
    }

    /**
     * Get the procotol manager of this socket manager.
     * @return The protocol manager of this socket manager.
     */
    public SBProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public SBSocketManager getThis() {
        return this;
    }

    public abstract void removeSocket(SBSocket socket);

    public abstract SBSocket getSocket(UUID UID);

    /**
     * Send a message to a connected socket. Server searches for UID in his connected Sockets and Client always sends the message to the connected server if it is connected.
     * @param UID The UID of the socket to send the message to. (The client socket manager ignores this)
     * @param message The protocol message to send.
     */
    public abstract void sendMessage(UUID UID, SBProtocolMessage message);

    public abstract Vector<SBSocket> getSockets();

    public abstract void startConnectionListener();

    public Date getLastPingAnswered() {
        return lastPingAnswered;
    }

    public void setLastPingAnswered(Date lastPingAnswered) {
        this.lastPingAnswered = lastPingAnswered;
    }

    /**
     * Get the port to which the socket is currently connected to.
     * @return The port. 0 if the socket is not connected.
     */
    public int getPort() {
        return port;
    }
}

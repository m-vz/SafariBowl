package client.logic;

import client.Client;
import network.SBNetworkException;
import network.SBProtocolMessage;
import network.SBSocket;
import network.SBSocketManager;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

/**
 * A socket manager that will handle the sending and recieving of messages for a client.
 * Created by milan on 25.3.15.
 */
public class ClientSocketManager extends SBSocketManager {

    private InetAddress address;
    private SBSocket socket;

    /**
     * Create a new client socket manager for client.
     * @param client The client to create this socket manager for.
     * @param protocolManager The protocol manager for this client.
     */
    public ClientSocketManager(Client client, ClientProtocolManager protocolManager) {
        super(client, protocolManager);
    }

    /**
     * Start the client and connect to address on port.
     * @param address The address to connect to.
     * @param port The port to connect to.
     */
    public void startClient(InetAddress address, int port) {
        this.port = port;
        this.address = address;
        // create socket creator thread
        SBSocketConnector connector = new SBSocketConnector(address, port, getParent().UID, this);
        connector.start();
        getParent().isConnecting();
    }

    /**
     * The thread that connects to a server in the background.
     */
    private class SBSocketConnector extends Thread {
        private InetAddress address;
        private int port;
        private UUID UID;
        private SBSocketManager manager;

        SBSocketConnector(InetAddress address, int port, UUID UID, SBSocketManager manager) {
            this.address = address;
            this.port = port;
            this.UID = UID;
            this.manager = manager;
        }

        public void run() {
            try {
                SBSocket socket = new SBSocket(address, port, UID, manager);
                connectorFinished(socket);
            } catch (SBNetworkException e) {
                connectorFinished(null);
            }
        }
    }

    /**
     * This method is run when the connector finished connecting to a server.
     * @param socket The socket that is connected to the server.
     */
    void connectorFinished(SBSocket socket) {
        if(socket != null) {
            this.socket = socket;
            getParent().hasConnected(address, port, true);
        } else {
            getParent().hasConnected(address, port, false);
        }
    }

    /**
     * Stop the client.
     */
    public void stopClient() {
        removeSocket(socket);
        address = null;
        port = 0;
    }

    /**
     * Get the address to which the client is currently connected to.
     * @return The address. null if the client is not connected.
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Close the socket. On the client the socket passed is ignored, because there exists only one socket.
     * @param socket On the client this is ignored, because there exists only one socket.
     */
    public void removeSocket(SBSocket socket) {
        try {
            getParent().log(Level.SEVERE, "Closing socket.");
            socket.close();
            getParent().lostConnection();
        } catch (IOException e) {
            getParent().log(Level.SEVERE, "Error while closing socket " + socket.getUID().toString().substring(0, 8) + ". Leaving open.");
        }
        this.socket = null;
    }

    public Vector<SBSocket> getSockets() {
        Vector<SBSocket> sockets = new Vector<SBSocket>();
        sockets.add(socket);
        return sockets;
    }

    /**
     * Get the socket if its UID equals the given UID.
     * @param UID The UID to check the UID of the socket against.
     * @return The socket if its UUD equals the given UID or null if not.
     */
    public SBSocket getSocket(UUID UID) {
        if(socket != null) if(socket.getUID().equals(UID)) return socket;
        return null;
    }

    public SBSocket getSocket() {
        return socket;
    }

    /**
     * Send a message to the socket if it is connected.
     * @param UID The client socket manager ignores this, because there is only one socket to send messages to.
     * @param message The protocol message to send.
     */
    public void sendMessage(UUID UID, SBProtocolMessage message) {
        if(socket != null) socket.sendMessage(message);
    }

    /**
     * Send a message to the socket if it is connected.
     * @param message The protocol message to send.
     */
    public void sendMessage(SBProtocolMessage message) {
        if(socket != null) socket.sendMessage(message);
    }

    /**
     * Get whether the socket is connected to a server.
     * @return Whether the socket is connected to a server.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    /**
     * Start a connection listener to wait for ping timeouts.
     */
    public void startConnectionListener() {
        // start connection listener
        (new Thread(new Runnable() {
            @Override
            public void run() {
                setLastPingAnswered(new Date(System.currentTimeMillis()));
                // check if any ping has been received in the last PING_MESSAGE_TIMEOUT milliseconds
                while(socket != null) {
                    if(getLastPingAnswered().before(new Date(System.currentTimeMillis()-PING_MESSAGE_TIMEOUT))) {
                        getParent().log(Level.SEVERE, "Timeout while waiting for a ping. Closing socket.");
                        removeSocket(socket);
                    }
                    try {
                        Thread.sleep(PING_SENDER_INTERVAL);
                    } catch (InterruptedException e) {
                        getParent().log(Level.WARNING, "Interrupted while waiting until checking last received ping. Checking now instead.");
                    }
                }
            }
        })).start();
    }
}

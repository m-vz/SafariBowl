package server.logic;

import network.SBNetworkException;
import network.SBProtocolMessage;
import network.SBSocket;
import network.SBSocketManager;
import server.Server;
import util.SBApplication;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

/**
 * A protocol manager that will handle the sending and recieving of messages for a server.
 * Created by milan on 25.3.15.
 */
public class ServerSocketManager extends SBSocketManager {

    private ServerSocket serverSocket;
    private Vector<SBSocket> sockets;
    private int socketsCount = -1;
    private ConnectionListener connenctionListener = new ConnectionListener();

    /**
     * Create a new server socket manager for server.
     * @param server The server to create this socket manager for.
     * @param protocolManager The protocol manager for this server socket manager.
     */
    public ServerSocketManager(SBApplication server, ServerProtocolManager protocolManager) {
        super(server, protocolManager);
    }

    /**
     * Start the server on port.
     * @param port The port for the server to listen to.
     * @throws SBNetworkException If there occurred an exception while creating the socket.
     */
    public void startServer(int port) throws SBNetworkException {
        this.port = port;
        this.sockets = new Vector<SBSocket>();
        // create sockets
        try {
            serverSocket = new ServerSocket(port);
            // create and start socket listener to accept incoming connections
            (new Thread(new Runnable() {
                @Override
                public void run() {
                    while(serverSocket != null) {
                        Socket socket = null;
                        // wait for connection
                        try {
                            socket = serverSocket.accept();
                        } catch (SocketException e) {
                            if(!e.getMessage().equals("Socket closed"))
                                getParent().log(Level.SEVERE, "Exception while listening for connections. " + e.toString());
                        } catch (IOException e) {
                            getParent().log(Level.SEVERE, "Exception while listening for connections. " + e.toString());
                        }
                        if(socket != null) {
                            try {
                                // create SBSocket from socket and create listener and writer for it
                                SBSocket sbsocket = new SBSocket(socket, UUID.randomUUID(), getThis());
                                sbsocket.createListenerAndWriter();
                                sockets.add(sbsocket);
                            } catch (SBNetworkException e) {
                                // close socket if creating listener or writer for sbsocket fails
                                try {
                                    getParent().log(Level.SEVERE, "Error while creating SBSocket from socket. Closing socket.");
                                    socket.close();
                                } catch (IOException e1) { getParent().log(Level.SEVERE, "Error while closing socket. Ignoring."); }
                            }
                        }
                    }
                }
            })).start();
            startConnectionListener();
            getParent().log(Level.INFO, "Successfully started server on port " + port + ".");
        } catch (IOException e) {
            getParent().log(Level.SEVERE, "Exception while creating socket on port "+port+". "+e.toString());
            throw new SBNetworkException();
        }
    }

    /**
     * Stop the server.
     * @throws SBNetworkException If there was an exception while stopping the server.
     */
    public void stopServer() throws SBNetworkException {
        try {
            if(sockets != null)
                if(sockets.size() > 0)
                    for(SBSocket socket: sockets)
                        socket.close();
            sockets = null;
            serverSocket.close();
            serverSocket = null;
            setSocketsCount(-1);
            port = 0;
        } catch (IOException e) {
            getParent().log(Level.SEVERE, "Exception while stopping server. "+e.toString());
            throw new SBNetworkException();
        }
    }

    /**
     * Restart the server on previously specified port.
     * @throws SBNetworkException If there was an exception while starting or stopping the server.
     */
    public void restartServer() throws SBNetworkException {
        stopServer();
        startServer(port);
    }

    /**
     * Restart the server on a new port.
     * @param port The port for the server to listen to.
     * @throws SBNetworkException If there was an exception while starting the server.
     */
    public void restartServer(int port) throws SBNetworkException {
        stopServer();
        startServer(port);
    }

    /**
     * Get the port on which the server is currently listening to.
     * @return The port. 0 if the server is not running.
     */
    public int getPort() {
        return port;
    }

    /**
     * Close a socket and remove it from the sockets.
     * @param socket The socket to close and remove.
     */
    public void removeSocket(SBSocket socket) {
        try {
            parent.logOutUserWithUID(socket.getUID());
            ((Server) parent).broadcastUpdatedUsersList();
            socket.close();
        } catch (IOException e) {
            getParent().log(Level.SEVERE, "Error while closing socket " + socket.getUID().toString().substring(0, 8) + ". Leaving open.");
        }
        if(sockets != null) sockets.remove(socket);
    }

    public Vector<SBSocket> getSockets() {
        return sockets;
    }

    /**
     * Get a socket by its UID.
     * @param UID The UID of the socket to get.
     * @return The socket if there exists a socket with the given UID or else null.
     */
    public SBSocket getSocket(UUID UID) {
        if(sockets != null) for(SBSocket socket: sockets) if(socket.getUID().equals(UID)) return socket;
        return null;
    }

    public void sendMessage(UUID UID, SBProtocolMessage message) {
        if(sockets != null) for(SBSocket socket: sockets) if(socket.getUID().equals(UID)) socket.sendMessage(message);
    }

    public void sendBroadcastMessage(SBProtocolMessage message) {
        if(sockets != null)
            for(SBSocket socket: sockets)
                socket.sendMessage(message);
    }

    public int getSocketsCount() {
        return socketsCount;
    }

    public void setSocketsCount(int socketsCount) {
        this.socketsCount = socketsCount;
    }


    /**
     * Start a connection listener that sends pings and handles ping timeouts.
     */
    public void startConnectionListener() {
        // create and start connection listener to ping connected clients
        connenctionListener.stopListening();
        connenctionListener = new ConnectionListener();
        connenctionListener.start();
    }

    private class ConnectionListener extends Thread {
        private boolean listening = true;

        @Override
        public void run() {
            // set up map with sent ping messages MIDs and corresponding sockets
            Hashtable<SBProtocolMessage, SBSocket> sentPings = new Hashtable<SBProtocolMessage, SBSocket>();
            // send ping to every socket in regular interval
            while(sockets != null && serverSocket != null && listening) {
                if(sockets.size() != getSocketsCount()) {
                    getParent().log(Level.INFO, "Sockets connected: "+sockets.size());
                    setSocketsCount(sockets.size());
                }
                // send new pings
                for(SBSocket socket: sockets) {
                    SBProtocolMessage pingMessage = SBProtocolMessage.createPingMessage(parent.UID);
                    getParent().log(Level.FINEST, "Sent ping to socket " + socket.getUID().toString().substring(0, 8));
                    sentPings.put(pingMessage, socket);
                    socket.sendMessage(pingMessage);
                }
                // process new ping answers
                while(getProtocolManager().getPingAnswersToProcess().size() > 0) {
                    // prepare answer and ping to remove
                    SBProtocolMessage pong = getProtocolManager().getPingAnswersToProcess().poll();
                    SBProtocolMessage pingToRemove = null; // ping messages that have been answered and will be removed
                    for(SBProtocolMessage ping: sentPings.keySet()) { // check all sentPings
                        if(ping.getMID().equals(pong.getMID())) pingToRemove = ping; // remove ping from sentPings if ping MID equals pong MID
                    }
                    if(pingToRemove != null) sentPings.remove(pingToRemove); // remove ping to remove
                }
                // check unanswered pings for timeouts
                Vector<SBSocket> socketsToRemove = new Vector<SBSocket>(); // vector with sockets for which all sent messages should be removed from sent pings because the following loop cannot do so while iterating
                for (SBProtocolMessage ping : sentPings.keySet()) {
                    if (!ping.getSentDate().equals(new Date(0))) {
                        if (ping.getSentDate().before((new Date(System.currentTimeMillis() - PING_MESSAGE_TIMEOUT)))) {
                            getParent().log(Level.SEVERE, "Timeout while waiting for a ping answer to ping " + ping.getMID().toString().substring(0, 8)
                                    + ".\nClosing socket " + sentPings.get(ping).getUID().toString().substring(0, 8)+".");
                            // remove socket
                            socketsToRemove.add(sentPings.get(ping));
                            removeSocket(sentPings.get(ping));
                        }
                    }
                }
                // remove all messages that have been sent to the socket of each message queued for removal in messagesToRemove
                Vector<SBProtocolMessage> messagesToRemove = new Vector<SBProtocolMessage>();
                for(SBSocket socket: socketsToRemove) // for every socket in removal list
                    for(SBProtocolMessage ping: sentPings.keySet()) // for every message in unanswered pings
                        if(sentPings.get(ping).equals(socket)) messagesToRemove.add(ping); // queue message for removal if it was sent to socket
                for(SBProtocolMessage ping: messagesToRemove) sentPings.remove(ping); // remove all messages queued for removal
                // wait for the next iteration
                try {
                    Thread.sleep(PING_SENDER_INTERVAL);
                } catch (InterruptedException e) {
                    getParent().log(Level.WARNING, "Interrupted while waiting until sending next ping. Sending next ping now instead.");
                }
            }
            getParent().log(Level.SEVERE, "Connection listener stopped.");
        }

        public void stopListening() {
            this.listening = false;
        }
    }
}

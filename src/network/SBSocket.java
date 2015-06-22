package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

/**
 * A subclass of socket with an attached UID, listener and reader. And methods to send SBProtocolMessages.
 * Created by milan on 23.3.15.
 */
public class SBSocket {

    private UUID UID;
    private BufferedReader listener;
    private PrintWriter writer;
    private SBSocketManager socketManager;
    private Socket socket;
    private SBProtocolReceiver receiver;

    /**
     * Create new SBSocket with an existing socket and a UUID.
     * @param socket The socket to create this socket with.
     * @param UID The UID of this SBSocket.
     * @param socketManager The socket manager of this SBSocket.
     * @throws SBNetworkException If there was an exception while creating the socket.
     */
    public SBSocket(Socket socket, UUID UID, SBSocketManager socketManager) throws SBNetworkException {
        this.socket = socket;
        this.UID = UID;
        this.socketManager = socketManager;
        createListenerAndWriter();
        createThreads();
    }

    /**
     * Create new SBSocket with a UUID, a listener and a writer.
     * @param address The address to connect to.
     * @param port The port to connect to.
     * @param UID The UID of this SBSocket.
     * @param socketManager The socket manager of this SBSocket.
     * @throws SBNetworkException If there was an exception while creating the socket.
     */
    public SBSocket(InetAddress address, int port, UUID UID, SBSocketManager socketManager) throws SBNetworkException {
        try {
            this.socket = new Socket(address, port);
        } catch (IOException e) {
            socketManager.getParent().log(Level.SEVERE, "Exception while creating socket for SBSocket. " + e.toString());
            throw new SBNetworkException();
        }
        this.UID = UID;
        this.socketManager = socketManager;
        createListenerAndWriter();
        createThreads();
    }

    /**
     * Create new SBSocket with an empty UUID, a listener and a writer.
     * @param address The address to connect to.
     * @param port The port to connect to.
     *  @param socketManager The socket manager of this SBSocket.
     * @throws SBNetworkException If there was an exception while creating the socket.
     */
    public SBSocket(InetAddress address, int port, SBSocketManager socketManager) throws SBNetworkException {
        try {
            this.socket = new Socket(address, port);
        } catch (IOException e) {
            getSocketManager().getParent().log(Level.SEVERE, "Exception while creating socket for SBSocket. " + e.toString());
            throw new SBNetworkException();
        }
        this.UID = new UUID(0,0);
        this.socketManager = socketManager;
        createListenerAndWriter();
        createThreads();
    }

    /**
     * Create the receiver thread for this socket.
     */
    private void createThreads() {
        // prepare thread for the receiver
        receiver = new SBProtocolReceiver(getSocketManager().getProtocolManager());
        // start receiver thread
        (new Thread(receiver)).start();
    }

    /**
     * Create the listener and the writer for this socket.
     * @throws SBNetworkException If the creation failed.
     */
    public void createListenerAndWriter() throws SBNetworkException {
        try {
            listener = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            getSocketManager().getParent().log(Level.FINE, "Successfully created listener.");
        } catch (IOException e) {
            getSocketManager().getParent().log(Level.SEVERE, "Exception while trying to create listener. " + e.toString());
            throw new SBNetworkException();
        }
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            getSocketManager().getParent().log(Level.FINE, "Successfully created writer.");
        } catch (IOException e) {
            getSocketManager().getParent().log(Level.SEVERE, "Exception while trying to create writer. " + e.toString());
            throw new SBNetworkException();
        }
    }

    /**
     * Send a message to the connected socket.
     * @param message The message to send.
     */
    public void sendMessage(SBProtocolMessage message) {
        if (message != null) {
        	// add to unanswered messages only if the message is no answer to another message and no ping
            if (message.getModule() != SBProtocolCommand.SBProtocolModule.SUC
                    && message.getModule() != SBProtocolCommand.SBProtocolModule.FAI
                    && message.getModule() != SBProtocolCommand.SBProtocolModule.PNG)
                socketManager.protocolManager.addUnansweredMessage(message);
            // log
            if(message.getModule() != SBProtocolCommand.SBProtocolModule.PNG) // don't log ping messages
                getSocketManager().getParent().log(Level.FINE, "Sending message " + message.toStringShortenUUID());
            // set sent date
            message.setSentDate(new Date(System.currentTimeMillis()));
            // sign message with this socket for returning answers
            message.setSocket(getThis());
            // send it
            writer.println(message.toString());
            if(writer.checkError()) getSocketManager().getParent().log(Level.FINE, "Exception while sending message. Socket probably disconnected.");
        } else {
            getSocketManager().getParent().log(Level.SEVERE, "Tried to send NULL message. Obviously not sending this.");
        }
    }

    /**
     * Read a line from the input stream.
     * @return The line read from the listener. Null if an error occurred.
     * @throws SBNetworkException if the socket is unable to read the next line.
     */
    public String readLine() throws SBNetworkException {
        try {
            if(listener == null) {
                getSocketManager().getParent().log(Level.SEVERE, "Exception while trying to read line. Listener is null.");
                throw new SBNetworkException();
            }
            return listener.readLine();
        } catch (IOException e) {
            getSocketManager().getParent().log(Level.WARNING, "Unable to read next line.");
            throw new SBNetworkException();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    /**
     * The thread processing received messages.
     */
    private class SBProtocolReceiver extends Thread {
        SBProtocolManager manager;

        SBProtocolReceiver(SBProtocolManager manager) {
            this.manager = manager;
        }

        public void run() {
            while (true) {
                if(socket != null) {
                    String readLine;
                    try {
                        readLine = readLine();
                    } catch (SBNetworkException e) {
                        readLine = null;
                        socket = null;
                    }
                    SBProtocolMessage receivedMessage = SBProtocolMessage.fromString(readLine);
                    if (receivedMessage != null) {
                        if(receivedMessage.getModule() != SBProtocolCommand.SBProtocolModule.PNG) // don't log ping messages
                            getSocketManager().getParent().log(Level.FINE, "Received message " + receivedMessage.toStringShortenUUID());
                        // sign message with this socket for returning answers
                        receivedMessage.setSocket(getThis());
                        if (receivedMessage.getCommand() == SBProtocolCommand.PIING) {
                            // return a success message (will only ever be executed by a client)
                            getSocketManager().getParent().log(Level.FINEST, "Answered ping message " + receivedMessage.getMID().toString().substring(0, 8));
                            sendMessage(SBProtocolMessage.createPingAnswer(getSocketManager().getParent().UID, receivedMessage.getMID()));
                            getSocketManager().setLastPingAnswered(new Date(System.currentTimeMillis()));
                        } else if (receivedMessage.getCommand() == SBProtocolCommand.POONG) {
                            // put in pingAnswersToProcess (will only ever be executed by a server)
                            getSocketManager().getProtocolManager().putInQueue(getSocketManager().getProtocolManager().getPingAnswersToProcess(), receivedMessage);
                            getSocketManager().getParent().log(Level.FINEST, "Received ping answer " + receivedMessage.getMID().toString().substring(0, 8));
                        } else if(receivedMessage.getModule() != SBProtocolCommand.SBProtocolModule.SUC && receivedMessage.getModule() != SBProtocolCommand.SBProtocolModule.FAI) {
                            // forward all messages that are no answers to the specific (client/server) protocol manager
                            getSocketManager().getProtocolManager().putInQueue(getSocketManager().getProtocolManager().getMessagesToProcess(), receivedMessage);
                            getSocketManager().getParent().log(Level.FINE, "Put the message " + receivedMessage.toStringShortenUUID() + " in handle queue.");
                        } else {
                            // remove the original message from the unanswered messages if this is an answer to that original message
                            getSocketManager().getProtocolManager().putInQueue(getSocketManager().getProtocolManager().getAnswersToProcess(), receivedMessage);
                            getSocketManager().getParent().log(Level.FINE, "Put the answer " + receivedMessage.toStringShortenUUID() + " in handle queue");
                        }
                    }
                    try { // sleep for a bit after each received message (prevents null from flooding the logs)
                        Thread.sleep(10);
                    } catch (InterruptedException e) { e.printStackTrace(); }
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Close this socket.
     * @throws IOException If there was an error closing this socket.
     */
    public void close() throws IOException {
        if(socket != null) socket.close();
        socket = null;
    }

    // getters and setters

    public UUID getUID() {
        return UID;
    }

    public void setUID(UUID UID) {
        this.UID = UID;
    }

    public BufferedReader getListener() {
        return listener;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public SBSocketManager getSocketManager() {
        return socketManager;
    }

    private SBSocket getThis() {
        return this;
    }
}
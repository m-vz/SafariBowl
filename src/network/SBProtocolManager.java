package network;

import util.SBApplication;

import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * The abstract protocol manager to handle protocols on a client or server.
 * Created by milan on 23.3.15.
 */
public abstract class SBProtocolManager {

    private volatile Vector<SBProtocolMessage> unansweredMessages = new Vector<SBProtocolMessage>(); // table with sent messages
    private volatile LinkedBlockingQueue<SBProtocolMessage> processedMessages = new LinkedBlockingQueue<SBProtocolMessage>(); // a list containing all messages that have already been processed.
    private volatile LinkedBlockingQueue<SBProtocolMessage> messagesToProcess = new LinkedBlockingQueue<SBProtocolMessage>(); // a list containing all messages that need to be processed by the server/client protocol manager thread.
    private volatile LinkedBlockingQueue<SBProtocolMessage> answersToProcess = new LinkedBlockingQueue<SBProtocolMessage>(); // a list containing all answers that need to be processed by the server/client protocol manager thread.
    private volatile LinkedBlockingQueue<SBProtocolMessage> pingAnswersToProcess = new LinkedBlockingQueue<SBProtocolMessage>(); // a list containing all ping answers that need to be processed by the socket manager.
    SBApplication parent;

    /**
     * Create a new protocol manager.
     * @param parent The parent application of the protocol manager.
     */
    public SBProtocolManager(SBApplication parent) {
        this.parent = parent;
        // create and start new thread to start processing messagesToProcess and answersToProcess queues
        (new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    getParent().processAnswers();
                    getParent().processMessages();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) { break; }
                }
            }
        })).start();
    }

    /**
     * Try to add a message to a queue until it worked (wait 100ms after each retry).
     * @param queue The queue to put the message in.
     * @param message The message to put into the queue.
     */
    void putInQueue(LinkedBlockingQueue<SBProtocolMessage> queue, SBProtocolMessage message) {
        boolean putInQueue = false;
        while(!putInQueue) {
            try {
                queue.put(message);
                putInQueue = true;
            } catch (InterruptedException e) {
                getParent().log(Level.WARNING, "Interrupted while adding the message to the queue. Retrying in 100ms...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    getParent().log(Level.SEVERE, "Interrupted while waiting to retry adding the message to the queue. Continuing anyway (because I'm badass).");
                }
            }
        }
    }

    // getters and setters

    public SBApplication getParent() {
        return parent;
    }

    public Vector<SBProtocolMessage> getUnansweredMessages() {
        return unansweredMessages;
    }

    public void removeUnansweredMessage(SBProtocolMessage message) {
        if(message != null) getUnansweredMessages().remove(message);
    }

    public void addUnansweredMessage(SBProtocolMessage message) {
        if(message != null) getUnansweredMessages().add(message);
    }

    public LinkedBlockingQueue<SBProtocolMessage> getProcessedMessages() {
        return processedMessages;
    }

    public void addProcessedMessage(SBProtocolMessage message) {
        if(message != null) getProcessedMessages().add(message);
    }

    public LinkedBlockingQueue<SBProtocolMessage> getMessagesToProcess() {
        return messagesToProcess;
    }

    public LinkedBlockingQueue<SBProtocolMessage> getAnswersToProcess() {
        return answersToProcess;
    }

    public LinkedBlockingQueue<SBProtocolMessage> getPingAnswersToProcess() {
        return pingAnswersToProcess;
    }

    public SBProtocolMessage getNextMessageToProcessAndStoreIt() {
        SBProtocolMessage message = getMessagesToProcess().poll();
        addProcessedMessage(message);
        return message;
    }
}

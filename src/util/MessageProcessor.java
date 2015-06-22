package util;

import network.SBProtocolMessage;

/**
 * An interface for Server and Client message processors.
 * Created by milan on 1.4.15.
 */
public interface MessageProcessor {

    void processMessage(SBProtocolMessage message);
    void processAnswer(SBProtocolMessage answer);

    void returnSuccessMessage(SBProtocolMessage returnTo, String... parameters);
    void returnFailureMessage(SBProtocolMessage returnTo, String... parameters);

}

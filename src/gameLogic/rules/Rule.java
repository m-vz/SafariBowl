package gameLogic.rules;

import java.util.UUID;

import server.logic.User;
import network.SBProtocolCommand;
import network.SBProtocolMessage;
import network.SBProtocolParameterArray;
import gameLogic.*;

/**
 * An abstract game rule.
 */
public abstract class Rule {
	protected Player actor;
	
	public boolean geTest(int mod) {
		int die = actor.getMatch().d6.throwDie();
		return die == 6 || die != 1 && mod + die + actor.invokeGetGe() >= 7;
	}
	
	public Rule(Player actor) {
		setActor(actor);
	}
	public void returnFailureMessage(SBProtocolMessage message, String... parameters){
		message.returnFailureMessage(getMatch().getParent().UID, new SBProtocolParameterArray(parameters));
	}
	public void returnSuccessMessage(SBProtocolMessage message, String... parameters){
		message.returnSuccessMessage(getMatch().getParent().UID, new SBProtocolParameterArray(parameters));
	}
	public void sendMessage(SBProtocolMessage message, SBProtocolCommand command, String... parameters){
		sendMessage(message.getUID(), command, parameters);
	}
	public void sendMessage(UUID UID, SBProtocolCommand command, String... parameters){
		if(UID.equals(getMatch().getUser(0).getUID())){
			sendMessage(getMatch().getUser(0), command, parameters);
		}else if(UID.equals(getMatch().getUser(1).getUID())){
			sendMessage(getMatch().getUser(1), command, parameters);
		}
	}
	public void sendMessage(User destinationUser, SBProtocolCommand command, SBProtocolParameterArray parameters){
		getMatch().sendMessage(destinationUser, command, parameters);
	}
	public void sendMessage(User destinationUser, SBProtocolCommand command, String... parameters){
		actor.getMatch().sendMessage(destinationUser, command, parameters);
	}
	public void sendMessageShowMe(String a, String b){
		sendMessage(getMatch().getUser(0), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_SHOW_ME, a, b);
		sendMessage(getMatch().getUser(1), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_SHOW_ME, a, b);
	}
	public void sendMessageShowMe(User user, String a, String b){
		sendMessage(user, SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_SHOW_ME, a, b);
	}
	
	public void checkForMovingPlayer(Player actor){
		if(actor.getTeam().getMovingPlayer() != null){
			if(actor.getTeam().getMovingPlayer() != actor){
				actor.getTeam().getMovingPlayer().invokeSetRemainingBe(0);
				actor.getTeam().setMovingPlayer(actor);
			}
		}else{
			actor.getTeam().setMovingPlayer(actor);
		}
	}

	// GETTERS & SETTERS

	public void setActor(Player actor){this.actor = actor;}
	public Player getActor(){return actor;}
	public GameController getMatch(){return actor.getMatch();}
}

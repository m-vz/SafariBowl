package gameLogic.rules;

import javax.vecmath.Vector2d;

import server.logic.ServerMatch;
import network.SBProtocolCommand;
import network.SBProtocolMessage;
import gameLogic.Player;
import gameLogic.PlayerCondition;
import gameLogic.Weather;

public class RuleCatch extends Rule{

	public RuleCatch(Player actor) {
		super(actor);
	}
	
	public void apply(boolean successfulThrow){
		int actingUserIndex;
		if(actor.getTeam() == actor.getMatch().getTeam(0)){
			actingUserIndex = 0;
		}else if(actor.getTeam() == actor.getMatch().getTeam(1)){
			actingUserIndex = 1;
		}else{
			return;
		}
		if(actor.invokeGetPlayerCondition() == PlayerCondition.FINE){
			catchBall(successfulThrow, actingUserIndex);
		}else{
			scatterBallAround(actingUserIndex);
		}
	}
	
	protected void catchBall(boolean successfulThrow, int actingUserIndex){
		int mod = -(actor.getMatch().getOpposingTeam(actor.getTeam()).getTacklezones(actor.getPos()));
		if(successfulThrow){
			mod++;
		}
		if(getMatch().getWeather() == Weather.POURING_RAIN){
			mod--;
		}
		if(geTest(mod)){
			ballCatched(actingUserIndex);
		}else{
			sendMessageShowMe(actor.toString(), "Dammit, I didn't catch the ball!");
			scatterBallAround(actingUserIndex);
		}
	}
	
	protected void scatterBallAround(int actingUserIndex){
		Vector2d newBallPos = new Vector2d(actor.getMatch().getPitch().getBallPos());
		newBallPos.add(actor.getMatch().scatter());
		actor.getMatch().getPitch().setBallPos(newBallPos);
		sendMessage(actor.getMatch().getUser(actingUserIndex), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_BALL_NOT_CATCHED);
	}
	
	protected void ballCatched(int actingUserIndex){
		actor.invokeSetIsHoldingBall(true);
		sendMessageShowMe(actor.toString(), "I catched the ball!");
		sendMessage(actor.getMatch().getUser(actingUserIndex), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_BALL_CATCHED);
		actor.getMatch().sendPlayer(actor);
	}
	
	public boolean giveBall(SBProtocolMessage message){
		actor.getMatch().getPitch().adjustBallPos(actor.getPos());
		actor.invokeSetIsHoldingBall(true);
		sendMessage(message, SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_YOUR_TURN);
		actor.getMatch().setGamePhase(3);
		return true;
	}
}

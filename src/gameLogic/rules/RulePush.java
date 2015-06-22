package gameLogic.rules;

import java.util.UUID;
import java.util.Vector;

import gameLogic.*;
import network.SBProtocolCommand;
import network.SBProtocolMessage;
import network.SBProtocolParameter;
import network.SBProtocolParameterArray;

import javax.vecmath.Vector2d;

import server.logic.ServerMatch;
import server.logic.User;
/**
 * The game rule for pushing stuff.
 */
public class RulePush extends Rule {

	public RulePush(Player actor) {
		super(actor);
	}
	/**
	 * the player is asked weather he wants to follow the pushed player or not
	 * @param message SBProtocolMessage
	 * @param defenderField The field you would go to in case you want to
	 */
	public void backUp(Vector2d defenderField, SBProtocolMessage message, Player playerBackingUp){
		((RuleBlock)actor.getRule(1)).clearHighlightFields();
		((ServerMatch) getMatch()).setCurrentActorWaitingForAnswer(playerBackingUp);
		((ServerMatch) getMatch()).setCurrentDefenderFieldWaitingForAnswer(defenderField);
		getMatch().sendMessage(playerBackingUp.getTeam().getCoach(), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_FOLLOW, "no", "yes");
		playerBackingUp.getMatch().setGamePhase(5);
	}
	/**
	 * waits until the user decided if he should follow and then executes his decision
	 * @param defenderField the destination field on which you can follow
	 * @param message SBProtocolMessage
	 * @param answer SBProtocolMessage
	 */
	public boolean follow(Vector2d defenderField, SBProtocolMessage message, SBProtocolMessage answer){
		actor.getMatch().setGamePhase(3);
		int decision;
		try{
			decision = Integer.parseInt(answer.getParameterContent(2));
		} catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
			return false;
		}
		
		if(decision == 1){
			actor.invokeSetPosition(defenderField);
			returnSuccessMessage(message, SBProtocolMessage.WORKD_FOLLOWED);
		}
		else if(decision ==0){
			returnSuccessMessage(message, SBProtocolMessage.WORKD_NOTFOLLOWED);
		}
		else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_CANT_BACKUP_FOR_SOME_ODD_REASON);
			return false;
		}
		return true;
	}
	/**
	 * main method for pushing
	 * @param message SBProtocolMessage
	 * @param defender the player who is going to be pushed
	 */
	public void apply(SBProtocolMessage message, Player defender, PitchField defenderField, UUID pushChooser, Player playerBackingUp, Vector2d posToBackup) {
		if(defender.invokeGetPlayerCondition() == PlayerCondition.DEAD
				|| defender.invokeGetPlayerCondition() == PlayerCondition.INJURED
				|| defender.invokeGetPlayerCondition() == PlayerCondition.KO
				|| defender.getPosition() == Pitch.THE_VOID){
			((RulePush)playerBackingUp.getRule(2)).backUp(posToBackup, message, playerBackingUp);
		}else if(pushPossible(actor.getPosition(), defenderField)){
			((ServerMatch) getMatch()).setCurrentActorWaitingForAnswer(actor);
			((ServerMatch) getMatch()).setCurrentDefenderWaitingForAnswer(defender);
			((ServerMatch) getMatch()).setCurrentPusherWaitingForAnswer(playerBackingUp);
			((ServerMatch) getMatch()).setCurrentBackUpPosWaitingForAnswer(posToBackup);
			SBProtocolParameterArray parameters = new SBProtocolParameterArray();
			parameters.addParameter(new SBProtocolParameter(SBProtocolMessage.EVENT_API_FIELD));
			parameters.addParameter(new SBProtocolParameter("$push"));
			Vector<Vector2d> pushPossibilities = pushDirections(actor.getPosition(), defenderField);
			for(Vector2d direction: pushPossibilities){
				Vector2d destinationField = new Vector2d(direction);
				destinationField.add(defenderField.getPos());
				parameters.addParameter(new SBProtocolParameter((int)destinationField.x + ""));
				parameters.addParameter(new SBProtocolParameter((int)destinationField.y + ""));
			}
			SBProtocolMessage messageToSend = new SBProtocolMessage(actor.getMatch().getParent().getUID(), SBProtocolCommand.EVENT, parameters);
			getMatch().sendMessage(pushChooser, messageToSend);
			actor.getMatch().setGamePhase(5);
		}else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_PUSHING_NOT_POSSIBLE);
		}
	}

	protected Vector<Vector2d> pushDirections(PitchField attackerField, PitchField defenderField){
		Vector2d mainDirection = new Vector2d(defenderField.getPos());
		mainDirection.sub(attackerField.getPos());
		Vector2d[] potentialDirections = new Vector2d[3];
		potentialDirections[0] = new Vector2d(mainDirection);
		if((int)mainDirection.x == 0){
			potentialDirections[1] = new Vector2d(-1, mainDirection.y);
			potentialDirections[2] = new Vector2d(1, mainDirection.y);
		}else if((int)mainDirection.y == 0){
			potentialDirections[1] = new Vector2d(mainDirection.x, -1);
			potentialDirections[2] = new Vector2d(mainDirection.x, 1);
		}else{
			potentialDirections[1] = new Vector2d(mainDirection.x, 0);
			potentialDirections[2] = new Vector2d(0, mainDirection.y);
		}
		Vector<Vector2d> directions = new Vector<Vector2d>();
		int emptyFields = 0;
		int outOfPitchFields = 0;
		for(Vector2d pd: potentialDirections){
			Vector2d newPosition = new Vector2d(defenderField.getPos());
			newPosition.add(pd);
			if(!(attackerField.getPlayer().getMatch().getPitch().isOnField(newPosition))){
				outOfPitchFields++;
			}else if(attackerField.getPlayer().getMatch().getPitch().getFields()[(int)newPosition.x][(int)newPosition.y].getPlayer() == null){
				emptyFields++;
			}
		}
		if(emptyFields > 0){
			for(Vector2d pd: potentialDirections){
				Vector2d newPosition = new Vector2d(defenderField.getPos());
				newPosition.add(pd);
				if(actor.getMatch().getPitch().isOnField(newPosition)){
					if(actor.getMatch().getPitch().getFields()[(int)newPosition.x][(int)newPosition.y].getPlayer() == null){
						directions.add(pd);
					}
				}
			}
		}else if(outOfPitchFields > 0){
			for(Vector2d pd: potentialDirections){
				Vector2d newPosition = new Vector2d(defenderField.getPos());
				newPosition.add(pd);
				if(!(actor.getMatch().getPitch().isOnField(newPosition))){
					directions.add(pd);
				}
			}
		}else{
			for(Vector2d pd: potentialDirections){
				directions.add(pd);
			}
		}
		return directions;
	}
	
	protected boolean pushPossible(PitchField attackerField, PitchField defenderField) {
		return pushDirections(attackerField, defenderField).size() > 0;
	}
	
	protected boolean pushPossible(PitchField attackerField, PitchField defenderField, Vector2d direction){
		Vector<Vector2d> possibilities = pushDirections(attackerField, defenderField);
		for(Vector2d d: possibilities){
			if(d.equals(direction)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * tests weather the chosen direction to push your enemy is legal and then moves (or not)
	 * @param defender the pushed person
	 * @param message SBProtocolMessage
	 * @param answer SBProtocolMessage
	 */
	public boolean chosenDirection(Player defender, SBProtocolMessage message, SBProtocolMessage answer, Player playerBackingUp, Vector2d posToBackUp){
		Vector2d actorField = new Vector2d(actor.getPos());
		Vector2d defenderField = new Vector2d(defender.getPos());
		Vector2d direction = new Vector2d(defenderField);
		direction.sub(actorField);
		Vector2d newDefenderField = new Vector2d(defenderField);
		
		int x;
		int y;
		try{
			x = Integer.parseInt(answer.getParameterContent(2));
			y = Integer.parseInt(answer.getParameterContent(3));
			}
		catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
			return false;
		}
		Vector2d walk = new Vector2d(x,y);
		walk.sub(defenderField);
		if(pushPossible(actor.getPosition(), defender.getPosition(), walk)){
			newDefenderField.add(walk);
		}else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_WRONG_DIRECTION);
			getMatch().sendMessage(actor.getTeam().getCoach().getUID(), message);
			return true;
		}
		if(!(actor.getMatch().getPitch().isOnField(newDefenderField))){
			crowdBeatsUpPlayer(defender);
			returnSuccessMessage(message, SBProtocolMessage.WORKD_DEFENDER_PUSHED);
			actor.getMatch().setGamePhase(3);
			pushAllWaitingPlayers(message);
			((RulePush)playerBackingUp.getRule(2)).backUp(posToBackUp, message, playerBackingUp);
		}else if(actor.getMatch().getPitch().getFields()[(int)newDefenderField.x][(int)newDefenderField.y].getPlayer() == null){
			//liegt der ball auf dem feld? -> scatter
			if (getMatch().getPitch().getBallPos().x == (int)newDefenderField.x && getMatch().getPitch().getBallPos().y == (int)newDefenderField.y){
				Vector2d newBallPos = new Vector2d (actor.getMatch().getPitch().getBallPos());
				newBallPos.add(actor.getMatch().scatter());
				actor.getMatch().getPitch().setBallPos(newBallPos);
			}
			((ServerMatch)actor.getMatch()).addCurrentPlayersBeingPushed(defender);
			((ServerMatch)actor.getMatch()).addCurrentPlayerPositionsBeingPushed(newDefenderField);
			pushAllWaitingPlayers(message);
			actor.getMatch().setGamePhase(3);
			((RulePush)playerBackingUp.getRule(2)).backUp(posToBackUp, message, playerBackingUp);
		}else{
			Player newDefender = actor.getMatch().getPitch().getFields()[(int)newDefenderField.x][(int)newDefenderField.y].getPlayer();
			((ServerMatch)getMatch()).addCurrentHighlitedFields(newDefender.getPos());
			((RuleBlock)actor.getRule(1)).sendHighlightFields(((ServerMatch)getMatch()).getCurrentHighlitedFields());
			((ServerMatch)actor.getMatch()).addCurrentPlayersBeingPushed(defender);
			((ServerMatch)actor.getMatch()).addCurrentPlayerPositionsBeingPushed(newDefenderField);
			((RulePush)defender.getRule(2)).apply(message, newDefender, newDefender.getPosition(), answer.getUID(), playerBackingUp, posToBackUp);
			returnSuccessMessage(message, SBProtocolMessage.WORKD_DEFENDER_PUSHED, "NEXT PUSH?");
			return true;
		}
		return true;
	}
	public void crowdBeatsUpPlayer(Player p) {
		if(p.isHoldingBall()){
			p.invokeSetIsHoldingBall(false);
			p.getMatch().getPitch().throwIn(p.getPos());
		}
		p.invokeClearPosition();
		if(p.invokeGetPlayerCondition() == PlayerCondition.FINE || p.invokeGetPlayerCondition() == PlayerCondition.PRONE){
			((RuleBlock)p.getRule(1)).injuryRoll(0);
		}
		sendMessageShowMe("Crowd", "Let's beat up this " + p.getName());
		sendMessage(p.getTeam().getCoach(), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_CROWD_BEATS_UP_YOUR_PLAYER);
	}
	
	public void pushAllWaitingPlayers(SBProtocolMessage message){
		if(((ServerMatch)actor.getMatch()).getCurrentPlayerPositionsBeingPushed().size() == ((ServerMatch)actor.getMatch()).getCurrentPlayersBeingPushed().size()){
			int numberOfPlayersBeingPushed = ((ServerMatch)actor.getMatch()).getCurrentPlayersBeingPushed().size();
			for(int i = 0; i < numberOfPlayersBeingPushed; i++){
				((ServerMatch)actor.getMatch()).getCurrentPlayersBeingPushed().get(numberOfPlayersBeingPushed-i-1).invokeSetPosition(((ServerMatch)actor.getMatch()).getCurrentPlayerPositionsBeingPushed().get(numberOfPlayersBeingPushed-i-1));
				returnSuccessMessage(message, SBProtocolMessage.WORKD_DEFENDER_PUSHED);
			}
		}else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_PUSHING_NOT_POSSIBLE);
		}
	}
}


package gameLogic.rules;

import javax.vecmath.Vector2d;

import gameLogic.*;
import network.SBProtocolMessage;
import network.SBProtocolParameterArray;
import server.logic.ServerMatch;

/**
 * rule for moving one player
 */
public class RuleMove extends Rule {

	public static final String I_AM_EXHAUSTED = "I am exhausted!";
	
	public RuleMove(Player actor) {
		super(actor);
	}
	
	/**
	 * can the player attain his goal?
	 * @param mod 		number of difficulties
	 * @return boolean	true if he is successful and false if he don't
	 */
	protected boolean stayFine(int mod){
		return geTest(mod + 1);
	}
	/**
	 * an enemy is on your target field. you try to block him.
	 * @param message	SBProtocolMessage
	 * @param i			every step of the path
	 * @param path		The way the actor wants to move
	 */
	protected void tryToBlock(SBProtocolMessage message, int i, PitchField... path){
		//enemy is on your path -> try to block him
		if(actor.getTeam().getBlitz()){
			if (actor.invokeGetRemainingBe() > 0){
				//blocking
				((RuleBlock) actor.getRule(1)).apply(message, getMatch().getPitch().getFields()[(int)path[i].getPos().x][(int)path[i].getPos().y].getPlayer());
			}
			else {
				//can't block
				sendMessageShowMe(actor.getTeam().getCoach(), actor.toString(), I_AM_EXHAUSTED);
				returnFailureMessage(message, SBProtocolMessage.FAILD_YOU_ARE_EXHAUSTED);
			}
		}else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_NO_BLITZ_LEFT);
		}
	}
	
	/**
	 * test whether there is a teammate or an enemy on your target field, and if he can block the enemy
	 * @param message	SBProtocolMessage
	 * @param i			Every step of the path
	 * @param path		The way the actor wants to move
	 */
	protected void isItEnemy(SBProtocolMessage message, int i, PitchField... path ){
		if (getMatch().getPitch().getFields()[(int)path[i].getPos().x][(int)path[i].getPos().y].getPlayer().getTeam() == actor.getTeam()){
			//own player stands on your path
			returnFailureMessage(message, SBProtocolMessage.FAILD_PATH_IS_BLOCKED);
		}else if(actor.getTeam().getBlitz()){
			tryToBlock(message, i, path);
		}else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_NO_BLITZ_LEFT);
		}
	}
	/**
	 * test whether there is the ball on your field and in case of that whether you pick him up or not
	 * @param message	SBProtocolMessage
	 * @param i			Every step of the path
	 * @param path		The way the actor wants to move
	 */
	protected boolean isBall(SBProtocolMessage message, int i, PitchField... path) {
		return actor.isHoldingBall() || !path[i].getPos().equals(actor.getMatch().getPitch().getBallPos()) || tryToPickUpBall(message, i, path);
	}
	
	protected boolean tryToPickUpBall(SBProtocolMessage message, int i, PitchField... path){
		int mod = 1-(actor.getMatch().getOpposingTeam(actor.getTeam()).getTacklezones(path[i].getPos()));
		if(getMatch().getWeather() == Weather.POURING_RAIN){
			mod--;
		}
		if(geTest(mod) && actor.invokeGetPlayerCondition()==PlayerCondition.FINE){
			return pickedUpBall(message);
		}else{
			return faildToPickUpBall(message);
		}
	}
	
	protected boolean pickedUpBall(SBProtocolMessage message){
		actor.invokeSetIsHoldingBall(true);
		returnSuccessMessage(message, SBProtocolMessage.WORKD_PICKED_UP_BALL);
		if(actor.getMatch().getPitch().isInTouchdownZone(actor.getTeam(), actor.getPos())){
			actor.getMatch().touchdown(actor.getTeam());
		}
		return true;
	}
	
	protected boolean faildToPickUpBall(SBProtocolMessage message){
		returnSuccessMessage(message, SBProtocolMessage.WORKD_FAILED_PICK_UP_BALL);
		Vector2d newBallPos = new Vector2d(actor.getMatch().getPitch().getBallPos());
		newBallPos.add(actor.getMatch().scatter());
		actor.getMatch().getPitch().setBallPos(newBallPos);
		((ServerMatch)actor.getMatch()).endTurn(message);
		return false;
	}
	
	
	/**
	 * trys to move one Field of the Path
	 * @param message	SBProtocolMessage
	 * @param i			Every step of the path
	 * @param path		The way the actor wants to move
	 * @return true if he can move further (or block), false if not for any reason
	 */
	protected boolean moveOneField(SBProtocolMessage message, int i, PitchField... path){
		if(i != 1){
			try{
				Thread.sleep(300);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}
		//is the field next to the player?
		boolean isNeighbour = getMatch().getPitch().isAdjacent(actor.getPosition(), path[i]);
		boolean isBlocked;
		 //test: is there someone standing in your way? can you block him?
		//noinspection SimplifiableIfStatement
		if(getMatch().getPitch().getFields()[(int)path[i].getPos().x][(int)path[i].getPos().y].getPlayer() == null){
			isBlocked=false;
		}else{
			isItEnemy(message, i, path);
			isBlocked=true;
		}
		if(isNeighbour && !isBlocked) {
			int problems = -(actor.getMatch().getOpposingTeam(actor.getTeam()).getTacklezones(path[i - 1].getPos()));
			//difficulties: how many tacklezones is the player standing in?
			actor.invokeSetPosition(path[i]);
			actor.invokeCountDownRemainingBe(1);
			if (problems != 0) {
				if(!(tackleTest(message, problems, i, path))){
					return false;
				}
			}
			return isBall(message, i, path);
		}else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_PATH_NOT_REACHABLE);
			return false;
		}
	}

	/**
	 * main method, moving
	 * @param message	SBProtocolMessage
	 * @param path 		The way he wants to move
	 */
	public void apply(SBProtocolMessage message, PitchField... path) {
		if(path.length > 1){
			checkForMovingPlayer(actor);
		}
		if(actor.invokeGetRemainingBe()==0){
			sendMessageShowMe(actor.getTeam().getCoach(), actor.toString(), I_AM_EXHAUSTED);
			returnFailureMessage(message, SBProtocolMessage.FAILD_YOU_ARE_EXHAUSTED);
		}else{
			if(actor.invokeGetPlayerCondition().equals(PlayerCondition.PRONE)){
				checkForMovingPlayer(actor);
				if(actor.invokeGetRemainingBe() > 2){
					actor.invokeSetPlayerCondition(PlayerCondition.FINE);
					actor.invokeCountDownRemainingBe(3);
					actor.getTeam().setMovingPlayer(actor);
					returnSuccessMessage(message, SBProtocolMessage.WORKD_PLAYER_IS_NOW_FINE);
				}else{
					if(actor.getMatch().d6.throwDie() > 3){
						actor.invokeSetPlayerCondition(PlayerCondition.FINE);
						returnSuccessMessage(message, SBProtocolMessage.WORKD_PLAYER_IS_NOW_FINE);
						actor.invokeSetRemainingBe(0);
					}else{
						sendMessageShowMe(actor.getTeam().getCoach(), actor.toString(), "I can't get up, it hurts to much");
						actor.invokeSetRemainingBe(0);
					}
				}
			}
			actor.getMatch().sendPlayer(actor);
			if(actor.getMatch().getPitch().isOnField(actor.getPos()) && actor.invokeGetPlayerCondition() == PlayerCondition.FINE){
				//test: is the path too long? in case of that, send a failure message
				if (path.length-1 <= actor.invokeGetRemainingBe()){
					for(int i=1;i<path.length;i++){
						if(actor.invokeGetPlayerCondition() == PlayerCondition.FINE){
							getMatch().sendBallPos();
							if(!(moveOneField(message, i, path))){break;}
						}
					}
				}else{
					returnFailureMessage(message, SBProtocolMessage.FAILD_PATH_TOO_LONG);
				}
			}else{
				returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_CANNOT_TAKE_ACTION);
			}
		}
	}
	
	protected boolean beingTackled(SBProtocolMessage message, int i, PitchField... path){
		if (actor.isHoldingBall()) {
			returnSuccessMessage(message, SBProtocolMessage.WORKD_YOU_HAVE_LOST_YOUR_BALLS);
		}
		//player gets tackled, he falls down and its no longer your turn
		sendMessageShowMe(actor.toString(), "Dammit, I was tackled!");
		((RuleBlock) actor.getRule(1)).playerDown(message, actor, RuleBlock.YOU_SUCK);
		actor.invokeSetRemainingBe(0);
		returnSuccessMessage(message, SBProtocolMessage.WORKD_YOU_ARE_TACKLED);
		actor.getMatch().sendPlayer(actor);
		if(path[i].getPos().equals(actor.getMatch().getPitch().getBallPos())){
			Vector2d newBallPos = new Vector2d(actor.getMatch().getPitch().getBallPos());
			newBallPos.add(actor.getMatch().scatter());
			actor.getMatch().getPitch().setBallPos(newBallPos);
		}
		return false;
	}
	
	protected boolean tackleTest(SBProtocolMessage message, int problems, int i, PitchField... path) {
		return stayFine(problems) || beingTackled(message, i, path);
	}
}

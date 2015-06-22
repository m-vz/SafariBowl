package gameLogic.rules;

import java.util.Vector;

import gameLogic.*;
import network.SBProtocolCommand;
import network.SBProtocolMessage;
import server.logic.ServerMatch;

import javax.vecmath.Vector2d;
/**
 * The game rule for throwing stuff.
 */
public class RuleThrow extends Rule {
	
	public static int QUICK_PASS=4;
	public static int SHORT_PASS=8;
	public static int LONG_PASS=12;
	public static int LONG_BOMB=16;
	public static final int INTERCEPT_DISTANCE = 1;
	
	
	public RuleThrow(Player actor) {
		super(actor);
	}
	/**
	 * where lands the ball if he doesnt pass the target
	 * @param targetField	the target field
	 */
	protected void notWellThrown(Vector2d targetField, SBProtocolMessage message){
		Vector2d failField=new Vector2d(targetField);
		failField.add(actor.getMatch().scatter());
		failField.add(actor.getMatch().scatter());
		failField.add(actor.getMatch().scatter());
		actor.getMatch().getPitch().setBallPos(failField);
		returnSuccessMessage(message, SBProtocolMessage.WORKD_NOT_WELL_THROWN);
	}
	
	protected void successfulThrow(SBProtocolMessage message, Vector2d destinationField){
		actor.getMatch().getPitch().setBallPos(destinationField, true);
		returnSuccessMessage(message, SBProtocolMessage.WORKD_SUCCESSFUL_THROW);
	}
	
	/**
	 * the actual throw
	 * @param message the message that initiated the throw
	 * @param destinationField where to throw
	 * @param problems how difficult it is to throw
	 */
	protected void throwBall(SBProtocolMessage message, Vector2d destinationField, int problems){
		if(geTest(problems)){
			successfulThrow(message, destinationField);
		}else{
			notWellThrown(destinationField, message);
		}
		actor.invokeSetIsHoldingBall(false);
		actor.invokeSetRemainingBe(0);
		actor.getTeam().setPass(false);
		actor.getMatch().sendBallPos();
		if(((ServerMatch)actor.getMatch()).findTeamHoldingTheBall() != actor.getMatch().findUserIndex(message)){
			((ServerMatch)actor.getMatch()).endTurn(message);
		}
		actor.getMatch().sendPlayer(actor);
	}
	
	/**
	 * the main method
	 * @param message The message that caused this rule to be applied.
	 * @param destination The pitch field to throw the ball to.
	 */
	public void apply(SBProtocolMessage message, PitchField destination) {
		if(actor.invokeGetRemainingBe() == 0 && !(actor.getTeam().getMovingPlayer() == actor)){
			sendMessageShowMe(actor.getTeam().getCoach(), actor.toString(), RuleMove.I_AM_EXHAUSTED);
			returnFailureMessage(message, SBProtocolMessage.FAILD_YOU_ARE_EXHAUSTED);
			return;
		}
		checkForMovingPlayer(actor);
		if(actor.getMatch().getPitch().isOnField(actor.getPos()) && actor.invokeGetPlayerCondition() == PlayerCondition.FINE){
			if(actor.isHoldingBall()){
				if(actor.getTeam().getPass()){
					Vector2d destinationField = new Vector2d(destination.getPos());
					Vector2d actorField = new Vector2d(actor.getPos());
					Vector2d difference = new Vector2d(destinationField);
					difference.sub(actorField);
					double distance = Math.sqrt((difference.x)*(difference.x)+(difference.y)*(difference.y));
					int problems = -(actor.getMatch().getOpposingTeam(actor.getTeam()).getTacklezones(actor.getPos()));
					if(findThrowModificator(problems, distance) < 1000){//if 1000 or higher, then its to far away.
						askForInterception(message, destinationField, findThrowModificator(problems, distance));
					}else{returnFailureMessage(message, SBProtocolMessage.FAILD_TOO_FAR_AWAY);}
				}else{
					returnFailureMessage(message, SBProtocolMessage.FAILD_NO_PASS_LEFT);
				}
			}else{
				returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_DOESNT_HOLD_THE_BALL);
			}
		}else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_CANNOT_TAKE_ACTION);
		}
	}
	
	protected int findThrowModificator(int problems, double distance){
		int temp = problems;
		if(getMatch().getWeather() == Weather.VERY_SUNNY){
			temp++;
		}
		if(distance<=QUICK_PASS){
			return temp+1;
		}else if(distance<=SHORT_PASS){
			return temp;
		}else if(distance<=LONG_PASS){
			return temp-1;
		}else if(distance<=LONG_BOMB){
			return temp-2;
		}else{
			return 2000;
		}
	}
	
	public int getLONG_BOMB(){
		return LONG_BOMB;
	}
	
	protected double getDistanceFromPassLine(Vector2d a, Vector2d b, Vector2d p){
		double quotient = Math.abs((b.y-a.y)*(p.x-a.x)-(b.x-a.x)*(p.y-a.y));
		double divisor = Math.sqrt((b.x-a.x)*(b.x-a.x)+(b.y-a.y)*(b.y-a.y));
		return (quotient/divisor);
	}
	
	private double getDistance(Vector2d a, Vector2d b){
		return Math.sqrt((a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y));
	}
	
	protected Vector<PitchField> findInterceptPositions(Vector2d a, Vector2d b){
		Vector<PitchField> possiblePositions = new Vector<PitchField>();
		Vector2d iterationA = new Vector2d(a);
		Vector2d iterationB = new Vector2d(b);
		if(a.x > b.x){
			double temp = iterationA.x;
			iterationA = new Vector2d(iterationB.x, iterationA.y);
			iterationB = new Vector2d(temp, iterationB.y);
		}
		if(a.y > b.y){
			double temp = iterationA.y;
			iterationA = new Vector2d(iterationA.x, iterationB.y);
			iterationB = new Vector2d(iterationB.x, temp);
		}
		for(int j = (int)iterationA.y-1; j < (int)iterationB.y+2; j++){
			for(int i = (int)iterationA.x-1; i < (int)iterationB.x+2; i++){
				if(getMatch().getPitch().isOnField(i, j)){
					if(getDistanceFromPassLine(a, b, getMatch().getPitch().getFields()[i][j].getPos()) <= INTERCEPT_DISTANCE
							&& getDistance(getMatch().getPitch().getFields()[i][j].getPos(), a) < getDistance(a, b) 
							&& getDistance(getMatch().getPitch().getFields()[i][j].getPos(), b) < getDistance(a, b)){
						possiblePositions.add(getMatch().getPitch().getFields()[i][j]);
					}
				}
			}
		}
		return possiblePositions;
	}
	
	protected Vector<Player> findPossibleInterceptors(Player thrower, Vector2d b){
		int teamIndex = -1;
		if(thrower.getTeam() == getMatch().getTeam(0)){
			teamIndex = 0;
		}else if(thrower.getTeam() == getMatch().getTeam(1)){
			teamIndex = 1;
		}else{
			return null;
		}
		Vector<Player> possibleInterceptors = new Vector<Player>();
		for(PitchField field: findInterceptPositions(thrower.getPos(), b)){
			if(field.getPlayer() != null){
				if(field.getPlayer().getTeam() != getMatch().getTeam(teamIndex) && field.getPlayer().invokeGetPlayerCondition() == PlayerCondition.FINE){
					possibleInterceptors.add(field.getPlayer());
				}
			}
		}
		return possibleInterceptors;
	}
	
	protected void askForInterception(SBProtocolMessage message, Vector2d destinationField, int throwModificator) {
		Vector<Player> possibleInterceptors = findPossibleInterceptors(actor, destinationField);
		if(possibleInterceptors.size() == 0){
			throwBall(message, destinationField, throwModificator);
		}else{
			Vector<String> parameters = new Vector<String>();
			parameters.add(SBProtocolMessage.EVENT_API_CHOICE);
			parameters.add(SBProtocolMessage.EVENT_ASK_FOR_INTERCEPTOR);
			for(Player player:possibleInterceptors){
				if(player.getTeam() == getMatch().getTeam(0)){
					parameters.add("0");
					parameters.add((player.getId()-1) + "");
				}else if(player.getTeam() == getMatch().getTeam(1)){
					parameters.add("1");
					parameters.add((player.getId()-1) + "");
				}
			}
			String[] parameterArray = parameters.toArray(new String[parameters.size()]);
			((ServerMatch)getMatch()).setCurrentModificatorWaitingForAnser(throwModificator);
			((ServerMatch)getMatch()).setCurrentDefenderFieldWaitingForAnswer(destinationField);
			((ServerMatch)getMatch()).setCurrentMessageWaitingForAnswer(message);
			((ServerMatch)getMatch()).setCurrentActorWaitingForAnswer(actor);
			getMatch().setGamePhase(5);
			sendMessage(getMatch().getOpponent(actor.getTeam().getCoach()), SBProtocolCommand.EVENT, parameterArray);
		}
	}
	
	public void intercept(Player player, int teamIndex, int playerIndex, int userIndex){
		getMatch().setGamePhase(3);
		int mod = -2;
		mod -= getMatch().getOpposingTeam(getMatch().getTeam(teamIndex)).getTacklezones(getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getPos());
		if(getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getRule(3).geTest(mod)){
			player.invokeSetIsHoldingBall(false);
			getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).invokeSetIsHoldingBall(true);
			getMatch().getPitch().adjustBallPos(getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getPos());
			int actingUserIndex = -1;
			if(userIndex == 0){
				actingUserIndex = 1;
			}else if(userIndex == 1){
				actingUserIndex = 0;
			}
			sendMessageShowMe(getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).toString(), "Successfully intercepted");
			((ServerMatch)getMatch()).endTurn(actingUserIndex);
		}else{
			((RuleThrow)player.getRule(3)).throwBall(((ServerMatch)getMatch()).getCurrentMessageWaitingForAnswer(), ((ServerMatch)getMatch()).getCurrentDefenderFieldWaitingForAnser(), ((ServerMatch)getMatch()).getCurrentModificatorWaitingForAnser());
		}
	}
}

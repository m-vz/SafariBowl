package gameLogic.rules;

import java.util.Vector;

import javax.vecmath.Vector2d;

import GUI.SBColor;

import java.awt.Color;

import gameLogic.*;
import network.SBProtocolCommand;
import network.SBProtocolMessage;
import network.SBProtocolParameter;
import network.SBProtocolParameterArray;
import server.logic.ServerMatch;

/**
 * The game rule for blocking stuff.
 */
public class RuleBlock extends Rule {
	public static final String ENEMY_DOWN = "YOUR ENEMY IS ";
	public static final String YOU_SUCK = "YOU ARE ";
	public static final String ENEMY_FOULED = "YOU HAVE BEATEN UP YOUR ENEMY. YOUR ENEMY IS ";
	
	SBProtocolMessage message;
	//Player defender;

	public RuleBlock(Player actor) {
		super(actor);
	}
	/**
	 * switch-case: which method gets executed
	 * @param result	which method is chosen
	 * @param defender	defending Player
	 * @param message	SBProtocolMessage
	 */
	public boolean reaction(int result, Player defender, SBProtocolMessage message){
		switch(result){
			case 1: 
				sendMessageShowMe("Block result", "Attacker down!");
				return attackerDown(message);
			case 2:	
				sendMessageShowMe("Block result", "Both down!");
				return bothDown(message, defender);
			case 3: 
				sendMessageShowMe("Block result", "Pushed!");
				return pushed(message, defender);
			case 4: 
				sendMessageShowMe("Block result", "Defender Stumbles!");
				return defenderStumbles(message, defender);
			case 5: 
				sendMessageShowMe("Block result", "Defender Down!");
				return defenderDown(message, defender);
			default: return false;
		}
	}	
	
	/**
	 * the main blocking method
	 * @param message the message that initiated the block
	 * @param defender the blocked player
	 */
	public void apply(SBProtocolMessage message, Player defender) {
		checkForMovingPlayer(actor);
		boolean playerIsFine = (actor.getMatch().getPitch().isOnField(actor.getPos()) && actor.invokeGetPlayerCondition() == PlayerCondition.FINE);
		boolean playerHasRemainingBe = (actor.invokeGetRemainingBe() > 0);
		boolean playerWantsToBlitz = (actor.invokeGetRemainingBe() != actor.invokeGetBe());
		boolean teamCanBlitz = actor.getTeam().getBlitz();
		if(playerIsFine && playerHasRemainingBe){
			if((!playerWantsToBlitz || teamCanBlitz) || defender.invokeGetPlayerCondition() != PlayerCondition.FINE){
				// Set fields for defender and message so they are available in attackerDown() and bothDown()
				this.message = message;
				if (getMatch().getPitch().isAdjacent(actor.getPosition(), defender.getPosition())
						&& actor.invokeGetPlayerCondition() == PlayerCondition.FINE){
					if(playerWantsToBlitz){
						actor.getTeam().setBlitz(false);
						actor.invokeCountDownRemainingBe(1);
					}
					if(defender.invokeGetPlayerCondition()== PlayerCondition.FINE){
						throwDice(message, defender);
						if(!playerWantsToBlitz){actor.invokeSetRemainingBe(0);}
					}else if(defender.invokeGetPlayerCondition()== PlayerCondition.PRONE || defender.invokeGetPlayerCondition()== PlayerCondition.STUNNED){
						beatHim(defender, message, playerWantsToBlitz);
					}
				} else returnFailureMessage(message, SBProtocolMessage.FAILD_BLOCKING_NOT_POSSIBLE);
			} else returnFailureMessage(message, SBProtocolMessage.FAILD_NO_BLITZ_LEFT);
		} else returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_CANNOT_TAKE_ACTION);
		
	}
/**
 * The defender moves one field back and falls down even if he has the specialrule block 
 * @param defender	defending Player
 * @param message	SBProtocolMessage
 */
	protected boolean defenderDown(SBProtocolMessage message, Player defender) {
		((RuleBlock)defender.getRule(1)).beingBlockedDefenderDown(message, actor, defender, 0, 0);
		return true;
	}
/**
 * The defender moves one field back and falls down. the actor moves on the defenders startpoint. when the defender has de specialrule block, the defender is only pushed.
 * @param defender	defending Player
 * @param message	SBProtocolMessage
 */
	protected boolean defenderStumbles(SBProtocolMessage message, Player defender) {
		((RuleBlock)defender.getRule(1)).beingBlockedDefenderStumbles(message, actor, defender, 0, 0);
		return true;
	}
/**
 * The defender moves one field back and the actor moves on the defenders startpoint.
 * @param defender	defending Player
 * @param message	SBProtocolMessage
 */
	protected boolean pushed(SBProtocolMessage message, Player defender) {
		((RuleBlock)defender.getRule(1)).beingBlockedPushed(message, actor, defender.getPosition());
		return true;
	}
/**
 * The actor falls down.
 * @param message	SBProtocolMessage
 */
	protected boolean attackerDown(SBProtocolMessage message) {
		playerDown(message, actor, YOU_SUCK);
		clearHighlightFields();
		return true;
	}
/**
 * Both fall down except they have the specialrule block.
 * @param defender	defending Player
 * @param message	SBProtocolMessage
 */
	protected boolean bothDown(SBProtocolMessage message, Player defender) {
		((RuleBlock)defender.getRule(1)).beingBlockedBothDown(message, 0, 0);
		playerDown(message, actor, YOU_SUCK);
		clearHighlightFields();
		return true;
	}
	
	public void playerDown(SBProtocolMessage message, Player p, String s){
		playerDown(message, p, s, 0, 0);
	}
	
	public void playerDown(SBProtocolMessage message, Player p, String s, int armorRollModifier, int injuryRollModifier){
		PlayerCondition defenderCondition = p.invokeGetPlayerCondition();
		int armorRoll = p.getMatch().d6.throwDie() + p.getMatch().d6.throwDie() + armorRollModifier;
		if(armorRoll > p.invokeGetRs()){
			s = ((RuleBlock)p.getRule(1)).injuryRoll(injuryRollModifier);
		}else{
			p.invokeSetPlayerCondition(PlayerCondition.PRONE);
			s = "prone";
		}
		if(defenderCondition == PlayerCondition.STUNNED && p.invokeGetPlayerCondition() == PlayerCondition.PRONE){
			sendMessageShowMe(p.toString(), "Next time you have to hit harder!");
		}else{
			sendMessageShowMe(p.toString(), "I am " + s + "!");
		}
		returnSuccessMessage(message, s);
		int teamIndex;
		if(p.getTeam().equals(p.getMatch().getTeam(0))){
			teamIndex = 0;
		}else if(p.getTeam().equals(p.getMatch().getTeam(1))){
			teamIndex = 1;
		}else{
			return;
		}
		if(((ServerMatch)p.getMatch()).checkUserTurn(teamIndex)){
			((ServerMatch)p.getMatch()).endTurn(teamIndex);
		}
	}
	
	public String injuryRoll(int modifier){
		String s = "";
		int injuryRoll = actor.getMatch().d6.throwDie() + actor.getMatch().d6.throwDie();
		if(injuryRoll + modifier < 8){
			actor.invokeSetPlayerCondition(PlayerCondition.STUNNED);
			s += "stunned";
		}else{
			if(injuryRoll + modifier < 10){
				actor.invokeSetPlayerCondition(PlayerCondition.KO);
				s += "KO";
			}else{
				int casultyRoll = actor.getMatch().d6.throwDie() * 10 + actor.getMatch().d8.throwDie(); 
				if(casultyRoll < 61){
					actor.invokeSetPlayerCondition(PlayerCondition.INJURED);
					s += "injured";
				}else{
					actor.invokeSetPlayerCondition(PlayerCondition.DEAD);
					s += "dead";
				}
				actor.getMatch().addCasualty(actor);
			}
			actor.invokeClearPosition();
		}
		return s;
	}
	
	public boolean blockReaction(SBProtocolMessage message, SBProtocolMessage answer, Player defender){
		actor.getMatch().setGamePhase(3);
		int dice1, dice2, dice3;
		try{
			dice1 = Integer.parseInt(message.getParameterContent(1)); 
			dice2 = Integer.parseInt(message.getParameterContent(2));
			try {
				dice3 = Integer.parseInt(message.getParameterContent(3));
			} catch(ArrayIndexOutOfBoundsException e) { // only two dice were given
				dice3 = dice1;
			}
		}catch(NumberFormatException e){
			return false;
		}
		switch(Integer.parseInt(answer.getParameterContent(2))){ // if answer was dice index 1 or 2, set dice1 to chosen dice index
			case 1: dice1 = dice2; break;
			case 2: dice1 = dice3; break;
		}
		return reaction(dice1, defender, answer);
	}
	
	public void beingBlockedDefenderDown(SBProtocolMessage message, Player attacker, Player defender, int firstThrowModifier, int injuryRollModifier){
		Vector2d posToBackup = new Vector2d(defender.getPos());
		playerDown(message, actor, ENEMY_DOWN, firstThrowModifier, injuryRollModifier);
		((ServerMatch)actor.getMatch()).clearCurrentPlayerPositionsBeingPushed();
		((ServerMatch)actor.getMatch()).clearCurrentPlayersBeingPushed();
		((RulePush)attacker.getRule(2)).apply(message, defender, defender.getPosition(), attacker.getTeam().getCoach().getUID(), attacker, posToBackup);
	}
	
	public void beingBlockedDefenderStumbles(SBProtocolMessage message, Player attacker, Player defender, int firstThrowModifier, int injuryRollModifier){
		Vector2d posToBackup = new Vector2d(defender.getPos());
		playerDown(message, actor, ENEMY_DOWN, firstThrowModifier, injuryRollModifier);
		((ServerMatch)actor.getMatch()).clearCurrentPlayerPositionsBeingPushed();
		((ServerMatch)actor.getMatch()).clearCurrentPlayersBeingPushed();
		((RulePush)attacker.getRule(2)).apply(message, defender, defender.getPosition(), attacker.getTeam().getCoach().getUID(), attacker, posToBackup);
	}
	
	public void beingBlockedPushed(SBProtocolMessage message, Player attacker, PitchField defenderField){
		Vector2d posToBackup = new Vector2d(defenderField.getPos());
		((ServerMatch)actor.getMatch()).clearCurrentPlayerPositionsBeingPushed();
		((ServerMatch)actor.getMatch()).clearCurrentPlayersBeingPushed();
		((RulePush) attacker.getRule(2)).apply(message, actor, defenderField, attacker.getTeam().getCoach().getUID(), attacker, posToBackup);
	}
	
	public void beingBlockedBothDown(SBProtocolMessage message, int firstThrowModifier, int injuryRollModifier){
		playerDown(message, actor, ENEMY_DOWN, firstThrowModifier, injuryRollModifier);
	}
	public void beatHim(Player defender, SBProtocolMessage message, boolean playerWantsToBlitz){
		if(actor.getTeam().getFoul()){
			int armorRollModifier = actor.getTeam().getTacklezones(defender.getPos()) -1;
			armorRollModifier += defender.getTeam().getTacklezones(actor.getPos());
			PlayerCondition defenderCondition = defender.invokeGetPlayerCondition();
			((RuleBlock)defender.getRule(1)).playerDown(message, defender, ENEMY_FOULED, armorRollModifier, 0);
			if(defenderCondition == PlayerCondition.STUNNED && defender.invokeGetPlayerCondition() == PlayerCondition.PRONE){
				defender.invokeSetPlayerCondition(PlayerCondition.STUNNED);
			}
			if(!playerWantsToBlitz){actor.invokeSetRemainingBe(0);}
			actor.getTeam().setFoul(false);
			refereeTriesToKeepSurvey(message);
		}
		else{
			sendMessageShowMe(actor.getTeam().getCoach(), actor.toString(), "Referee is watching me, I can't foul!");
			returnFailureMessage(message, SBProtocolMessage.FAILD_NO_FOUL_LEFT);
		}
	}
	public void refereeTriesToKeepSurvey(SBProtocolMessage message){
		int refereeThrow1 = actor.getMatch().d6.throwDie();
		int refereeThrow2 = actor.getMatch().d6.throwDie();
		if(refereeThrow1 == refereeThrow2){
			if(actor.isHoldingBall()){
				actor.invokeSetIsHoldingBall(false);
				Vector2d newBallPos = new Vector2d(actor.getPos());
				newBallPos.add(actor.getMatch().scatter());
				actor.getMatch().getPitch().setBallPos(newBallPos);
			}
			actor.invokeClearPosition();
			actor.invokeSetRedCard(true);
			int actingUserIndex = -1;
			if(actor.getMatch().getTeam(0) == actor.getTeam()){
				actingUserIndex = 0;
			}else if(actor.getMatch().getTeam(1) == actor.getTeam()){
				actingUserIndex = 1;
			}
			((ServerMatch)actor.getMatch()).endTurn(actingUserIndex);
			sendMessageShowMe(actor.toString(), "This stupid referee sent me of the pitch!");
			returnSuccessMessage(message, SBProtocolMessage.WORKD_PLAYER_HAS_BEEN_SENT_OFF_THE_PITCH_BY_REFEREE);
		}
	}
	
	public void throwDice(SBProtocolMessage message, Player defender){
		((ServerMatch)getMatch()).addCurrentHighlitedFields(actor.getPos());
		((ServerMatch)getMatch()).addCurrentHighlitedFields(defender.getPos());
		sendHighlightFields(((ServerMatch)getMatch()).getCurrentHighlitedFields());
		int dice1 ,dice2, dice3;
		int forceActor = actor.invokeGetSt() + actor.getTeam().getTacklezones(defender.getPos()) - 1;
		int forceDefender = defender.invokeGetSt() + defender.getTeam().getTacklezones(actor.getPos()) - 1;
		if (forceActor == forceDefender){
		//same force
			reaction(actor.getMatch().db.throwDie(), defender, message);
		}else{
			// set the actor and the defender in the match so it knows who was waiting for an answer.
			((ServerMatch) getMatch()).setCurrentActorWaitingForAnswer(actor);
			((ServerMatch) getMatch()).setCurrentDefenderWaitingForAnswer(defender);
			// send question
			if(forceActor < forceDefender && forceDefender <= 2* forceActor){
				dice1 = actor.getMatch().db.throwDie();
				dice2 = actor.getMatch().db.throwDie();
				getMatch().sendMessage(defender.getTeam().getCoach(), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_WHAT_DIE, dice1+"", dice2+"");
			}else if(forceDefender < forceActor && forceActor <= 2* forceDefender){
				dice1 = actor.getMatch().db.throwDie();
				dice2 = actor.getMatch().db.throwDie();
				getMatch().sendMessage(actor.getTeam().getCoach(), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_WHAT_DIE, dice1+"", dice2+"");
			}else if(forceActor < forceDefender && forceDefender > 2* forceActor){
				dice1 = actor.getMatch().db.throwDie();
				dice2 = actor.getMatch().db.throwDie();
				dice3 = actor.getMatch().db.throwDie();
				getMatch().sendMessage(defender.getTeam().getCoach(), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_WHAT_DIE, dice1+"", dice2+"", dice3+"");
			}else if (forceDefender < forceActor && forceActor > 2*forceDefender){
				dice1 = actor.getMatch().db.throwDie();
				dice2 = actor.getMatch().db.throwDie();
				dice3 = actor.getMatch().db.throwDie();
				getMatch().sendMessage(actor.getTeam().getCoach(), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_WHAT_DIE, dice1+"", dice2+"", dice3+"");
			}else{
				returnFailureMessage(message, SBProtocolMessage.FAILD_PARAMANIA_HAS_TAKEN_OVER);
				return;
			}
			actor.getMatch().setGamePhase(5);
		}
	}
	
	public void sendHighlightFields(Vector<Vector2d> fields){
		Vector<Color> colors = new Vector<Color>();
		colors.add(SBColor.BLACK_80);
		for(int i = 0; i < fields.size()-1; i++){
			colors.add(SBColor.RED_80);
		}
		sendHighlightFields(fields, colors);
	}
	
	public void sendHighlightFields(Vector<Vector2d> fields, Vector<Color> colors){
		SBProtocolParameterArray parameters = new SBProtocolParameterArray();
		parameters.addParameter(new SBProtocolParameter(SBProtocolMessage.EVENT_API_HIGHLIGHT));
		for(int i = 0; i < fields.size(); i++){
			parameters.addParameter(new SBProtocolParameter((int)fields.get(i).x + ""));
			parameters.addParameter(new SBProtocolParameter((int)fields.get(i).y + ""));
			parameters.addParameter(new SBProtocolParameter(colors.get(i).getRed() + ""));
			parameters.addParameter(new SBProtocolParameter(colors.get(i).getGreen() + ""));
			parameters.addParameter(new SBProtocolParameter(colors.get(i).getBlue() + ""));
			parameters.addParameter(new SBProtocolParameter(colors.get(i).getAlpha() + ""));
		}
		sendMessage(getMatch().getUser(0), SBProtocolCommand.EVENT, parameters);
		sendMessage(getMatch().getUser(1), SBProtocolCommand.EVENT, parameters);
	}
	
	public void clearHighlightFields(){
		((ServerMatch)getMatch()).clearCurrentHighlitedFields();
		sendMessage(getMatch().getUser(0), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_API_HIGHLIGHT);
		sendMessage(getMatch().getUser(1), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_API_HIGHLIGHT);
	}
}
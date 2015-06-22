load("nashorn:mozilla_compat.js");
var imports = new JavaImporter(Packages.client,
                               Packages.client.display,
                               Packages.client.logic,
                               Packages.server,
                               Packages.server.display,
                               Packages.server.logic,
                               Packages.gameLogic,
                               Packages.gameLogic.dice,
                               Packages.gameLogic.rules,
                               Packages.GUI,
                               Packages.network,
                               Packages.util);
with(imports) {

	function setDescriptionLines(player, descriptionLines) {
		var lines = [
			"Cheater.",
			"Cannot pick up the ball.",
			"Throws coconuts at his enemies.",
			"Just pushed on 'defender stumbles'.",
			"Very good at passing through tackle",
			"zones, ignores helping tackle zones",
			"of other enemies, when dodging."
		];
		return Java.to(lines, Java.type("java.lang.String[]"));
	}
	
	var cheatedKey = "cheated";
	var alreadyDodgedKey = "hasAlreadyDodged";
	var noHandsString = "Sorry, but I have no hands left!";
	var API_AIM_FUNCTION_NAME = "apiAim";
	var SBProtocolCommand = Java.type("network.SBProtocolCommand");
	var SBProtocolMessage = Java.type("network.SBProtocolMessage");
	
	function apiAim(player, destX, destY, userIndex){
		var Math = Java.type("java.lang.Math");
		var distance = Math.sqrt((player.getPos().x - destX)*(player.getPos().x - destX) + (player.getPos().y - destY)*(player.getPos().y - destY));
		var problems = -(player.getMatch().getOpposingTeam(player.getTeam()).getTacklezones(player.getPos()));
		var mod = player.getRule(3).findThrowModificator(problems, distance);
		if(player.getRule(3).geTest(mod)){
			coconutHail(player, destX, destY);
		}else{
			notWellThrownCoconut(player, destX, destY);
		}
		player.invokeSetRemainingBe(0);
		player.getMatch().setGamePhase(3);
	}
	function notWellThrownCoconut(player, destX, destY){
		var Vector2d = Java.type("javax.vecmath.Vector2d");
		var destPos = new Vector2d(destX, destY);
		for(var i = 0; i < 3; i++){
			destPos.add(player.getMatch().scatter());
		}
		coconutHail(player, destPos.x, destPos.y);
	}
	function coconutHail(player, destX, destY){
		var PitchField = Java.type("gameLogic.PitchField");
		playerDownCoconut(player, destX, destY);
		var neighboursArray = Java.from(player.getMatch().getPitch().getNeighbours(destX, destY));
		for(var i = 0; i < neighboursArray.length; i++){
			if(player.getMatch().d6.throwDie() > 3){
				playerDownCoconut(player, neighboursArray[i].getPos().x, neighboursArray[i].getPos().y);
			}
		}
	}
	function playerDownCoconut(player, destX, destY){
		if(player.getMatch().getPitch().getFields()[destX][destY].getPlayer() != null){
			var defender = player.getMatch().getPitch().getFields()[destX][destY].getPlayer();
			var PlayerCondition = Java.type("gameLogic.PlayerCondition");
			var message = new SBProtocolMessage(player.getTeam().getCoach().getUID(), SBProtocolCommand.EVENT, " ");
			defenderCondition = defender.invokeGetPlayerCondition();
			defenderPosition = defender.getPosition();
			defender.getRule(1).playerDown(message, defender, ENEMY_DOWN);
			if(defenderCondition == PlayerCondition.STUNNED && defender.invokeGetPlayerCondition() == PlayerCondition.PRONE){
				defender.invokeSetPlayerCondition(PlayerCondition.STUNNED);
			}
		}
	}

    function setPrice(player, price) {
        return 40000;
    }
    function setGe(player, ge) {
        return 3;
    }
    function setRs(player, rs) {
        return 7;
    }
    function setSt(player, st) {
        return 2;
    }
    function setBe(player, be) {
        return 6;
    }
    function adjustMaxHeadcount(player, maxHeadcount) {
        return 1;
    }
    function setRules(player, ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch) {
    	player.addSpecialStat(alreadyDodgedKey, "false");
    	player.addSpecialStat(cheatedKey, "false");
        var rulesArray = [ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch];
 
        var RuleBlock = Java.type("gameLogic.rules.RuleBlock");
        var RuleBlockDodgeStunty = Java.extend(RuleBlock);
        var ruleBlockDodgeStunty = new RuleBlockDodgeStunty(rulesArray[1].getActor()) {
        	beingBlockedDefenderStumbles: function(message, attacker, firstThrowModifier, injuryRollModifier){
        		var _super_ = Java.super(ruleBlockDodgeStunty);
        		_super_.beingBlockedPushed(message, attacker, player.getPosition());
        	},
        	injuryRoll: function(modifier){
        		s = "";
        		injuryRoll = player.getMatch().d6.throwDie() + player.getMatch().d6.throwDie() + 1;
        		var PlayerCondition = Java.type("gameLogic.PlayerCondition");
        		if(injuryRoll + modifier < 8){
        			player.invokeSetPlayerCondition(PlayerCondition.STUNNED);
        			s += "stunned";
        		}else{
        			if(injuryRoll + modifier < 10){
        				player.invokeSetPlayerCondition(PlayerCondition.KO);
        				s += "KO";
        			}else{
        				casultyRoll = player.getMatch().d6.throwDie() * 10 + player.getMatch().d8.throwDie(); 
        				if(casultyRoll < 61){
        					player.invokeSetPlayerCondition(PlayerCondition.INJURED);
        					s += "injured";
        				}else{
        					player.invokeSetPlayerCondition(PlayerCondition.DEAD);
        					s += "dead";
        				}
        				player.getMatch().addCasualty(player);
        			}
        			player.invokeClearPosition();
        		}
        		return s;
        	}
        };
        rulesArray[1] = ruleBlockDodgeStunty;
        
        var RuleMove = Java.type("gameLogic.rules.RuleMove");
        var RuleMoveDodgeStuntyNoHandsCheater = Java.extend(RuleMove);
        var ruleMoveDodgeStuntyNoHandsCheater = new RuleMoveDodgeStuntyNoHandsCheater(rulesArray[0].getActor()){
        	tackleTest: function(message, problems, i, path){
        		var _super_ = Java.super(ruleMoveDodgeStuntyNoHandsCheater);
        		if(!(_super_.stayFine(problems))){
        			if(player.getSpecialStat(alreadyDodgedKey) == "false"){
        				player.setSpecialStat(alreadyDodgedKey, "true");
        				if(!(_super_.stayFine(problems))){
            				return _super_.beingTackled(message, i, path);
        				}else{
        					return true;
        				}
        			}else{
        				return _super_.beingTackled(message, i, path);
        			}
        		}else{
        			return true;
        		}
        	},
        	tryToPickUpBall: function(message, i, path){
        		var _super_ = Java.super(ruleMoveDodgeStuntyNoHandsCheater);
        		_super_.sendMessageShowMe(player.getTeam().getCoach(), player.toString(), noHandsString);
        		return _super_.faildToPickUpBall(message);
        	}
        };
        rulesArray[0] = ruleMoveDodgeStuntyNoHandsCheater;
        
        var RuleThrow = Java.type("gameLogic.rules.RuleThrow");
        var RuleThrowStuntyNoHands = Java.extend(RuleThrow);
        var ruleThrowStuntyNoHands = new RuleThrowStuntyNoHands(rulesArray[3].getActor()){
        	findThrowModificator: function(problems, distance){
        		var _super_ = Java.super(ruleThrowStuntyNoHands);
        		_super_.findThrowModificator(problems - 1, distance);
        	},
        	apply: function(message, destination){
        		var _super_ = Java.super(ruleThrowStuntyNoHands);
        		_super_.sendMessageShowMe(player.getTeam().getCoach(), player.toString(), noHandsString);
        	}
        };
        rulesArray[3] = ruleThrowStuntyNoHands;

        var RuleCatch = Java.type("gameLogic.rules.RuleCatch");
        var RuleCatchNoHands = Java.extend(RuleCatch);
        var ruleCatchNoHands = new RuleCatchNoHands(rulesArray[4].getActor()){
        	apply: function(successfulThrow){
	        	var actingUserIndex;
	    		if(player.getTeam() == player.getMatch().getTeam(0)){
	    			actingUserIndex = 0;
	    		}else if(player.getTeam() == player.getMatch().getTeam(1)){
	    			actingUserIndex = 1;
	    		}else{
	    			return;
	    		}
	    		var _super_ = Java.super(ruleCatchNoHands);
	    		_super_.scatterBallAround(actingUserIndex);
        	},
			giveBall: function(message){
				var _super_ = Java.super(ruleCatchNoHands);
				_super_.sendMessageShowMe(player.getTeam().getCoach(), player.toString(), noHandsString);
				_super_.sendMessage(message, SBProtocolCommand.EVENT, noHandsString);
				_super_.sendMessage(message, SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_GIVE_THE_BALL_TO_SOMEONE);
			}
        };
        rulesArray[4] = ruleCatchNoHands;

        var rulesToReturn = Java.to(rulesArray, Java.type("gameLogic.rules.Rule[]"));
    	return rulesToReturn;
    }
    function eventHappened(player, eventString){
    	var SBProtocolMessage = Java.type("network.SBProtocolMessage");
    	if(eventString == SBProtocolMessage.EVENT_SETUP_YOUR_TEAM){
    		if(player.getSpecialStat(cheatedKey).equals("true")){
    			player.invokeSetRedCard(true);
    		}
    	}
    }
    function setRemainingBe(player, remainingBe) {}
    function setSpecialRules(player) {
    	var SpecialRule = Java.type("gameLogic.rules.SpecialRule");
	    var specialRulesArray = [];
	    
	    var SpecialRuleCoconutHail = Java.extend(SpecialRule);
    	var specialRuleCoconutHail = new SpecialRuleCoconutHail(player, "Hail Coconut") {
    		apply: function(message){
    			var _super_ = Java.super(specialRuleCoconutHail);
    			var _super_super_ = Java.super(_super_);
    			_super_.checkForMovingPlayer(player);
    			var PlayerCondition = Java.type("gameLogic.PlayerCondition");
    			if(player.invokeGetPlayerCondition() != PlayerCondition.FINE){
	    			_super_.returnFailureMessage(message, FAILD_PLAYER_IS_NOT_IN_A_GOOD_CONDITION);
	    			return;
	    		}
    			if(player.invokeGetRemainingBe() == player.invokeGetBe()){
    				player.setSpecialStat(cheatedKey, "true");
    				var parameterArray = [];
        			parameterArray[0] = SBProtocolMessage.EVENT_API_AIM;
        			parameterArray[1] = API_AIM_FUNCTION_NAME;
        			parameterArray[2] = player.getId() - 1;
        			parameterArray[3] = player.getRule(3).getLONG_BOMB();
        			player.getMatch().setGamePhase(5);
        			player.getMatch().setCurrentActorWaitingForAnswer(player);
        			_super_.sendMessage(message, SBProtocolCommand.EVENT, parameterArray);
    			}else{
    				_super_.sendMessageShowMe(player.getTeam().getCoach(), player.toString(), "Ask again next turn!");
    				_super_.returnFailureMessage(message, SBProtocolMessage.FAILD_YOU_ARE_EXHAUSTED);
    			}
    		}
    	};
	   	specialRulesArray[0] = specialRuleCoconutHail;
	
	   	var specialRulesToReturn = Java.to(specialRulesArray, Java.type("gameLogic.rules.SpecialRule[]"));
	   	return specialRulesToReturn;
    }
    function setTackleZone(player, tackleZone) {}
    function setActiveTackleZone(player, activeTackleZone) {}
    function setPlayerCondition(player, playerCondition) {}
    function setPosition(player, position) {}
    function setIsHoldingBall(player, isHoldingBall) {}
    function setRedCard(player, redCard) {}
 
    function adjustTeam(player, team) {}
    function adjustPosition(player, position) {}
    function adjustRemainingBe(player, be) {}
    function adjustMinHeadcount(player, minHeadcount) {}
    function getGe(player, ge) {}
    function getRs(player, rs) {}
    function getSt(player, st) {}
    function getBe(player, be) {}
    function getRemainingBe(player, remainingBe) {}
    function getPlayerCondition(player, playerCondition) {}
}
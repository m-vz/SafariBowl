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
			"Sneaky when fouling.",
			"Easily hurt.",
            "Can jump over enemies using his tail.",
			"Just pushed on 'defender stumbles'.",
			"Very good at passing through tackle",
			"zones, ignores helping tackle zones",
            "of other enemies, when dodging."
		];
		return Java.to(lines, Java.type("java.lang.String[]"));
	}
	
	var alreadyDodgedKey = "hasAlreadyDodged";
	var cheatedKey = "cheated";
	var API_FIELD_FUNCTION_NAME = "apiField";
	var FAILD_NO_EMPTY_SQUARE_TO_LEAP_TO = "NO EMPTY SQUARE TO LEAP TO";
	var FAILD_NOT_ENOUGH_REMAINING_BE = "NOT ENOUGH REMAINING BE";
	var ENEMY_FOULED = "YOU HAVE BEATEN UP YOUR ENEMY. YOUR ENEMY IS ";
	var SBProtocolCommand = Java.type("network.SBProtocolCommand");
	var SBProtocolMessage = Java.type("network.SBProtocolMessage");
	
	function apiField(player, x, y, userIndex){
		player.getMatch().setGamePhase(3);
		player.invokeSetPosition(x, y);
		player.invokeCountDownRemainingBe(2);
		if(player.getRule(0).geTest(1)){
		}else{
			var message = new SBProtocolMessage(player.getTeam().getCoach().getUID(), SBProtocolCommand.EVENT, " ");
			player.getRule(1).playerDown(message, player, player.getRule(1).YOU_SUCK);
			player.invokeSetRemainingBe(0);
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
        return 7;
    }
    function adjustMaxHeadcount(player, maxHeadcount) {
        return 1;
    }
    function setRules(player, ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch) {
    	player.addSpecialStat(alreadyDodgedKey, "false");
    	player.addSpecialStat(cheatedKey, "false");
    	var rulesArray = [ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch];
 
        var RuleBlock = Java.type("gameLogic.rules.RuleBlock");
        var RuleBlockDodgeStuntyDirtyPlayer = Java.extend(RuleBlock);
        var ruleBlockDodgeStuntyDirtyPlayer = new RuleBlockDodgeStuntyDirtyPlayer(rulesArray[1].getActor()) {
        	beingBlockedDefenderStumbles: function(message, attacker, firstThrowModifier, injuryRollModifier){
        		var _super_ = Java.super(ruleBlockDodgeStuntyDirtyPlayer);
        		_super_.beingBlockedPushed(message, attacker, player.getPosition());
        	},
        	beatHim: function(defender, message){
        		var _super_ = Java.super(ruleBlockDodgeStuntyDirtyPlayer);
        		if(player.getTeam().getFoul()){
        			armorRollModifier = player.getTeam().getTacklezones(defender.getPos()) -1;
        			armorRollModifier += defender.getTeam().getTacklezones(player.getPos());
        			defender.getRule(1).playerDown(message, defender, ENEMY_FOULED, armorRollModifier + 1, 1);		
        			player.getTeam().setFoul(false);
        			_super_.refereeTriesToKeepSurvey(message);
        		}
        		else{
        			returnFailureMessage(message, SBProtocolMessage.FAILD_NO_FOUL_LEFT);
        		}
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
        rulesArray[1] = ruleBlockDodgeStuntyDirtyPlayer;
        
        var RuleMove = Java.type("gameLogic.rules.RuleMove");
        var RuleMoveDodgeStuntyCheater = Java.extend(RuleMove);
        var ruleMoveDodgeStuntyCheater = new RuleMoveDodgeStuntyCheater(rulesArray[0].getActor()){
        	tackleTest: function(message, problems, i, path){
        		var _super_ = Java.super(ruleMoveDodgeStuntyCheater);
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
        	}
        };
        rulesArray[0] = ruleMoveDodgeStuntyCheater;
        
        var RuleThrow = Java.type("gameLogic.rules.RuleThrow");
        var RuleThrowStuntyLongTail = Java.extend(RuleThrow);
        var ruleThrowStuntyLongTail = new RuleThrowStuntyLongTail(rulesArray[3].getActor()){
        	findThrowModificator: function(problems, distance){
        		var _super_ = Java.super(ruleThrowStuntyLongTail);
        		_super_.findThrowModificator(problems - 1, distance);
        	},
        	intercept: function(thrower, teamIndex, playerIndex, userIndex){
				player.getMatch().setGamePhase(3);
				var mod = -1;
				mod -= player.getMatch().getOpposingTeam(player.getMatch().getTeam(teamIndex)).getTacklezones(player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getPos());
				if(player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getRule(3).geTest(mod)){
					player.invokeSetIsHoldingBall(false);
					player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).invokeSetIsHoldingBall(true);
					player.getMatch().getPitch().adjustBallPos(palyer.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getPos());
					var actingUserIndex = -1;
					if(userIndex == 0){
						actingUserIndex = 1;
					}else if(userIndex == 1){
						actingUserIndex = 0;
					}
					sendMessageShowMe(player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).toString(), "Successfully intercepted!");
					player.getMatch().endTurn(actingUserIndex);
				}else{
					thrower.getRule(3).throwBall(player.getMatch().getCurrentMessageWaitingForAnswer(), player.getMatch().getCurrentDefenderFieldWaitingForAnser(), player.getMatch().getCurrentModificatorWaitingForAnser());
				}
			}
        };
        rulesArray[3] = ruleThrowStuntyLongTail;

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
	    
	    var SpecialRuleLeapLongTail = Java.extend(SpecialRule);
    	var specialRuleLeapLongTail = new SpecialRuleLeapLongTail(player, "Leap") {
    		apply: function(message){
    			var _super_ = Java.super(specialRuleLeapLongTail);
    			_super_.checkForMovingPlayer(player);
    			var PlayerCondition = Java.type("gameLogic.PlayerCondition");
    			if(player.invokeGetPlayerCondition() != PlayerCondition.FINE){
	    			_super_.returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_IS_NOT_IN_A_GOOD_CONDITION);
	    			return;
	    		}
    			if(player.invokeGetRemainingBe() > 1){
    				var parameterArray = [];
        			parameterArray[0] = SBProtocolMessage.EVENT_API_FIELD;
        			parameterArray[1] = API_FIELD_FUNCTION_NAME;
        			var fieldCounter = 0;
        			for(var j = -2; j < 3; j++){
        				for(var i = -2; i < 3; i++){
        					var posX = player.getPos().x+i;
        					var posY = player.getPos().y+j;
        					if(player.getMatch().getPitch().isOnField(posX, posY)){
        						if(player.getMatch().getPitch().getFields()[posX][posY].getPlayer() == null && !(i==0 && j==0)){
            						parameterArray[2*fieldCounter+2] = posX;
            						parameterArray[2*fieldCounter+3] = posY;
            						fieldCounter++;
            					}
        					}
        				}
        			}
        			if(fieldCounter > 0){
        				player.setSpecialStat(cheatedKey, "true");
        				player.getMatch().setGamePhase(5);
            			player.getMatch().setCurrentActorWaitingForAnswer(player);
            			_super_.sendMessage(message, SBProtocolCommand.EVENT, parameterArray);
        			}else{
        				_super_.sendMessageShowMe(player.getTeam().getCoach(), player.toString(), "There is no Square I can leap to!");
        				_super_.returnFailureMessage(message, FAILD_NO_EMPTY_SQUARE_TO_LEAP_TO);
        			}
    			}else{
    				_super_.sendMessageShowMe(player.getTeam().getCoach(), player.toString(), "I'm exhausted!");
    				_super_.returnFailureMessage(message, FAILD_NOT_ENOUGH_REMAINING_BE);
    			}
    		}
    	};
	   	specialRulesArray[0] = specialRuleLeapLongTail;
	
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
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
			"Whirls across the field with another monkey",
			"blocking everyone in his path."
		];
		return Java.to(lines, Java.type("java.lang.String[]"));
	}
		
	var cheatedKey = "cheated";
	var noHandsString = "Sorry, but I have no hands left!";
	var ONLY_TORNADO = "SRY, BUT I'M SO HYPERACTIVE, THAT I CAN ONLY USE MY SPECIAL ABILITY TORNADO";
	var API_FIELD_FUNCTION_NAME = "apiField";
	var ENEMY_DOWN = "YOUR ENEMY IS ";
	var YOU_SUCK = "YOU ARE ";
	var FAILD_PLAYER_IS_NOT_IN_A_GOOD_CONDITION = "PLAYER IS NOT IN A GOOD CONDITION";
	var SBProtocolCommand = Java.type("network.SBProtocolCommand");
	var SBProtocolMessage = Java.type("network.SBProtocolMessage");
	
	function apiField(player, x, y, userIndex){
		var directionThrow = player.getMatch().d3.throwDie();
		var newX = x;
		var newY = y;
		if(x == player.getPos().x){
			newX = directionThrow - 2 + x;
		}else if(y == player.getPos().y){
			newY = directionThrow - 2 + y;
		}
		if(player.getMatch().getPitch().isOnField(newX, newY)){
			if(player.getMatch().getPitch().getFields()[newX][newY].getPlayer() == null){
				player.invokeSetPosition(newX, newY);
				if(player.getMatch().getPitch().getBallPos().x == newX && player.getMatch().getPitch().getBallPos().y == newY){
					player.getRule(4).scatterBallAround(userIndex);
				}
				endMove(player);
			}else{
				var message = new SBProtocolMessage(player.getTeam().getCoach().getUID(), SBProtocolCommand.EVENT, " ");		
				blockFromMove(player, message, player.getMatch().getPitch().getFields()[newX][newY].getPlayer());
			}
		}else{
			player.getRule(2).crowdBeatsUpPlayer(player);
			player.getMatch().setGamePhase(3);
		}
	}
	function endMove(player){
		player.getMatch().setGamePhase(5);
		player.invokeCountDownRemainingBe(1);
		var PlayerCondition = Java.type("gameLogic.PlayerCondition");
		if(player.invokeGetRemainingBe() > 0 && player.invokeGetPlayerCondition() == PlayerCondition.FINE){
			var message = new SBProtocolMessage(player.getTeam().getCoach().getUID(), SBProtocolCommand.EVENT, " ");
			player.getSpecialRule(0).apply(message);
		}else{
			player.getMatch().setGamePhase(3);
		}
	}
	function playerDownInAttack(message, p, s){
		armorRoll = p.getMatch().d6.throwDie() + p.getMatch().d6.throwDie();
		s = injuryRollInAttack(p, 0);
		p.getRule(1).sendMessageShowMe(p.toString(), "I am " + s + "!");
		p.getRule(1).returnSuccessMessage(message, s);
		var teamIndex = -1;
		if(p.getTeam().equals(p.getMatch().getTeam(0))){
			teamIndex = 0;
		}else if(p.getTeam().equals(p.getMatch().getTeam(1))){
			teamIndex = 1;
		}else{
			return;
		}
		p.getMatch().endTurn(teamIndex);
	}
	function injuryRollInAttack(player, modifier){
		s = "";
		injuryRoll = player.getMatch().d6.throwDie() + player.getMatch().d6.throwDie() + 1;
		var PlayerCondition = Java.type("gameLogic.PlayerCondition");
		if(injuryRoll + modifier < 10){
			player.invokeSetPlayerCondition(PlayerCondition.KO);
			s += "KO";
		}else{
			casultyRoll = player.getMatch().d6.throwDie() * 10 + player.getMatch().d8.throwDie(); 
			if(casultyRoll < 61){
				player.invokeSetPlayerCondition(PlayerCondition.INJURED);
				s += "INJURED";
			}else{
				player.invokeSetPlayerCondition(PlayerCondition.DEAD);
				s += "DEAD";
			}
			player.getMatch().addCasualty(player);
		}
		player.invokeClearPosition();
		return s;
	}
	function blockFromMove(player, message, defender){
		player.setSpecialStat(cheatedKey, "true");
		var PlayerCondition = Java.type("gameLogic.PlayerCondition");
		playerIsFine = (player.getMatch().getPitch().isOnField(player.getPos()) && player.invokeGetPlayerCondition() == PlayerCondition.FINE);
		playerHasRemainingBe = (player.invokeGetRemainingBe() > 0);
		playerWantsToBlitz = (player.invokeGetRemainingBe() != player.invokeGetBe());
		if(playerIsFine && playerHasRemainingBe){
			// Set fields for defender and message so they are available in attackerDown() and bothDown()
			this.message = message;
			if (player.getMatch().getPitch().isAdjacent(player.getPosition(), defender.getPosition())){
				if(defender.invokeGetPlayerCondition() == PlayerCondition.FINE){
					player.getRule(1).throwDice(message, defender);
				}else{
					player.getRule(1).beatHim(defender, message, true);
					player.getRule(2).apply(message, defender, defender.getPosition(), player.getTeam().getCoach().getUID(), player, defender.getPos());
				}
			} else player.getRule(1).returnFailureMessage(message, SBProtocolMessage.FAILD_BLOCKING_NOT_POSSIBLE);
		} else player.getRule(1).returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_CANNOT_TAKE_ACTION);
	}
	function followWithoutQuestion(player, defenderField, message){       	
		player.invokeSetPosition(defenderField);
		player.getRule(2).returnSuccessMessage(message, SBProtocolMessage.WORKD_FOLLOWED);
		endMove(player);
	}
	
    function setPrice(player, price) {
        return 70000;
    }
    function setGe(player, ge) {
        return 3;
    }
    function setRs(player, rs) {
        return 7;
    }
    function setSt(player, st) {
        return 7;
    }
    function setBe(player, be) {
        return 3;
    }
    function adjustMaxHeadcount(player, maxHeadcount) {
        return 1;
    }
    function setRules(player, ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch) {
    	player.addSpecialStat(cheatedKey, "false");
    	var rulesArray = [ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch];
 
        var RuleBlock = Java.type("gameLogic.rules.RuleBlock");
        var RuleBlockStuntyCheaterTornado = Java.extend(RuleBlock);
        var ruleBlockStuntyCheaterTornado = new RuleBlockStuntyCheaterTornado(rulesArray[1].getActor()) {
        	apply: function(message, defender){
        		var _super_ = Java.super(ruleBlockStuntyCheaterTornado);
        		_super_.sendMessageShowMe(player.getTeam().getCoach(), player.toString(), "I can only use my tornado!");
        		_super_.returnFailureMessage(message, ONLY_TORNADO);
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
        	},
        	beatHim: function(defender, message, playerWantsToBlitz){
        		var PlayerCondition = Java.type("gameLogic.PlayerCondition");
        		defenderCondition = defender.invokeGetPlayerCondition();
        		defenderPosition = defender.getPosition();
        		defender.getRule(1).playerDown(message, defender, ENEMY_DOWN);
        		if(defenderCondition == PlayerCondition.STUNNED && defender.invokeGetPlayerCondition() == PlayerCondition.PRONE){
        			defender.invokeSetPlayerCondition(PlayerCondition.STUNNED);
        		}
        		player.getRule(2).apply(message, defender, defenderPosition, player.getTeam().getCoach().getUID(), player, defenderPosition.getPos());
        	},
        	attackerDown: function(message) {
        		playerDownInAttack(message, player, YOU_SUCK);
        		player.getMatch().setGamePhase(3);
        		player.getMatch().clearCurrentHighlitedFields();
        		var _super_ = Java.super(ruleBlockStuntyCheaterTornado);
				_super_.clearHighlightFields();
        		return true;
        	},
        	bothDown: function(message, defender) {
        		defender.getRule(1).beingBlockedBothDown(message, 0, 0);
        		playerDownInAttack(message, player, YOU_SUCK);
        		player.getMatch().clearCurrentHighlitedFields();
        		var _super_ = Java.super(ruleBlockStuntyCheaterTornado);
				_super_.clearHighlightFields();
        		player.getMatch().setGamePhase(3);
        		return true;
        	}
        };
        rulesArray[1] = ruleBlockStuntyCheaterTornado;
        
        var RulePush = Java.type("gameLogic.rules.RulePush");
        var RulePushTornado = Java.extend(RulePush);
        var rulePushTornado = new RulePushTornado(rulesArray[2].getActor()){
        	backUp: function(defenderField, message, playerBackingUp){
        		var _super_ = Java.super(rulePushTornado);
        		player.getRule(1).clearHighlightFields();
        		followWithoutQuestion(player, defenderField, message);
        	}
        };
        rulesArray[2] = rulePushTornado;
        
        var RuleMove = Java.type("gameLogic.rules.RuleMove");
        var RuleMoveNoHandsTornado = Java.extend(RuleMove);
        var ruleMoveNoHandsTornado = new RuleMoveNoHandsTornado(rulesArray[0].getActor()){
        	apply: function(message, path){
        		var _super_ = Java.super(ruleMoveNoHandsTornado);
        		_super_.sendMessageShowMe(player.getTeam().getCoach(), player.toString(), "I can only use my tornado!");
        		_super_.returnFailureMessage(message, ONLY_TORNADO);
        	},        
        	tryToPickUpBall: function(message, i, path){
        		var _super_ = Java.super(ruleMoveNoHandsTornado);
        		return _super_.faildToPickUpBall(message);
        	}
        };
        rulesArray[0] = ruleMoveNoHandsTornado;
        
		var RuleThrow = Java.type("gameLogic.rules.RuleThrow");
		var RuleThrowStuntyNoHands = Java.extend(RuleThrow);
		var ruleThrowStuntyNoHands = new RuleThrowStuntyNoHands(rulesArray[3].getActor()){
			findThrowModificator: function(problems, distance){
				var _super_ = Java.super(ruleThrowStuntyNoHands);
				_super_.findThrowModificator(problems - 1, distance);
			},
			apply: function(message, destination){
				var _super_ = Java.super(ruleThrowStuntyNoHands);
				_super_.sendMessageShowMe(player.getTeam().getCoach(), player.toString(), "I can only use my tornado!");
				_super_.sendMessage(message, SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_SHOW_ME, player.toString(), noHandsString);
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
	    
	    var SpecialRuleTornado = Java.extend(SpecialRule);
    	var specialRuleTornado = new SpecialRuleTornado(player, "Tornado") {
    		apply: function(message){
    			var _super_ = Java.super(specialRuleTornado);
    			_super_.checkForMovingPlayer(player);
    			var PlayerCondition = Java.type("gameLogic.PlayerCondition");
    			if(player.invokeGetPlayerCondition() != PlayerCondition.FINE){
	    			_super_.returnFailureMessage(message, FAILD_PLAYER_IS_NOT_IN_A_GOOD_CONDITION);
	    			return;
	    		}
    			if(player.invokeGetRemainingBe() > 0){
    				var parameterArray = [];
        			parameterArray[0] = SBProtocolMessage.EVENT_API_FIELD;
        			parameterArray[1] = API_FIELD_FUNCTION_NAME;
        			var fieldCounter = 0;
        			for(var j = -1; j < 2; j++){
        				for(var i = -1; i < 2; i++){
        					var posX = player.getPos().x+i;
        					var posY = player.getPos().y+j;
        					if(player.getMatch().getPitch().isOnField(posX, posY)){
        						if((i == 0 && j != 0) || (i != 0 && j == 0)){
            						parameterArray[2*fieldCounter+2] = posX;
            						parameterArray[2*fieldCounter+3] = posY;
            						fieldCounter++;
            					}
        					}
        				}
        			}
        			player.getMatch().setGamePhase(5);
        			player.getMatch().setCurrentActorWaitingForAnswer(player);
        			_super_.sendMessage(message, SBProtocolCommand.EVENT, parameterArray);
    			}else{
    				_super_.returnFailureMessage(message, SBProtocolMessage.FAILD_YOU_ARE_EXHAUSTED);
    			}
    		}
    	};
	   	specialRulesArray[0] = specialRuleTornado;
	
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
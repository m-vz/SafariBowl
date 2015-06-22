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
			"High chance of hurting enemies when blocking.",
			"Can throw players with the attribute 'throwable',",
			"but sometimes crushes them accidentally.",
			"Heals faster than other players.",
			"Stupid, so sometimes she just forgets",
			"what she was about to do. This can be",
			"helped by setting other players to her side."
		];
		return Java.to(lines, Java.type("java.lang.String[]"));
	}

	var checkForStupidKey = "checkForStupid";
	var stupidKey = "isStupid";
	var stupidString = "HHNG";
	var YOU_SUCK = "YOU ARE ";
	var SBProtocolCommand = Java.type("network.SBProtocolCommand");
	var SBProtocolMessage = Java.type("network.SBProtocolMessage");
	
	var ENEMY_DOWN = "YOUR ENEMY IS ";
	var YOU_SUCK = "YOUR ARE ";
	var FAILD_NO_THROWABLE_PLAYER_NEXT_TO_ME = "NO THROWABLE PLAYER NEXT TO ME";
	var FAILD_CLUMSY = "THE SEACOW WAS TOO CLUMSY, ROLLED ON THE TEAMMATE AND CRUMPLED HIM TO DEATH";
	var throwableKey = "throwable";
	var API_CHOICE_FUNCTION_NAME = "apiChoice";
	var API_AIM_FUNCTION_NAME = "apiAim";
	function apiChoice(player, teamIndex, playerToBeThrownIndex, userIndex){
		var playerToBeThrown = player.getTeam().getPlayers().get(playerToBeThrownIndex);
		player.getMatch().setCurrentDefenderWaitingForAnswer(playerToBeThrown);
		var playerIndex = player.findPlayerIndex();
		if(!(player.getMatch().d6.throwDie() == 1 && player.getMatch().d6.throwDie() == 1)){
			var parameterArray = [];
			parameterArray[0] = SBProtocolMessage.EVENT_API_AIM;
			parameterArray[1] = API_AIM_FUNCTION_NAME;
			parameterArray[2] = playerIndex;
			parameterArray[3] = 8; //MaxRange
			player.getMatch().sendMessage(player.getMatch().getUser(userIndex), SBProtocolCommand.EVENT, parameterArray);
		}else{
			var PlayerCondition = Java.type("gameLogic.PlayerCondition");
			var Pitch = Java.type("gameLogic.Pitch");
			playerToBeThrown.invokeSetPlayerCondition(PlayerCondition.DEAD);
			playerToBeThrown.invokeSetPosition(Pitch.THE_VOID);
			player.sendMessageShowMe(player.toString(), "Oups, I just squeezed the poor puffin to death.");
			var parameterArray = [];
			parameterArray[0] = FAILD_CLUMSY;
			player.getMatch().sendMessage(player.getMatch().getUser(userIndex), SBProtocolCommand.EVENT, parameterArray);
			player.getMatch().setGamePhase(3);
		}
		player.invokeSetRemainingBe(0);
	}
	function apiAim(player, destX, destY, userIndex){
		var x = destX;
		var y = destY;
		for(var i = 0; i < 3; i++){
			scatterRoll = player.getMatch().scatter();
			x += scatterRoll.x;
			y += scatterRoll.y;
		}
		if(player.getMatch().getPitch().isOnField(x, y)){
			playerLanding(player.getMatch().getCurrentDefenderWaitingForAnswer(), userIndex, x, y, false);
		}else{
			player.getRule(2).crowdBeatsUpPlayer(player.getMatch().getCurrentDefenderWaitingForAnswer());
		}
		player.getMatch().setGamePhase(3);
	}
	function playerLanding(player, userIndex, x, y, willFailForSure){
		if(player.getMatch().getPitch().getFields()[x][y].getPlayer() == null){
			player.invokeSetPosition(x, y);
			if(willFailForSure){
				var message = new SBProtocolMessage(player.getTeam().getCoach().getUID(), SBProtocolCommand.EVENT, " ");
				player.getRule(1).playerDown(message, player, YOU_SUCK);
				if(player.getMatch().getPitch().getBallPos() == player.getPos()){
					player.getRule(4).scatterBallAround(userIndex);
				}
			}else{
				var enemyIndex = -1;
				if(userIndex == 0){
					enemyIndex = 1;
				}else if(userIndex == 1){
					enemyIndex = 0;
				}
				var mod = -(player.getMatch().getTeam(enemyIndex).getTacklezones(x, y));
				if(!(player.getRule(0).geTest(mod))){
					var message = new SBProtocolMessage(player.getTeam().getCoach().getUID(), SBProtocolCommand.EVENT, " ");
					player.getRule(1).playerDown(message, player, YOU_SUCK);
					if(player.getMatch().getPitch().getBallPos() == player.getPos()){
						player.getRule(4).scatterBallAround(userIndex);
					}
				}
				if(!(player.getRule(0).geTest(mod))){
					if(player.getMatch().getPitch().getBallPos() == player.getPos()){
						player.getRule(4).scatterBallAround(userIndex);
					}
				}
			}
		}else{
			var PlayerCondition = Java.type("gameLogic.PlayerCondition");
			var message = new SBProtocolMessage(player.getTeam().getCoach().getUID(), SBProtocolCommand.EVENT, " ");
			playerCondition = player.getMatch().getPitch().getFields()[x][y].getPlayer().invokeGetPlayerCondition();
			defender = player.getMatch().getPitch().getFields()[x][y].getPlayer();
			player.getMatch().getPitch().getFields()[x][y].getPlayer().getRule(1).playerDown(message, player.getMatch().getPitch().getFields()[x][y].getPlayer(), ENEMY_DOWN);
			if(playerCondition.equals(PlayerCondition.STUNNED) && defender.invokeGetPlayerCondition().equals(PlayerCondition.PRONE)){
				defender.invokeSetPlayerCondition(PlayerCondition.STUNNED);
			}
			var scatterRoll = player.getMatch().scatter();
			var newX = x + scatterRoll.x;
			var newY = y + scatterRoll.y;
			if(player.getMatch().getPitch().isOnField(newX, newY)){
				playerLanding(player, userIndex, newX, newY, true);
			}else{
				player.getRule(2).crowdBeatsUpPlayer(player);
			}
		}
	}
	
	function checkForStupid(player){
		var difficulty = 4;
		if(player.getTeam().getTacklezones(player.getPos()) > 0){
			difficulty = 2;
		}
		if(player.getSpecialStat(stupidKey) == "false"){
			if(player.getTeam().getMatch().d6.throwDie() < difficulty){
				player.setSpecialStat(stupidKey, "true");
				player.updateActiveTackleZone();
			}
		}
	}

    function setPrice(player, price) {
        return 110000;
    }
    function setGe(player, ge) {
        return 1;
    }
    function setRs(player, rs) {
        return 9;
    }
    function setSt(player, st) {
        return 5;
    }
    function setBe(player, be) {
        return 4;
    }
    function adjustMaxHeadcount(player, maxHeadcount) {
        return 1;
    }
    function setRules(player, ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch) {
    	var rulesArray = [ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch];
    	player.addSpecialStat(stupidKey, "false");
    	
    	var RuleMove = Java.type("gameLogic.rules.RuleMove");
    	var RuleMoveStupid = Java.extend(RuleMove);
    	var ruleMoveStupid = new RuleMoveStupid(rulesArray[0].getActor()){
    		apply: function(message, path){
    			var _super_ = Java.super(ruleMoveStupid);
    			var PlayerCondition = Java.type("gameLogic.PlayerCondition");
    			var pathArray = Java.from(path);
    			if(pathArray.length < 2){
    				if(player.invokeGetPlayerCondition() == PlayerCondition.FINE){
    					return;
    				}
    			}
    			if(player.getTeam().getMovingPlayer() != player){
    				player.invokeFunctionByName(checkForStupidKey, player);
    			}
    			if(player.getSpecialStat(stupidKey) == "true"){
    				_super_.sendMessageShowMe(player.toString(), stupidString);
    			}else{
    				_super_.apply(message, path);
    			}	
    		}
    	};
    	rulesArray[0] = ruleMoveStupid;

    	var RuleBlock = Java.type("gameLogic.rules.RuleBlock");
    	var RuleBlockStupidMightyBlowRegeneration = Java.extend(RuleBlock);
    	var ruleBlockStupidMightyBlowRegeneration = new RuleBlockStupidMightyBlowRegeneration(rulesArray[1].getActor()) {
    		apply: function(message, defender){
    			var _super_ = Java.super(ruleBlockStupidMightyBlowRegeneration);
    			if(player.getTeam().getMovingPlayer() != player){
    				player.invokeFunctionByName(checkForStupidKey, player);
    			}
    			if(player.getSpecialStat(stupidKey) == "true"){
    				_super_.sendMessageShowMe(player.toString(), stupidString);
    			}else{
    				_super_.apply(message, defender);
    			}
    		},
    		defenderDown: function(message, defender) {
    			var PitchField = Java.type("gameLogic.PitchField");
    			var defenderField = new PitchField(defender.getPos());
    			defender.getRule(1).beingBlockedDefenderDown(message, player, defender, 1, 1);
    			return true;
    		},
    		defenderStumbles: function(message, defender) {
    			var PitchField = Java.type("gameLogic.PitchField");
    			var defenderField = new PitchField(defender.getPos());
    			defender.getRule(1).beingBlockedDefenderStumbles(message, player, defender, 1, 1);
    			return true;
    		},
    		bothDown: function(message, defender) {
    			var _super_ = Java.super(ruleBlockStupidMightyBlowRegeneration);
    			defender.getRule(1).beingBlockedBothDown(message, 1, 1);
    			_super_.playerDown(message, player, YOU_SUCK, 0, 0);
				_super_.clearHighlightFields();
    			return true;
    		},
    		injuryRoll: function(modifier){
    			s = "";
        		injuryRoll = player.getMatch().d6.throwDie() + player.getMatch().d6.throwDie();
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
        			if(player.getMatch().d6.throwDie() > 3){
        				player.invokeSetPlayerCondition(PlayerCondition.FINE);
        				s += ", BUT REGENERATED";
        			}
        			player.invokeClearPosition();
        		}
        		return s;
    		}
    	};
    	rulesArray[1] = ruleBlockStupidMightyBlowRegeneration;
    	
    	var RuleThrow = Java.type("gameLogic.rules.RuleThrow");
    	var RuleThrowStupid = Java.extend(RuleThrow);
    	var ruleThrowStupid = new RuleThrowStupid(rulesArray[3].getActor()){
    		apply: function(message, destination){
    			var _super_ = Java.super(ruleThrowStupid);
    			if(player.getTeam().getMovingPlayer() != player){
    				player.invokeFunctionByName(checkForStupidKey, player);
    			}
    			if(player.getSpecialStat(stupidKey) == "true"){
    				_super_.sendMessageShowMe(player.toString(), stupidString);
    			}else{	
    				_super_.apply(message, destination);
    			}
    		}
    	};
    	rulesArray[3] = ruleThrowStupid;

    	var RuleCatch = Java.type("gameLogic.rules.RuleCatch");
    	var RuleCatchStupid = Java.extend(RuleCatch);
    	var ruleCatchStupid = new RuleCatchStupid(rulesArray[4].getActor()){
    		apply: function(successfulThrow){
    			var _super_ = Java.super(ruleCatchStupid);
    			if(player.getSpecialStat(stupidKey) == "true"){
    				var actingUserIndex = -1;
    				if(player.getTeam() == player.getMatch().getTeam(0)){
    					actingUserIndex = 0;
    				}else if(player.getTeam() == player.getMatch().getTeam(1)){
    					actingUserIndex = 1;
    				}else{
    					return;
    				}
    				_super_.scatterBallAround(actingUserIndex);
    				_super_.sendMessageShowMe(player.toString(), stupidString);
    			}else{
    				_super_.apply(successfulThrow);
    			}
    		}
    	};
    	rulesArray[4] = ruleCatchStupid;

    	var rulesToReturn = Java.to(rulesArray, Java.type("gameLogic.rules.Rule[]"));
    	return rulesToReturn;
    }
    function eventHappened(player, eventString){
    	var SBProtocolMessage = Java.type("network.SBProtocolMessage");
    	if(eventString == SBProtocolMessage.EVENT_YOUR_TURN || eventString == SBProtocolMessage.EVENT_ENDED_TURN){
    		var difficulty = 3;
    		if(player.getTeam().getTacklezones(player.getPos()) > 0){
    			difficulty = 1;
    		}
    		if(player.getTeam().getMatch().d6.throwDie() > difficulty){
				player.setSpecialStat(stupidKey, "false");
				player.updateActiveTackleZone();
    		}
    	}
    }
    function setRemainingBe(player, remainingBe) {}
    function setSpecialRules(player) {
    	var SpecialRule = Java.type("gameLogic.rules.SpecialRule");
	    var specialRulesArray = [];
	    
	    var SpecialRuleThrowTeammate = Java.extend(SpecialRule);
    	var specialRuleThrowTeammate = new SpecialRuleThrowTeammate(player, "Throw Teammate") {
    		apply: function(message){
    			var _super_ = Java.super(specialRuleThrowTeammate);
    			var PlayerCondition = Java.type("gameLogic.PlayerCondition");
	    		if(player.invokeGetRemainingBe() == 0 && !(player.getTeam().getMovingPlayer() == player)){
	    			_super_.returnFailureMessage(message, SBProtocolMessage.FAILD_YOU_ARE_EXHAUSTED);
	    			return;
	    		}
	    		if(player.invokeGetPlayerCondition() != PlayerCondition.FINE){
	    			_super_.returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_IS_NOT_IN_A_GOOD_CONDITION);
	    			return;
	    		}
    			_super_.checkForMovingPlayer(player);
    			if(player.getTeam().getMovingPlayer() != player){
    				player.invokeFunctionByName(checkForStupidKey, player);
    			}
    			if(player.getSpecialStat(stupidKey) == "true"){
    				_super_.sendMessageShowMe(player.toString(), stupidString);
    			}else{	
	    			if(player.invokeGetRemainingBe() > 0){
	    				var parameterArray = [];
	        			parameterArray[0] = SBProtocolMessage.EVENT_API_CHOICE;
	        			parameterArray[1] = API_CHOICE_FUNCTION_NAME;
	        			var playerCounter = 0;
	        			var PitchField = Java.type("gameLogic.PitchField");
	        			var neighboursArray = Java.from(player.getMatch().getPitch().getNeighbours(player.getPos()));
	        			for(var i = 0; i < neighboursArray.length; i++){
	        				var field = neighboursArray[i];
	        				if(field.getPlayer() != null){
	        					if(player.getTeam() == field.getPlayer().getTeam() && field.getPlayer().invokeGetPlayerCondition().equals(PlayerCondition.FINE)){
	        						var throwable = false;
	        						throwable = field.getPlayer().getSpecialStat(throwableKey);
	        						if(throwable == "true"){
	        							var teamIndex = -1;
	                					if(player.getTeam() == field.getPlayer().getMatch().getTeam(0)){
	                						teamIndex = 0;
	                					}else if(player.getTeam() == field.getPlayer().getMatch().getTeam(1)){
	                						teamIndex = 1;
	                					}
	                					parameterArray[2*playerCounter+2] = teamIndex;
	                					parameterArray[2*playerCounter+3] = field.getPlayer().getId() - 1;
	                					playerCounter++;
	        						}
	        					}
	        				}
	        			}
	        			if(playerCounter > 0){
	        				player.getMatch().setGamePhase(5);
	            			player.getMatch().setCurrentActorWaitingForAnswer(player);
	            			_super_.sendMessage(message, SBProtocolCommand.EVENT, parameterArray);
	        			}else{
	        				_super_.sendMessageShowMe(player.getTeam().getCoach(), player.toString(), "I can't find any throwable players");
	        				_super_.returnFailureMessage(message, FAILD_NO_THROWABLE_PLAYER_NEXT_TO_ME);
	        			}
	    			}else{
	    				_super_.returnFailureMessage(message, SBProtocolMessage.FAILD_YOU_ARE_EXHAUSTED);
	    			}
    			}
    		}
    	};
	   	specialRulesArray[0] = specialRuleThrowTeammate;
	
	   	var specialRulesToReturn = Java.to(specialRulesArray, Java.type("gameLogic.rules.SpecialRule[]"));
	   	return specialRulesToReturn;
    }
    function setTackleZone(player, tackleZone) {}
    function setActiveTackleZone(player, activeTackleZone) {
    	var Vector = Java.type("java.util.Vector");
    	if(player.getSpecialStat(stupidKey) == "true"){
    		return new Vector();
    	}
    }
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

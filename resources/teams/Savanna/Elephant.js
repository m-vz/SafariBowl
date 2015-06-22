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
	
	var checkForStubbornKey = "checkForStubborn";
	var stubbornKey = "isStubborn";
	var stubbornString = "I'm stubborn!";
	var YOU_SUCK = "YOU ARE ";
	var SBProtocolCommand = Java.type("network.SBProtocolCommand");
	var SBProtocolMessage = Java.type("network.SBProtocolMessage");

	function checkForStubborn(player){
		if(player.getSpecialStat(stubbornKey) == "false"){
			if(player.getTeam().getMatch().d6.throwDie() < 2){
				player.setSpecialStat(stubbornKey, "true");
				player.updateActiveTackleZone();
			}
		}
	}

	function setDescriptionLines(player, descriptionLines) {
		var lines = [
			"High chance of hurting enemies when blocking.",
			"Very tough and not easily injured.",
			"Stubborn, so sometimes he'll refuse to do",
            "anything."
		];
		return Java.to(lines, Java.type("java.lang.String[]"));
	}
	
    function setPrice(player, price) {
        return 140000;
    }
    function setGe(player, ge) {
        return 2;
    }
    function setRs(player, rs) {
        return 9;
    }
    function setSt(player, st) {
        return 5;
    }
    function setBe(player, be) {
        return 5;
    }
    function setRules(player, ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch) {
    	player.addSpecialStat(stubbornKey, "false");
        var rulesArray = [ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch];
 
        var RuleMove = Java.type("gameLogic.rules.RuleMove");
        var RuleMoveStubborn = Java.extend(RuleMove);
        var ruleMoveStubborn = new RuleMoveStubborn(rulesArray[0].getActor()) {
        	apply: function(message, path){
        		var _super_ = Java.super(ruleMoveStubborn);
        		var PlayerCondition = Java.type("gameLogic.PlayerCondition");
    			var pathArray = Java.from(path);
    			if(pathArray.length < 2){
    				if(player.invokeGetPlayerCondition() == PlayerCondition.FINE){
    					return;
    				}
    			}
        		if(player.getTeam().getMovingPlayer() != player){
        			player.invokeFunctionByName(checkForStubbornKey, player);
        		}
        		if(player.getSpecialStat(stubbornKey) == "true"){
        			_super_.sendMessageShowMe(player.toString(), stubbornString);
        		}else{
        			_super_.apply(message, path);
        		}
		
        	}
        };
        rulesArray[0] = ruleMoveStubborn;

        var RuleBlock = Java.type("gameLogic.rules.RuleBlock");
        var RuleBlockStubbornMightyBlowThickSkull = Java.extend(RuleBlock);
        var ruleBlockStubbornMightyBlowThickSkull = new RuleBlockStubbornMightyBlowThickSkull(rulesArray[1].getActor()) {
        	apply: function(message, defender){
        		var _super_ = Java.super(ruleBlockStubbornMightyBlowThickSkull);
        		if(player.getTeam().getMovingPlayer() != player){
        			player.invokeFunctionByName(checkForStubbornKey, player);
        		}
        		if(player.getSpecialStat(stubbornKey) == "true"){
        			_super_.sendMessageShowMe(player.toString(), stubbornString);
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
        		var _super_ = Java.super(ruleBlockStubbornMightyBlowThickSkull);
        		defender.getRule(1).beingBlockedBothDown(message, 1, 1);
        		_super_.playerDown(message, player, YOU_SUCK, 0, 0);
				_super_.clearHighlightFields();
        		return true;
        	},
        	injuryRoll: function(modifier){
        		s = "";
        		injuryRoll = player.getMatch().d6.throwDie() + player.getMatch().d6.throwDie();
        		var PlayerCondition = Java.type("gameLogic.PlayerCondition");
        		if(injuryRoll + modifier < 9){
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
        rulesArray[1] = ruleBlockStubbornMightyBlowThickSkull;
        
        var RuleThrow = Java.type("gameLogic.rules.RuleThrow");
        var RuleThrowStubborn = Java.extend(RuleThrow);
        var ruleThrowStubborn = new RuleThrowStubborn(rulesArray[3].getActor()) {
        	apply: function(message, destination){
        		var _super_ = Java.super(ruleThrowStubborn);
        		if(player.getTeam().getMovingPlayer() != player){
        			player.invokeFunctionByName(checkForStubbornKey, player);
        		}
        		if(player.getSpecialStat(stubbornKey) == "true"){
        			_super_.sendMessage(message, SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_SHOW_ME, player.toString() + " of " + player.getTeam().toString(), stubbornString);
        		}else{
        			_super_.apply(message, destination);
        		}
        	}
        };
        rulesArray[3] = ruleThrowStubborn;        

        var RuleCatch = Java.type("gameLogic.rules.RuleCatch");
        var RuleCatchStubborn = Java.extend(RuleCatch);
        var ruleCatchStubborn = new RuleCatchStubborn(rulesArray[4].getActor()) {
        	apply: function(successfulThrow){
        		var _super_ = Java.super(ruleCatchStubborn);
        		if(player.getSpecialStat(stubbornKey) == "true"){
        			var actingUserIndex = -1;
        			if(player.getTeam() == player.getMatch().getTeam(0)){
        				actingUserIndex = 0;
        			}else if(player.getTeam() == player.getMatch().getTeam(1)){
        				actingUserIndex = 1;
        			}else{
        				return;
        			}
        			_super_.scatterBallAround(actingUserIndex);
        			_super_.sendMessageShowMe(player.toString(), stubbornString);
        		}else{
        			_super_.apply(successfulThrow);
        		}
        	}
        };
        rulesArray[4] = ruleCatchStubborn;        

        var rulesToReturn = Java.to(rulesArray, Java.type("gameLogic.rules.Rule[]"));
        return rulesToReturn;
    }
    function setSpecialRules(player) {}
    function setActiveTackleZone(player, activeTackleZone) {
    	var Vector = Java.type("java.util.Vector");
    	if(player.getSpecialStat(stubbornKey) == "true"){
    		return new Vector();
    	}
    }
    function adjustMaxHeadcount(player, maxHeadcount) {
        return 1;
    }
    function eventHappened(player, eventString){
    	if(eventString == SBProtocolMessage.EVENT_YOUR_TURN){
    		if(player.getTeam().getMatch().d6.throwDie() > 1){
    			player.setSpecialStat(stubbornKey, "false");
				player.updateActiveTackleZone();
    		}
    	}
    }
    function setRemainingBe(player, remainingBe) {}
    function setTackleZone(player, tackleZone) {}
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
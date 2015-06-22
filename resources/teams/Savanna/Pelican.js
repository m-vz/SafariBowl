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

	var alreadyDodgedKey = "hasAlreadyDodged";

	function setDescriptionLines(player, descriptionLines) {
		var lines = [
			"Only pushed on 'defender stumbles'.",
			"Very good at passing through tackle zones.",
            "Very good at catching the ball."
		];
		return Java.to(lines, Java.type("java.lang.String[]"));
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
        return 2;
    }
    function setBe(player, be) {
        return 8;
    }
    function adjustMaxHeadcount(player, maxHeadcount) {
        return 4;
    }
    function setRules(player, ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch) {
    	player.addSpecialStat(alreadyDodgedKey, "false");
        var rulesArray = [ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch];
 
        var RuleBlock = Java.type("gameLogic.rules.RuleBlock");
        var RuleBlockDodge = Java.extend(RuleBlock);
        var ruleBlockDodge = new RuleBlockDodge(rulesArray[1].getActor()) {
        	beingBlockedDefenderStumbles: function(message, attacker, firstThrowModifier, injuryRollModifier){
        		var _super_ = Java.super(ruleBlockDodge);
        		_super_.beingBlockedPushed(message, attacker, player.getPosition());
        	}
        };
        rulesArray[1] = ruleBlockDodge;
        
        var RuleMove = Java.type("gameLogic.rules.RuleMove");
        var RuleMoveDodge = Java.extend(RuleMove);
        var ruleMoveDodge = new RuleMoveDodge(rulesArray[0].getActor()){
        	tackleTest: function(message, problems, i, path){
        		var _super_ = Java.super(ruleMoveDodge);
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
        rulesArray[0] = ruleMoveDodge;
        
        var RuleThrow = Java.type("gameLogic.rules.RuleThrow");
        var RuleThrowCatch = Java.extend(RuleThrow);
        var ruleThrowCatch = new RuleThrowCatch(rulesArray[3].getActor()){
        	intercept: function(thrower, teamIndex, playerIndex, userIndex){
				player.getMatch().setGamePhase(3);
				var mod = -2;
				mod -= player.getMatch().getOpposingTeam(player.getMatch().getTeam(teamIndex)).getTacklezones(player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getPos());
				if(player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getRule(3).geTest(mod)){
					player.invokeSetIsHoldingBall(false);
					player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).invokeSetIsHoldingBall(true);
					player.getMatch().getPitch().adjustBallPos(player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getPos());
					var actingUserIndex = -1;
					if(userIndex == 0){
						actingUserIndex = 1;
					}else if(userIndex == 1){
						actingUserIndex = 0;
					}
					player.getMatch().endTurn(actingUserIndex);
				}else{
					if(player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getRule(3).geTest(mod)){
						player.invokeSetIsHoldingBall(false);
						player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).invokeSetIsHoldingBall(true);
						player.getMatch().getPitch().adjustBallPos(player.getMatch().getTeam(teamIndex).getPlayers().get(playerIndex).getPos());
						var actingUserIndex = -1;
						if(userIndex == 0){
							actingUserIndex = 1;
						}else if(userIndex == 1){
							actingUserIndex = 0;
						}
						player.getMatch().endTurn(actingUserIndex);
					}else{
						thrower.getRule(3).throwBall(player.getMatch().getCurrentMessageWaitingForAnswer(), player.getMatch().getCurrentDefenderFieldWaitingForAnser(), player.getMatch().getCurrentModificatorWaitingForAnser());
					}
				}
			}
        }
        rulesArray[3] = ruleThrowCatch;
        
        var RuleCatch = Java.type("gameLogic.rules.RuleCatch");
        var RuleCatchCatch = Java.extend(RuleCatch);
        var ruleCatchCatch = new RuleCatchCatch(rulesArray[4].getActor()){ 
        	catchBall: function(successfulThrow, actingUserIndex){
        		var _super_ = Java.super(ruleCatchCatch);
        		var mod = -(player.getMatch().getOpposingTeam(player.getTeam()).getTacklezones(player.getPos()));
        		if(successfulThrow){
        			mod = mod+1;
        		}
        		var Weather = Java.type("gameLogic.Weather");
        		if(player.getMatch().getWeather() == Weather.POURING_RAIN){
        			mod--;
        		}
        		if(_super_.geTest(mod)){
        			_super_.ballCatched(actingUserIndex);
        		}else{
        			if(_super_.geTest(mod)){
            			_super_.ballCatched(actingUserIndex);
            		}else{
            			_super_.scatterBallAround(actingUserIndex);
            		}
        		}
        	}
        };
        rulesArray[4] = ruleCatchCatch;
        
        var rulesToReturn = Java.to(rulesArray, Java.type("gameLogic.rules.Rule[]"));
    	return rulesToReturn;
    }
    function eventHappened(player, eventString){
    	var SBProtocolMessage = Java.type("network.SBProtocolMessage");
    	if(eventString == SBProtocolMessage.EVENT_YOUR_TURN){
    		player.setSpecialStat(alreadyDodgedKey, "false");
    	}
    }
    function setRemainingBe(player, remainingBe) {}
    function setSpecialRules(player) {}
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
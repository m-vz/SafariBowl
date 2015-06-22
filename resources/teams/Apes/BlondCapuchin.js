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
            "Throwable.",
            "Very small and easily hurt.",
            "Just pushed on 'defender stumbles'.",
            "Very good at passing through tackle",
            "zones, ignores helping tackle zones",
            "of other enemies, when dodging."
        ];
        return Java.to(lines, Java.type("java.lang.String[]"));
    }

	var alreadyDodgedKey = "hasAlreadyDodged";
	var throwableKey = "throwable";

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
        return 16;
    }
    function setRules(player, ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch) {
    	player.addSpecialStat(alreadyDodgedKey, "false");
    	player.addSpecialStat(throwableKey, "true");
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
        var RuleMoveDodgeStunty = Java.extend(RuleMove);
        var ruleMoveDodgeStunty = new RuleMoveDodgeStunty(rulesArray[0].getActor()){
        	tackleTest: function(message, problems, i, path){
        		var _super_ = Java.super(ruleMoveDodgeStunty);
        		if(!(_super_.stayFine(0))){
        			if(player.getSpecialStat(alreadyDodgedKey) == "false"){
        				player.setSpecialStat(alreadyDodgedKey, "true");
        				if(!(_super_.stayFine(0))){
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
        rulesArray[0] = ruleMoveDodgeStunty;
        
        var RuleThrow = Java.type("gameLogic.rules.RuleThrow");
        var RuleThrowStunty = Java.extend(RuleThrow);
        var ruleThrowStunty = new RuleThrowStunty(rulesArray[3].getActor()){
        	findThrowModificator: function(problems, distance){
        		var _super_ = Java.super(ruleThrowStunty);
        		_super_.findThrowModificator(problems - 1, distance);
        	}
        };
        rulesArray[3] = ruleThrowStunty;

        var rulesToReturn = Java.to(rulesArray, Java.type("gameLogic.rules.Rule[]"));
    	return rulesToReturn;
    }
    function eventHappened(player, eventString){}
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
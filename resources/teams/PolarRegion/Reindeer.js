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
            "Doesn't tumble on 'both down'."
        ];
        return Java.to(lines, Java.type("java.lang.String[]"));
    }

    function setPrice(player, price) {
        return 80000;
    }
    function setGe(player, ge) {
        return 3;
    }
    function setRs(player, rs) {
        return 9;
    }
    function setSt(player, st) {
        return 3;
    }
    function setBe(player, be) {
        return 6;
    }
    function adjustMaxHeadcount(player, maxHeadcount) {
        return 4;
    }
    function setRules(player, ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch) {
    	var rulesArray = [ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch];
 
        var RuleBlock = Java.type("gameLogic.rules.RuleBlock");
        var RuleBlockBlock = Java.extend(RuleBlock);
        var ruleBlockBlock = new RuleBlockBlock(rulesArray[1].getActor()) {
        	beingBlockedBothDown: function(message){},
        	bothDown: function(message, defender){
        		defender.getRule(1).beingBlockedBothDown(message, 0, 0);
        		var _super_ = Java.super(ruleBlockBlock);
				_super_.clearHighlightFields();
        		return true;
	    	}
        };
 
        rulesArray[1] = ruleBlockBlock;
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
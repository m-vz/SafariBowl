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
            "Very good at picking up the ball and passing it."
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
        return 8;
    }
    function setSt(player, st) {
        return 3;
    }
    function setBe(player, be) {
        return 6;
    }
    function adjustMaxHeadcount(player, maxHeadcount) {
        return 2;
    }
    function setRules(player, ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch) {
        var rulesArray = [ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch];
    	
    	var RuleMove = Java.type("gameLogic.rules.RuleMove");
    	var RuleMoveSureHands = Java.extend(RuleMove);
    	var ruleMoveSureHands = new RuleMoveSureHands(rulesArray[0].getActor()) {
    		tryToPickUpBall: function(message, i, path){
    			var _super_ = Java.super(ruleMoveSureHands);
    			var PlayerCondition = Java.type("gameLogic.PlayerCondition");
    			var Weather = Java.type("gameLogic.Weather");
    			var mod = 1-(player.getMatch().getOpposingTeam(player.getTeam()).getTacklezones(path[i].getPos()));
    			if(player.getMatch().getWeather() == Weather.POURING_RAIN){
    				mod--;
    			}
    			if(_super_.geTest(mod) && player.invokeGetPlayerCondition()==PlayerCondition.FINE){
    				return _super_.pickedUpBall(message);
    			}else{
    				if(_super_.geTest(mod)){
    					return _super_.pickedUpBall(message);
    				}else{
    					return _super_.faildToPickUpBall(message);
    				}
    			}
    		}
    	};
    	rulesArray[0] = ruleMoveSureHands;
    	 
    	var RuleThrow = Java.type("gameLogic.rules.RuleThrow");
    	var RuleThrowPass = Java.extend(RuleThrow);
    	var ruleThrowPass = new RuleThrowPass(rulesArray[3].getActor()) {
    		throwBall: function(message, destinationField, problems){
    			var _super_ = Java.super(ruleThrowPass);
    			if(_super_.geTest(problems)){
    				_super_.successfulThrow(message, destinationField);
    			}else{
    				if(_super_.geTest(problems)){
    					_super_.successfulThrow(message, destinationField);
    				}else{
    					_super_.notWellThrown(destinationField, message);
    				}
    			}
    			player.invokeSetIsHoldingBall(false);
    			player.invokeSetRemainingBe(0);
    			player.getTeam().setPass(false);
    			player.getMatch().sendBallPos();
    			if(player.getMatch().findTeamHoldingTheBall() != player.getMatch().findUserIndex(message)){
    				player.getMatch().endTurn(message);
    			}
    			player.getMatch().sendPlayer(player);
    		}
    	};
    	rulesArray[3] = ruleThrowPass;
        
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
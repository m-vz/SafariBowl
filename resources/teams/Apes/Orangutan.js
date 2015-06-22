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
			"Has a rock to beat his enemies,",
			"so the risk of injury is high"
		];
		return Java.to(lines, Java.type("java.lang.String[]"));
	}

	var cheatedKey = "cheated";
	var noHandsString = "Sorry, but I have no hands left!";
	var ENEMY_DOWN = "YOU HAVE ROCKED YOUR ENEMY, HE IS ";
	var YOU_SUCK = "YOU HAVE BEEN ROCKED, YOU ARE ";
	var SBProtocolCommand = Java.type("network.SBProtocolCommand");
	var SBProtocolMessage = Java.type("network.SBProtocolMessage");

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
    	player.addSpecialStat(cheatedKey, "false");
    	var rulesArray = [ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch];
    	 
		var RuleBlock = Java.type("gameLogic.rules.RuleBlock");
		var RuleBlockStuntyCheaterRockAttack = Java.extend(RuleBlock);
		var ruleBlockStuntyCheaterRockAttack = new RuleBlockStuntyCheaterRockAttack(rulesArray[1].getActor()) {
			apply: function(message, defender){
				player.setSpecialStat(cheatedKey, "true");
				var _super_ = Java.super(ruleBlockStuntyCheaterRockAttack);
				_super_.apply(message, defender);
			},
			throwDice: function(message, defender){
				player.getMatch().addCurrentHighlitedFields(player.getPos());
				player.getMatch().addCurrentHighlitedFields(defender.getPos());
				var _super_ = Java.super(ruleBlockStuntyCheaterRockAttack);
				_super_.sendHighlightFields(player.getMatch().getCurrentHighlitedFields());
				var attackThrow = player.getMatch().d6.throwDie();
				if(attackThrow > 1){
					defenderField = defender.getPos();
					defender.getRule(1).playerDown(message, defender, ENEMY_DOWN, 3, 0);
					var PlayerCondition = Java.type("gameLogic.PlayerCondition");
					if(defender.invokeGetPlayerCondition().equals(PlayerCondition.DEAD) || defender.invokeGetPlayerCondition().equals(PlayerCondition.INJURED) || defender.invokeGetPlayerCondition().equals(PlayerCondition.KO)){
						player.getRule(2).backUp(defenderField, message, player);
					}
					_super_.clearHighlightFields();
				}else{
					_super_.playerDown(message, player, YOU_SUCK, 3, 0);
					_super_.clearHighlightFields();
				}
			},
			beatHim: function(defender, message){
				var _super_ = Java.super(ruleBlockStuntyCheaterRockAttack);
				var armorRollModifier = player.getTeam().getTacklezones(defender.getPos()) + 2;
					armorRollModifier += defender.getTeam().getTacklezones(player.getPos());
					defender.getRule(1).playerDown(message, defender, RuleBlock.ENEMY_FOULED, armorRollModifier, 0);		
					player.getTeam().setFoul(false);
					_super_.refereeTriesToKeepSurvey(message);
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
		rulesArray[1] = ruleBlockStuntyCheaterRockAttack;
		
		var RuleMove = Java.type("gameLogic.rules.RuleMove");
		var RuleMoveStuntyNoHands = Java.extend(RuleMove);
		var ruleMoveStuntyNoHands = new RuleMoveStuntyNoHands(rulesArray[0].getActor()){
			tackleTest: function(message, problems, i, path){
				var _super_ = Java.super(ruleMoveStuntyNoHands);
				if(!(_super_.stayFine(0))){
					return _super_.beingTackled(message, i, path);
				}else{
					return true;
				}
			},
			tryToPickUpBall: function(message, i, path){
				var _super_ = Java.super(ruleMoveStuntyNoHands);
				return _super_.faildToPickUpBall(message);
			}
		};
		rulesArray[0] = ruleMoveStuntyNoHands;
		
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
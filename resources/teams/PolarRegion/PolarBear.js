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
            "Guy"
        ];
        return Java.to(lines, Java.type("java.lang.String[]"));
    }

    function setPrice(player, price) {
        return 80000;
    }
    function setGe(player, ge) {
        return 2;
    }
    function setRs(player, rs) {
        return 9;
    }
    function setSt(player, st) {
        return 4;
    }
    function setBe(player, be) {
        return 4;
    }
    function adjustMaxHeadcount(player, maxHeadcount) {
        return 4;
    }
    function setRules(player, ruleMove, ruleBlock, rulePush, ruleThrow, ruleCatch) {}
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
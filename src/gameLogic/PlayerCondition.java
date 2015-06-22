package gameLogic;

import java.util.Random;

/**
 * The conditions a player can be in.
 */
public enum PlayerCondition {
    FINE, PRONE, STUNNED, KO, INJURED, DEAD;

    public String randomConditionDescription(Player player) {
        String[] strings;
        switch(player.invokeGetPlayerCondition()) {
            case FINE:
                Weather weather = player.getMatch().getWeather();
                strings = new String[]{
                        "Ready to rumble!",
                        "Ready to go!",
                        "Let's go!",
                        "Let's win this game!",
                        "WE MUST WIN",
                        "Let's go "+player.getName()+"s!",
                        "Come on "+player.getTeam().getName()+"!",
                        "Let's go "+player.getTeam().getName()+"!",
                        "So close!",
                        "Heh, we're gonna win anyway.",
                        "Please no touchdowns on our side!",
                        "God, am I hungry!",
                        "Can I eat this Puffin?",
                        player.getName()+"s totally rock!",
                        player.getName()+"s are the best!",
                        player.getName()+"s will blow you away!",
                        "We'll blow them away!",
                        player.getTeam().getName().length() > 0 ? "GIVE ME A "+player.getTeam().getName().substring(0, 1) : "Why does our team have no name?!",
                        player.getTeam().getName()+" will blow you away!",
                        player.getTeam().getName()+" always win!",
                        player.getTeam().getName()+" are epic!",
                        player.getTeam().getName()+" for the win!",
                        player.getTeam().getMatch().getOpposingTeam(player.getTeam()).getName()+" can suck it!",
                        player.getTeam().getMatch().getOpposingTeam(player.getTeam()).getName()+" are just morons!",
                        player.getTeam().getMatch().getOpposingTeam(player.getTeam()).getName()+" can't even pee straight!",
                        "We'll crush "+player.getTeam().getMatch().getOpposingTeam(player.getTeam()).getName()+"!",
                        "We'll totally murder "+player.getTeam().getMatch().getOpposingTeam(player.getTeam()).getName()+"!",
                        "I'M HUNGRY!",
                        "I urgently need to clobber someone!",
                        "I urgently need to batter someone!",
                        "Those greenhorns gonna eat mud.",
                        "I like fish.",
                        "I don't like fish",
                        "Ey, "+player.getTeam().getCoach().getName()+", fuck off!",
                        "Ey, "+player.getTeam().getCoach().getName()+", get on your seat!",
                        "Ey, "+player.getTeam().getCoach().getName()+", take a walk!",
                        weather == Weather.BLIZZARD ? "I'm gonna freeze in this cold!" : "",
                        weather == Weather.BLIZZARD ? "It's so cold!" : "",
                        weather == Weather.BLIZZARD ? "What ugly weather!" : "",
                        weather == Weather.BLIZZARD ? "Oh look! Snow!" : "",
                        weather == Weather.BLIZZARD ? "SNOWBALL FIGHT!" : "",
                        weather == Weather.BLIZZARD ? "Snowball fight is on!" : "",
                        weather == Weather.BLIZZARD ? "I wanna make a snowman!" : "",
                        weather == Weather.BLIZZARD ? "I wanna make a snowgirl!" : "",
                        weather == Weather.POURING_RAIN ? "I don't like rain!" : "",
                        weather == Weather.POURING_RAIN ? "Ah, I love rain!" : "",
                        weather == Weather.POURING_RAIN ? "Rain!" : "",
                        weather == Weather.POURING_RAIN ? "It's raining man!" : "",
                        weather == Weather.POURING_RAIN ? "Crap rain!" : "",
                        weather == Weather.POURING_RAIN ? "Stop raining already!" : "",
                        weather == Weather.POURING_RAIN ? "Petrus fuck off!" : "",
                        weather == Weather.POURING_RAIN ? "I'm soaking wet!" : "",
                        weather == Weather.VERY_SUNNY ? "It's so hot!" : "",
                        weather == Weather.VERY_SUNNY ? "Look at this weather!" : "",
                        weather == Weather.VERY_SUNNY ? "Boy is it hot!" : "",
                        weather == Weather.VERY_SUNNY ? "HOT HOT HOT!" : "",
                        weather == Weather.VERY_SUNNY ? "The sun is shining." : "",
                        weather == Weather.VERY_SUNNY ? "What a beautiful sun!" : "",
                        weather == Weather.VERY_SUNNY ? "The sun is yellow!" : "",
                        weather == Weather.VERY_SUNNY ? "I have a sunburn!" : "",
                        weather == Weather.SWELTERING_HEAT ? "This sun really is burning me!" : "",
                        weather == Weather.SWELTERING_HEAT ? "This heat, I think I'm fainting." : "",
                        weather == Weather.SWELTERING_HEAT ? "Yeah! Sun!" : "",
                        weather == Weather.SWELTERING_HEAT ? "I hope I won't faint in this heat" : "",
                        weather == Weather.SWELTERING_HEAT ? "Who invented this weather system?! I'm dying in this heat!" : "",
                        weather == Weather.SWELTERING_HEAT ? "Matias I hate you for this heat!" : "",
                        weather == Weather.SWELTERING_HEAT ? "Whoever made it so hot is an arse!" : "",
                        weather == Weather.SWELTERING_HEAT ? "Hhhhhhhhhhhh" : "",
                };
                return randomStringFromArray(strings, player.hashCode());
            case PRONE:
                strings = new String[]{
                        "Prone",
                        "Prone as a pear",
                        "Prone already",
                        "Bananaprone",
                        "Prone as always"
                };
                return randomStringFromArray(strings, player.hashCode());
            case STUNNED:
                strings = new String[]{
                        "Ugh",
                        "Grks",
                        "Phhk",
                        "Grgrgll",
                        "Bhhh"
                };
                return randomStringFromArray(strings, player.hashCode());
            case KO:
                return "K.O.";
            case INJURED:
                strings = new String[]{
                        "It hurst!",
                        "Ouch!",
                        "Ooh, my knee!"
                };
                return randomStringFromArray(strings, player.hashCode());
            case DEAD:
                strings = new String[]{
                        "R.I.P.",
                        "Dead!",
                        "D E A D"
                };
                return randomStringFromArray(strings, player.hashCode());
            default:
                return "In outer space!";
        }
    }

    private String randomStringFromArray(String[] strings, int seed) {
        String returnString;
        int add = 0;
        do {
            returnString = strings[(new Random(seed+add)).nextInt(strings.length-1)];
            add++;
        } while(returnString.length() == 0);
        return returnString;
    }
}
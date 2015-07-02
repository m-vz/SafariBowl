package client.sound;

import gameLogic.Player;

/**
 * Class handling the sounds players make
 */
public class PlayerSound {

    public static void pickUp(Player player) {
        SoundManager.play(player.getSoundPickUp());
    }

    public static void proned(Player player) {
        SoundManager.play(player.getSoundProned());
    }

    public static void stunned(Player player) {
        SoundManager.play(player.getSoundStunned());
    }

    public static void injured(Player player) {
        SoundManager.play(player.getSoundInjured());
    }

    public static void ko(Player player) {
        SoundManager.play(player.getSoundKO());
    }

    public static void blitz(Player player) {
        SoundManager.play(player.getSoundBlitz());
    }

    public static void block(Player player) {
        SoundManager.play(player.getSoundBlock());
    }

    public static void pass(Player player) {
        SoundManager.play(player.getSoundPass());
    }

    public static void died(Player player) {
        SoundManager.play(player.getSoundDied());
    }

}

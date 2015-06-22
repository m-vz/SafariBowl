package gameLogic;

/**
 * An invisible Player that does kickoffs.
 */
public class Kicker extends Player {

    public static final String LEFT = "left";
    public static final String RIGHT = "right";

    /**
     * Constructs a kicker on the specified side. (<code>Kicker.LEFT </code>or <code>Kicker.RIGHT</code>)
     * @param side The side from which this kicker kicks. (<code>Kicker.LEFT </code>or <code>Kicker.RIGHT</code>)
     * @param pitch The pitch this kicker kicks onto.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public Kicker(String side, Pitch pitch) {
        super("Kicker", null);
        position = new PitchField(-1, Pitch.PITCH_WIDTH);
        if(side.equals(RIGHT)) position = new PitchField(Pitch.PITCH_LENGTH, Pitch.PITCH_WIDTH);
    }

}

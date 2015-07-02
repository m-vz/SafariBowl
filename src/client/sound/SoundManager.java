package client.sound;

import javax.sound.sampled.*;

/**
 * Here's a handler for all things audio.
 */
public class SoundManager {

    /**
     * Start playback of an Clip.
     * @param clip The Clip to play back.
     * @return The clip playing the audio. Null if an exception occurs.
     */
    public static Clip play(Clip clip) {
        return loop(clip, 0);
    }

    /**
     * Start looped playback of an Clip.
     * @param clip The Clip to play back.
     * @param count How often the audio should be looped.
     * @return The clip looping the audio. Null if an exception occurs.
     */
    public static Clip loop(Clip clip, int count) {
        if(clip != null) {
            if(clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.loop(count);
        }
        return clip;
    }

}

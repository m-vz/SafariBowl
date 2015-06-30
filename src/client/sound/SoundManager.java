package client.sound;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;

/**
 * Here's a handler for all things audio.
 */
public class SoundManager {

    /**
     * Start playback of an AudioInputStream.
     * @param audio The AudioInputStream to play back.
     * @return The clip playing the audio. Null if an exception occurs.
     */
    public static Clip play(AudioInputStream audio) {
        Clip clip = getClipForAudio(audio);
        if(clip != null) clip.start();
        return clip;
    }

    /**
     * Start looped playback of an AudioInputStream.
     * @param audio The AudioInputStream to play back.
     * @param count How often the audio should be looped.
     * @return The clip looping the audio. Null if an exception occurs.
     */
    public static Clip loop(AudioInputStream audio, int count) {
        Clip clip = getClipForAudio(audio);
        if(clip != null) clip.loop(count);
        return clip;
    }

    /**
     * Get a clip object preloaded with an AudioInputStream.
     * @param audio the AudioInputStream to preload.
     * @return A Clip preloaded with the audio. Null if an exception occurs.
     */
    public static Clip getClipForAudio(AudioInputStream audio) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            return clip;
        } catch(LineUnavailableException e) { e.printStackTrace();
        } catch(IOException e) { e.printStackTrace(); }
        return null;
    }

}

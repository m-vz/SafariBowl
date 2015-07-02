package util;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages resources. Heh.
 * Created by milan on 15.4.15.
 */
public class ResourceManager {

    // IMAGES

    public static final int IMAGE_WIDTH = 429,
                            IMAGE_HEIGHT = 500,
                            PEDESTAL_WIDTH = 351,
                            DIE_IMAGE_WIDTH = 500;
    public static final double IMAGE_RATIO = (double) IMAGE_WIDTH / IMAGE_HEIGHT;

    public static final String IMAGES_PATH = "/images/";

    public static final String IMAGES_MODERATORS_PATH = IMAGES_PATH + "moderators/";
    public static final String IMAGES_DICE_PATH = IMAGES_PATH + "dice/";

    @SuppressWarnings("SuspiciousNameCombination")
    public static BufferedImage IMAGE_MODERATOR_TURTLE = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_MODERATOR_WALRUS = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB),

                                IMAGE_BACKGROUND = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB),

                                IMAGE_PROP_CROSS = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_PROP_STAR = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_PROP_BAND_AID = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_PROP_BAND_AID_DOUBLE = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_PROP_RED_CARD = new BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_PROP_FOOTBALL = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB),

                                IMAGE_DEFAULT_PLAYER_R = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_DEFAULT_PLAYER_L = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB),

                                IMAGE_DIE_PUSHED = new BufferedImage(DIE_IMAGE_WIDTH, DIE_IMAGE_WIDTH, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_DIE_ATTACKER_DOWN = new BufferedImage(DIE_IMAGE_WIDTH, DIE_IMAGE_WIDTH, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_DIE_BOTH_DOWN = new BufferedImage(DIE_IMAGE_WIDTH, DIE_IMAGE_WIDTH, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_DIE_DEFENDER_DOWN = new BufferedImage(DIE_IMAGE_WIDTH, DIE_IMAGE_WIDTH, BufferedImage.TYPE_INT_ARGB),
                                IMAGE_DIE_DEFENDER_STUMBLES = new BufferedImage(DIE_IMAGE_WIDTH, DIE_IMAGE_WIDTH, BufferedImage.TYPE_INT_ARGB);

    // SOUND

    public static final String SOUNDS_PATH = "/sounds/";

    public static final String SOUNDS_CHAT_PATH = SOUNDS_PATH + "chat/";

    public static Clip SOUND_SNAP;

    static {

        // IMAGES

        try {

            IMAGE_BACKGROUND =            ImageIO.read(ResourceManager.class.getResource(IMAGES_PATH + "background.png"));

            // MODERATORS

            IMAGE_MODERATOR_TURTLE =      ImageIO.read(ResourceManager.class.getResource(IMAGES_MODERATORS_PATH + "turtle.png"));
            IMAGE_MODERATOR_WALRUS =      ImageIO.read(ResourceManager.class.getResource(IMAGES_MODERATORS_PATH + "walrus.png"));

            // PROPS

            IMAGE_PROP_CROSS =            ImageIO.read(ResourceManager.class.getResource(IMAGES_PATH + "remove_player.png"));
            IMAGE_PROP_STAR =             ImageIO.read(ResourceManager.class.getResource(IMAGES_PATH + "stunned_star.png"));
            IMAGE_PROP_BAND_AID =         ImageIO.read(ResourceManager.class.getResource(IMAGES_PATH + "band_aid.png"));
            IMAGE_PROP_BAND_AID_DOUBLE =         ImageIO.read(ResourceManager.class.getResource(IMAGES_PATH + "band_aid_double.png"));
            IMAGE_PROP_RED_CARD =         ImageIO.read(ResourceManager.class.getResource(IMAGES_PATH + "red_card.png"));
            IMAGE_PROP_FOOTBALL =         ImageIO.read(ResourceManager.class.getResource(IMAGES_PATH + "football.png"));

            // SPECIAL

            IMAGE_DEFAULT_PLAYER_R =      ImageIO.read(ResourceManager.class.getResource(IMAGES_PATH + "defaultR.png"));
            IMAGE_DEFAULT_PLAYER_L =      ImageIO.read(ResourceManager.class.getResource(IMAGES_PATH + "defaultL.png"));

            // DIE

            IMAGE_DIE_PUSHED =            ImageIO.read(ResourceManager.class.getResource(IMAGES_DICE_PATH + "die_pushed.png"));
            IMAGE_DIE_ATTACKER_DOWN =     ImageIO.read(ResourceManager.class.getResource(IMAGES_DICE_PATH + "die_attacker_down.png"));
            IMAGE_DIE_BOTH_DOWN =         ImageIO.read(ResourceManager.class.getResource(IMAGES_DICE_PATH + "die_both_down.png"));
            IMAGE_DIE_DEFENDER_DOWN =     ImageIO.read(ResourceManager.class.getResource(IMAGES_DICE_PATH + "die_defender_down.png"));
            IMAGE_DIE_DEFENDER_STUMBLES = ImageIO.read(ResourceManager.class.getResource(IMAGES_DICE_PATH + "die_defender_stumbles.png"));

        } catch (IOException e) { e.printStackTrace(); }

        // SOUNDS

        try {

            // CHAT

            SOUND_SNAP = AudioSystem.getClip();
            SOUND_SNAP.open(AudioSystem.getAudioInputStream(ResourceManager.class.getResource(SOUNDS_CHAT_PATH + "snap.wav")));

        } catch(UnsupportedAudioFileException e) { e.printStackTrace();
        } catch(IOException e) { e.printStackTrace();
        } catch(LineUnavailableException e) { e.printStackTrace(); }

    }

    /**
     * Extract the whole file structure of a zip directory into a target directory.
     * @param file The zip file to read.
     * @param destination The destination to write the contents of the file to.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void extractZipFile(File file, File destination) {
        try {

            ZipInputStream inputStream = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry;
            String entryName, dir;

            while ((entry = inputStream.getNextEntry()) != null) {
                entryName = entry.getName();
                if(entry.isDirectory()) {

                    // create directory
                    new File(destination, entryName).mkdirs();

                } else {

                    // create parent directories if necessary
                    int s = entryName.lastIndexOf( File.separatorChar );
                    dir = s == -1 ? null : entryName.substring( 0, s );
                    if(dir != null) new File(destination, dir).mkdirs();

                    // write file
                    byte[] buffer = new byte[4096];
                    int count;
                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(new File(destination, entryName)));
                    while ((count = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, count);
                    outputStream.close();

                }
            }

            inputStream.close();

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Recursively delete a directory.
     * @param file The directory to delete.
     * @throws IOException If there was an error while deleting the directory.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteDirectory(File file) throws IOException{

        if(file.isDirectory()) {
            if(file.list().length == 0) file.delete();
            else {
                for (String nextChild: file.list()) deleteDirectory(new File(file, nextChild));
                file.delete();
            }
        } else file.delete();

    }
}
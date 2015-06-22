package server.logic;

import util.SBApplication;
import util.SBLogger;

import java.io.*;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

/**
 * A class to manage multiple users and their stored data on disk.
 * Created by milan on 23.3.15.
 */
public class UserManager {

    private Vector<User> users = new Vector<User>();
    private File file;
    private SBApplication parent;

    /**
     * Constuct a vector of all valid users in the file. Ignores invalid users.
     * @param parent The parent to log to.
     * @param file The file to read users from.
     */
    public UserManager(SBApplication parent, File file) {
        this.file = file;
        this.parent = parent;
        // read users from database
        if(file.exists()) { // only read file if it exists
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String newUserString;
                while((newUserString = reader.readLine()) != null) {
                    User newUser = new User(this, newUserString);
                    addUser(newUser);
                }
            } catch (IOException e) {
                getParent().log(Level.SEVERE, "Could not read user file. Shutting down not to accidentally overwrite users in unread file.");
                System.exit(-1);
            }
        }
    }

    /**
     * Add a new user to users without writing file.
     * @param user The user to be added.
     */
    public void addUser(User user) {
        // check if name exists already
        boolean existsAlready;
        do {
            existsAlready = false;
            for (User userWithSameNameMaybeBaby: users) {
                if (userWithSameNameMaybeBaby.getName().equals(user.getName())) {
                    user.setNameWithoutWrite(countUpName(user.getName()));
                    existsAlready = true;
                    break;
                }
            }
        } while(existsAlready);
        users.add(user);
    }

    /**
     * Count up the name of the user that is being added.
     * @param name The name to count up.
     * @return The next generation (counted up) name.
     */
    private String countUpName(String name) {
        if(name.endsWith("XIX")) // 19, 29, ...
            return name.replaceAll("XIX$", "XX");
        if(name.endsWith("XVIII")) // 18, 28, ...
            return name.replaceAll("XVIII$", "XIX");
        if(name.endsWith("XIV")) // 14, 24, ...
            return name.replaceAll("XIV$", "XV");
        if(name.endsWith("XIII")) // 13, 23, ...
            return name.replaceAll("XIII$", "XIV");
        if(name.endsWith("IX")) // 9
            return name.replaceAll("IX$", "X");
        if(name.endsWith("VIII")) // 8
            return name.replaceAll("VIII$", "IX");
        for (int i = 1; i <= 3; i++) // 5, 6, 7, 10, 11, 12, 15, 16, 17, 20, 21, 22, ...
            if(name.substring(name.length() - i).matches("(?<!I)(V|X)I?I?$"))
                return name+"I";
        if(name.endsWith("TheFourth")) // 4
            return name.replaceAll("TheFourth$", "V");
        if(name.endsWith("TheThird")) // 3
            return name.replaceAll("TheThird$", "TheFourth");
        if(name.endsWith("TheSecond")) // 2
            return name.replaceAll("TheSecond$", "TheThird");
        return name+"TheSecond"; // 1
    }

    /**
     * Does what is says.
     */
    public void logoutAllUsers() {
        for(User user: users) user.setLoggedOut();
    }

    /**
     * Get a user by its UID.
     * @param UID The UID to authenticate a user for.
     * @return The authenticated user or null if there is no authenticated user with the given UID.
     */
    public User getAuthenticatedUser(UUID UID) {
        for(User user: users) if(user.authenticate(UID)) return user;
        return null;
    }

    /**
     * Get a user by its name.
     * @param name The name of the user to get.
     * @return The user with the given name or null if there is no user with the given name.
     */
    public User getUser(String name) {
        for(User user: users) if(user.getName().equals(name)) return user;
        return null;
    }

    /**
     * Remove a user by its UID and write to file.
     * @param UID The UID of the user to remove.
     */
    public void removeUser(UUID UID) {
        User userToRemove = null;
        for(User user: users) if(user.authenticate(UID)) userToRemove = user;
        if(userToRemove != null) users.remove(userToRemove);
        writeUsers();
    }

    /**
     * Remove a user by its name and write to file.
     * @param name The name of the user to remove.
     */
    public void removeUser(String name) {
        User userToRemove = null;
        for(User user: users) if(user.getName().equals(name)) userToRemove = user;
        if(userToRemove != null) users.remove(userToRemove);
        writeUsers();
    }

    /**
     * Add a new user to users and write file.
     * @param user The user to be added.
     */
    public void writeUser(User user) {
        users.add(user);
        writeUsers();
    }

    /**
     * Check if there already exists a user with the given name.
     * @param name The name to check for.
     * @return Whether there exists a user with the same name.
     */
    public boolean existsName(String name) {
        for(User user: users) if(user.getName().toLowerCase().equals(name.toLowerCase())) return true;
        return false;
    }

    /**
     * Write the (changed) users Vector to file.
     * @return Whether the users have been written to file.
     */
    public boolean writeUsers() {
        // backup file before writing to prevent data loss
        File backup = new File(file.getAbsolutePath()+".lock");
        try {
            InputStream backupReader = new FileInputStream(file);
            OutputStream backupWriter = new FileOutputStream(backup);
            byte[] backupBuffer = new byte[1024];
            int length;
            while ((length = backupReader.read(backupBuffer)) > 0)
                backupWriter.write(backupBuffer, 0, length);
            backupWriter.close();
        } catch (IOException e) {
            getParent().log(Level.SEVERE, "Could not backup file. Not writing to disk.");
            return false;
        }
        // delete and rewrite file
        boolean deletedFile = file.delete();
        PrintWriter writer;
        try {
            writer = new PrintWriter(new FileWriter(file));
            boolean createdFile = file.createNewFile();
        } catch (IOException e) {
            getParent().log(Level.SEVERE, "Could not write file.");
            return false;
        }
        for(User user: users) {
            writer.println(user.resUoTgnirts());
        }
        if(writer.checkError()) {
            getParent().log(Level.SEVERE, "Could not write file.");
            return false;
        } else {
            writer.close();
            // remove backup file again
            boolean deletedBackup = backup.delete();
            return true;
        }
    }

    /**
     * Get a clone of the users vector (to iterate over, etc.).
     * @return A clone of the users vector.
     */
    @SuppressWarnings("unchecked")
    public Vector<User> getUsers() {
        return (Vector<User>) users.clone();
    }

    public SBApplication getParent() {
        return parent;
    }
}

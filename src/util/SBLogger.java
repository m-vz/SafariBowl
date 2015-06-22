package util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.*;

/**
 * A logger class with log levels which writes all messages to file and specified level messages to stdout.
 * Created by milan on 19.3.15.
 */
public class SBLogger extends Logger {

    public static final Level LOG_LEVEL = Level.INFO;
    static private final Level DEFAULT_LEVEL = Level.INFO;

    static private FileHandler logFile;
    static private SBFormatter formatterTxt = new SBFormatter();

    static {
        try {
            File logDir = new File("log");

            boolean madeDir = logDir.mkdirs();
            logFile = new FileHandler("log/main.log", true);
        } catch (IOException e) {
            System.out.println("Couldn't set up logger.");
            e.printStackTrace();
            System.exit(-100);
        }
        logFile.setFormatter(formatterTxt);
        logFile.setLevel(Level.FINE);
    }

    /**
     * Protected method to construct a logger for a named subsystem.<br>
     * The logger will be initially configured with a null Level
     * and with useParentHandlers set to true.
     *
     * @param name               A name for the logger.  This should
     *                           be a dot-separated name and should normally
     *                           be based on the package name or class name
     *                           of the subsystem, such as java.net
     *                           or javax.swing.  It may be null for anonymous Loggers.
     * @param resourceBundleName name of ResourceBundle to be used for localizing
     *                           messages for this logger.  May be null if none
     *                           of the messages require localization.
     */
    public SBLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
        setup(DEFAULT_LEVEL);
    }

    /**
     * SBLogger constructor unlocalized.
     * @param name A name for the logger. Usually MyClass.class.getName().
     * @param level The level at which this logger should log to the console.
     */
    public SBLogger(String name, Level level) {
        super(name, null);
        setup(level);
    }

    /**
     * Log message with set level.
     * @param message The message to log.
     */
    public void log(String message) {
        super.log(Level.SEVERE, message);
    }

    /**
     * Private method to setup logger with level.
     * @param level The level to set for the log console.
     */
    private void setup(Level level) {
        setLevel(Level.FINEST);
        // setup file
        addHandler(logFile);
        // setup console logger
        ConsoleHandler logConsole = new ConsoleHandler();
        logConsole.setFormatter(formatterTxt);
        logConsole.setLevel(level);
        addHandler(logConsole);
    }

    /**
     * Private formatter class to format log messages.
     */
    private static class SBFormatter extends SimpleFormatter {
        public String format(LogRecord logRecord) {
            // format date
            Date date = new Date(logRecord.getMillis());
            // format level
            String level = logRecord.getLevel()+":";
            for (int i = level.length(); i < 10; i++) level += " ";
            // format message
            SimpleDateFormat dateFormat = new SimpleDateFormat("d.M.y H:m:s.S");
            return dateFormat.format(date)+" - "+logRecord.getLoggerName().replaceAll("([^\\.]*\\.)+(?=[^\\.]+)", "")+", "+level+formatMessage(logRecord)+"\n";
        }
    }
}
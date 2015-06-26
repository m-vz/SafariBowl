package server;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import server.shells.GuiShell;
import server.shells.LineShell;

public class ServerController {

    private Server server;

    public static final int DEFAULT_PORT = 9989;

    public void run(boolean withGui, int port) {

        ServerListener serverShell = withGui ? new GuiShell() : new LineShell(port);
        this.server = new Server(serverShell);

        this.server.runServer();
    }

    public static void main(String[] args) throws ParseException {
        Option helpOption = Option.builder("h")
                            .longOpt("help")
                            .required(false)
                            .desc("shows this message")
                            .build();

        Option noGuiOption = Option.builder("n")
                             .longOpt("no-gui")
                             .required(false)
                             .desc("do not use the server gui")
                             .build();

        Option portOption = Option.builder("p")
                            .longOpt("port")
                            .numberOfArgs(1)
                            .required(false)
                            .type(Number.class)
                            .desc("port this server listens on")
                            .build();

        Options options = new Options();
        options.addOption(helpOption);
        options.addOption(noGuiOption);
        options.addOption(portOption);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(options, args);

        if (cmdLine.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sb server", options);
        } else {
            boolean withGui = !cmdLine.hasOption("no-gui");
			int port = cmdLine.hasOption("port") ?
                       ((Number)cmdLine.getParsedOptionValue("port")).intValue() : DEFAULT_PORT;

            ServerController server = new ServerController();
            server.run(withGui, port);
        }
    }
}

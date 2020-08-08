package com.nchroniaris.ASC.util.model;

/**
 * This is a class that represents a GameServer. All variables are final so this can be by some accounts regarded as a data class. Apparently they are regarded as "evil" or "not useful" but at least in the way that I use it it provides some programmatical way of representing a database object. Many of its parameters are accessed multiple times over the course of many Event objects.
 */
public class GameServer {

    // This should match any string that is alphanumeric (lowercase) with any number of optional dashes. The + means at least one character should be there. This regex is used for matching both the game and the moniker *seperately*
    public static final String sessionRegex = "^[a-z0-9\\-]+$";

    private final int sid;

    private final String description;
    private final String sessionName;

    private final String startFile;
    private final String stopCommand;
    private final String warnCommand;

    private final int port;

    private final boolean autostart;

    public GameServer(int sid, String description, String game, String moniker, String startFile, String stopCommand, String warnCommand, int port, boolean autostart) {

        this.sid = sid;

        if (description == null)
            throw new IllegalArgumentException("Description field cannot be null!");

        this.description = description;

        if (game == null)
            throw new IllegalArgumentException("Game field cannot be null!");

        // We won't accept any string with malformed syntax into this system so we raise an error
        if (!game.matches(GameServer.sessionRegex))
            throw new IllegalArgumentException(String.format("Game field must be at least one lowercase alphanumeric character with any number of optional dashes. Got '%s'", game));

        if (moniker == null)
            throw new IllegalArgumentException("Moniker field cannot be null!");

        // We won't accept any string with malformed syntax into this system so we raise an error
        if (!moniker.matches(GameServer.sessionRegex))
            throw new IllegalArgumentException(String.format("Moniker field must be at least one lowercase alphanumeric character with any number of optional dashes. Got '%s'", moniker));

        // Assemble moniker string
        this.sessionName = game + "_" + moniker;

        if (startFile == null)
            throw new IllegalArgumentException("Start File field cannot be null!");

        this.startFile = startFile;

        if (stopCommand == null)
            throw new IllegalArgumentException("Stop Command field cannot be null!");

        this.stopCommand = stopCommand;

        if (warnCommand == null)
            throw new IllegalArgumentException("Warn Text field cannot be null!");

        this.warnCommand = warnCommand;

        if (port < 0 || port > 65535)
            throw new IllegalArgumentException("Port cannot be outside of the range [0, 65535]");

        this.port = port;

        this.autostart = autostart;
    }

    public int getSid() {
        return sid;
    }

    public String getDescription() {
        return description;
    }

    public String getSessionName() {
        return sessionName;
    }

    public String getStartFile() {
        return startFile;
    }

    public String getStopCommand() {
        return stopCommand;
    }

    public String getWarnCommand() {
        return warnCommand;
    }

    public int getPort() {
        return port;
    }

    public boolean isAutostart() {
        return autostart;
    }

}

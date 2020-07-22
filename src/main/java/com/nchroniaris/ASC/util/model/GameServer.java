package com.nchroniaris.ASC.util.model;

public class GameServer {

   private final int sid;
   private final String description;
   private final String game;
   private final String moniker;
   private final int port;
   private final boolean enabled;
   private final boolean autostart;

    public GameServer(int sid, String description, String game, String moniker, int port, boolean enabled, boolean autostart) {
        this.sid = sid;
        this.description = description;
        this.game = game;
        this.moniker = moniker;

        if (port < 0 || port > 65535)
            throw new IllegalArgumentException("Port cannot be outside of the range [0, 65535]");

        this.port = port;

        this.enabled = enabled;
        this.autostart = autostart;
    }

    public int getSid() {
        return sid;
    }

    public String getDescription() {
        return description;
    }

    public String getGame() {
        return game;
    }

    public String getMoniker() {
        return moniker;
    }

    public int getPort() {
        return port;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAutostart() {
        return autostart;
    }

}

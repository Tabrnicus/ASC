package com.nchroniaris.ASC.client.database;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nchroniaris.ASC.client.core.ASCClient;
import com.nchroniaris.ASC.client.model.Event;
import com.nchroniaris.ASC.client.model.EventFactory;
import com.nchroniaris.ASC.util.model.GameServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class that handles the various functions that interact with the ASC database. Most functions read the database and construct rich objects or lists for the rest of the program to use.
 */
public class ASCRepository {

    // Singleton implementation
    private static ASCRepository repository = null;

    // Location of the database. Written as a combination of the actual path of the jar file and a relative path for which the DB should exist. For safety, we are using File.separator in order to grab the system separator chars ('/' on UNIX and '\' on Windows).
    // TODO: 2020-07-21 Potentially move this to a configuration file rather than "hardcode" it
    private static final String DBLocation = ASCClient.jarWorkingDir + File.separator + "resources" + File.separator + "ASC.sqlite3";

    // Generic error message for a SQLExceptionError
    private static final String SQLExceptionError = "[CRITICAL] There was an error communicating with the database! This is most likely due to an old version of the database, a corrupt database (no tables for where there should be one), or an empty database.";

    /**
     * Although there is nothing in this constructor, it MUST be declared private because of the singleton pattern.
     */
    private ASCRepository() {

    }

    public static ASCRepository getInstance() {

        if (ASCRepository.repository == null)
            ASCRepository.repository = new ASCRepository();

        return ASCRepository.repository;

    }

    /**
     * Connects to the database using the DBLocation string and returns a Connection object.
     * The caller must handle both exceptions and MUST close the connection when done. This method is meant to be used on demand -- that is, whenever a database method from this class is called, the function must call connect(), do its work, and call connection.close().
     * The reason that this approach is chosen as opposed to keeping a single connection object alive for the duration of the enclosing class is that the DB operations that are actually run are very few and very far between (mostly running once a day) so it would not make much sense to keep a connection object alive for eighteen hours for example, only for the application to close it on exit.
     *
     * @return Returns the connection object that the caller can use to connect to the database.
     * @throws FileNotFoundException Thrown if the database file specified in DBLocation does not exist
     * @throws SQLException          Thrown if the connection to the database fails somehow other than it not being there
     */
    private Connection connect() throws SQLException, FileNotFoundException {

        // TODO: 2020-07-21 Proper Integrity check for DB
        // Check if the DB exists in the location specified. The reason that this is a separate check is that as far as I can tell, the default behaviour for the DriverManager.getConnection() method when ONLY the DB is missing (and NOT other folders in its path) is that it creates an empty DB which shouldn't technically be allowed. Therefore, if we don't do this check and you happen to delete the DB and run the program again, you will encounter other errors down the line since the schema would be empty. Obviously this is not a foolproof solution but it serves as an extra check
        if (!new File(ASCRepository.DBLocation).exists())
            throw new FileNotFoundException(String.format("[CRITICAL] Database file not found (%s)! Please generate it before running the base program.", ASCRepository.DBLocation));

        // According to the sqlite tutorial for java, in order to use jdbc you must have the following string be prepended to the path.
        return DriverManager.getConnection("jdbc:sqlite:" + ASCRepository.DBLocation);

    }

    // Actual DB Methods //

    /**
     * This method queries the database for all the game servers stored in the servers table. The intention is that the caller will sort through the results and get what they need.
     *
     * @return A List object holding all the model GameServer objects in the table
     */
    public List<GameServer> getAllGameServers() {

        // Get all the game servers in the table. Using * may break the query later on if the database is updated with new columns so all columns are explicitly written
        String query = "SELECT sid, description, game, moniker, startfile, stopcommand, warncommand, port, autostart FROM servers";

        List<GameServer> serverList = new ArrayList<>();

        // https://www.sqlitetutorial.net/sqlite-java/select/
        // All of conn, stmt, and rs are resources that in such a try block structure will automatically get closed -- avoiding a finally statement at the end
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            // Looping through all the elements of the result set
            while (rs.next()) {

                // Create a model GameServer object with all the parameters in the database and add it to the list
                serverList.add(new GameServer(
                        rs.getInt("sid"),
                        rs.getString("description"),
                        rs.getString("game"),
                        rs.getString("moniker"),
                        rs.getString("startfile"),
                        rs.getString("stopcommand"),
                        rs.getString("warncommand"),
                        rs.getInt("port"),
                        rs.getBoolean("autostart")
                ));

            }

        } catch (SQLException e) {

            System.err.println(ASCRepository.SQLExceptionError);
            e.printStackTrace();

            System.exit(1);

        } catch (FileNotFoundException e) {

            e.printStackTrace();
            System.exit(1);

        }

        return serverList;

    }

    /**
     * This method queries the database for all the events for a given GameServer as a parameter.
     *
     * @param server The GameServer object to get all the events for.
     * @return A List of all the events associated with the particular GameServer.
     */
    public List<Event> getAllEvents(GameServer server) {

        // Get all the game servers in the table. Using * may break the query later on if the database is updated with new columns so all columns are explicitly written
        String query = "SELECT e.time, e.etype, e.args FROM events AS e INNER JOIN servers AS s USING(sid) WHERE s.sid = ?";

        List<Event> eventList = new ArrayList<>();

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // According to the Java sql docs, "A ResultSet object is automatically closed when the Statement object that generated it is closed, ..." therefore not closing this should be safe because the try-with-resources block will attempt to close the prepared statement.
            pstmt.setInt(1, server.getSid());
            ResultSet rs = pstmt.executeQuery();

            Gson gson = new Gson();

            while (rs.next()) {

                try {

                    // Add the event based on its etype, passing all information to the factory class. We also deserialize the JSON string that is held in the args field.
                    // https://stackoverflow.com/questions/5554217/google-gson-deserialize-listclass-object-generic-type/17300003#17300003
                    eventList.add(EventFactory.buildEvent(
                            rs.getInt("etype"),
                            server,

                            // Since SQLite does not support storing an actual time type, we have to store it as a string. Therefore, we have to retrieve it as a string and use the java.sql.Time.valueOf() method to convert the string to a Time object. Further, since we are using LocalTime() in Event, we have to convert the SQL Time object to a LocalTime object.
                            Time.valueOf(rs.getString("time")).toLocalTime(),
                            gson.fromJson(rs.getString("args"), String[].class)
                    ));

                } catch (JsonSyntaxException e) {

                    System.err.println(String.format("There was an issue parsing Json data from the args field! Got (%s)", rs.getString("args")));
                    e.printStackTrace();
                    System.exit(1);

                }

            }

        } catch (SQLException e) {

            System.err.println(ASCRepository.SQLExceptionError);
            e.printStackTrace();
            System.exit(1);

        } catch (FileNotFoundException e) {

            e.printStackTrace();
            System.exit(1);

        }

        return eventList;

    }

}

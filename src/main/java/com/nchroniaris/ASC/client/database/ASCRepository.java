package com.nchroniaris.ASC.client.database;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nchroniaris.ASC.client.core.ASCProperties;
import com.nchroniaris.ASC.client.exception.DatabaseNotFoundException;
import com.nchroniaris.ASC.client.model.Event;
import com.nchroniaris.ASC.client.model.EventFactory;
import com.nchroniaris.ASC.util.model.GameServer;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class that handles the various functions that interact with the ASC database. Most functions read the database and construct rich objects or lists for the rest of the program to use.
 */
public class ASCRepository {

    // Singleton implementation
    private static ASCRepository repository = null;

    // Generic error message for a SQLExceptionError
    private static final String SQL_EXCEPTION_ERROR = "[CRITICAL] There was an error communicating with the database! This is most likely due to an old version of the database, a corrupt database (no tables for where there should be one), or an empty database.";

    private static final String TABLE_SERVERS = "servers";
    private static final String TABLE_EVENTS = "events";

    private static final String FIELD_SERVERS_SID = "sid";
    private static final String FIELD_SERVERS_DESCRIPTION = "description";
    private static final String FIELD_SERVERS_GAME = "game";
    private static final String FIELD_SERVERS_MONIKER = "moniker";
    private static final String FIELD_SERVERS_STARTFILE = "startfile";
    private static final String FIELD_SERVERS_STOPCOMMAND = "stopcommand";
    private static final String FIELD_SERVERS_WARNCOMMAND = "warncommand";
    private static final String FIELD_SERVERS_PORT = "port";
    private static final String FIELD_SERVERS_AUTOSTART = "autostart";

    private static final String FIELD_EVENTS_EID = "eid";
    private static final String FIELD_EVENTS_SID = "sid";
    private static final String FIELD_EVENTS_TIME = "time";
    private static final String FIELD_EVENTS_ETYPE = "etype";
    private static final String FIELD_EVENTS_ARGS = "args";

    /**
     * Although there is nothing in this constructor, it MUST be declared private because of the singleton pattern.
     */
    private ASCRepository() {

    }

    public synchronized static ASCRepository getInstance() {

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
     * @throws SQLException Thrown if the connection to the database fails somehow other than it not being there
     */
    private Connection connect() throws SQLException {

        // Get properties instance to access path variable
        ASCProperties properties = ASCProperties.getInstance();

        // TODO: 2020-07-21 Proper Integrity check for DB
        // Check if the DB exists in the location specified. The reason that this is a separate check is that as far as I can tell, the default behaviour for the DriverManager.getConnection() method when ONLY the DB is missing (and NOT other folders in its path) is that it creates an empty DB which shouldn't technically be allowed. Therefore, if we don't do this check and you happen to delete the DB and run the program again, you will encounter other errors down the line since the schema would be empty. Obviously this is not a foolproof solution but it serves as an extra check
        if (!new File(properties.PATH_DB).exists())
            throw new DatabaseNotFoundException(String.format("[CRITICAL] Database file not found (%s)! Please generate it before running the base program.", properties.PATH_DB));

        // According to the sqlite tutorial for java, in order to use jdbc you must have the following string be prepended to the path.
        return DriverManager.getConnection("jdbc:sqlite:" + properties.PATH_DB);

    }

    // Actual DB Methods //

    /**
     * This method queries the database for all the game servers stored in the servers table. The intention is that the caller will sort through the results and get what they need.
     *
     * @return A List object holding all the model GameServer objects in the table
     */
    public List<GameServer> getAllGameServers() {

        // Get all the game servers in the table. Using * may break the query later on if the database is updated with new columns so all columns are explicitly written
        String query = "SELECT" +
                ASCRepository.FIELD_SERVERS_SID +
                ASCRepository.FIELD_SERVERS_DESCRIPTION +
                ASCRepository.FIELD_SERVERS_GAME +
                ASCRepository.FIELD_SERVERS_MONIKER +
                ASCRepository.FIELD_SERVERS_STARTFILE +
                ASCRepository.FIELD_SERVERS_STOPCOMMAND +
                ASCRepository.FIELD_SERVERS_WARNCOMMAND +
                ASCRepository.FIELD_SERVERS_PORT +
                ASCRepository.FIELD_SERVERS_AUTOSTART +
                "FROM" +
                ASCRepository.TABLE_SERVERS;

        List<GameServer> serverList = new ArrayList<>();

        // https://www.sqlitetutorial.net/sqlite-java/select/
        // All of `connection`, `statement`, and `results` are resources that in such a try block structure will automatically get closed -- avoiding a finally statement at the end
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            // Loop through all the elements of the result set
            while (resultSet.next()) {

                // Create a model GameServer object with all the parameters in the database and add it to the list
                serverList.add(new GameServer(
                        resultSet.getInt(ASCRepository.FIELD_SERVERS_SID),
                        resultSet.getString(ASCRepository.FIELD_SERVERS_DESCRIPTION),
                        resultSet.getString(ASCRepository.FIELD_SERVERS_GAME),
                        resultSet.getString(ASCRepository.FIELD_SERVERS_MONIKER),
                        resultSet.getString(ASCRepository.FIELD_SERVERS_STARTFILE),
                        resultSet.getString(ASCRepository.FIELD_SERVERS_STOPCOMMAND),
                        resultSet.getString(ASCRepository.FIELD_SERVERS_WARNCOMMAND),
                        resultSet.getInt(ASCRepository.FIELD_SERVERS_PORT),
                        resultSet.getBoolean(ASCRepository.FIELD_SERVERS_AUTOSTART)
                ));

            }

        } catch (SQLException e) {

            System.err.println(ASCRepository.SQL_EXCEPTION_ERROR);
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
        // Readable SQL statement:
        //      SELECT e.time, e.etype, e.args FROM events AS e INNER JOIN servers AS s USING(sid) WHERE s.sid = ?
        String query = "SELECT" +
                "e." + ASCRepository.FIELD_EVENTS_TIME +
                "e." + ASCRepository.FIELD_EVENTS_ETYPE +
                "e." + ASCRepository.FIELD_EVENTS_ARGS +
                "FROM" +
                ASCRepository.TABLE_EVENTS + "AS e" +
                "INNER JOIN" +
                ASCRepository.TABLE_SERVERS + "AS s" + "USING(sid)" +
                "WHERE" +
                "s." + ASCRepository.FIELD_SERVERS_SID + "= ?";

        List<Event> eventList = new ArrayList<>();

        // We use a PreparedStatement in conjunction with its set*() methods to avoid SQL injection attacks.
        try (Connection connection = connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // According to the Java sql docs, "A ResultSet object is automatically closed when the Statement object that generated it is closed, ..." therefore not closing this should be safe because the try-with-resources block will attempt to close the prepared statement.
            preparedStatement.setInt(1, server.getSid());
            ResultSet resultSet = preparedStatement.executeQuery();

            Gson gson = new Gson();

            // Loop through all the elements of the result set
            while (resultSet.next()) {

                try {

                    // Add the event based on its etype, passing all information to the factory class. We also deserialize the JSON string that is held in the args field.
                    // https://stackoverflow.com/questions/5554217/google-gson-deserialize-listclass-object-generic-type/17300003#17300003
                    eventList.add(EventFactory.buildEvent(
                            resultSet.getInt(ASCRepository.FIELD_EVENTS_ETYPE),
                            server,

                            // Since SQLite does not support storing an actual time type, we have to store it as a string. Therefore, we have to retrieve it as a string and use the java.sql.Time.valueOf() method to convert the string to a Time object. Further, since we are using LocalTime() in Event, we have to convert the SQL Time object to a LocalTime object.
                            Time.valueOf(resultSet.getString(ASCRepository.FIELD_EVENTS_TIME)).toLocalTime(),
                            gson.fromJson(resultSet.getString(ASCRepository.FIELD_EVENTS_ARGS), String[].class)
                    ));

                } catch (JsonSyntaxException e) {

                    System.err.printf("There was an issue parsing Json data from the args field! Got (%s)%n", resultSet.getString(ASCRepository.FIELD_EVENTS_ARGS));
                    e.printStackTrace();
                    System.exit(1);

                }

            }

        } catch (SQLException e) {

            System.err.println(ASCRepository.SQL_EXCEPTION_ERROR);
            e.printStackTrace();
            System.exit(1);

        }

        return eventList;

    }

}

package com.nchroniaris.ASC.client.database;

import com.nchroniaris.ASC.client.core.ASCClient;
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

    public static ASCRepository getRepository() {

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
     * This method queries the database for all the game servers with the autostart flag set to true.
     *
     * @return A List object holding all the model GameServer objects that match the query.
     */
    public List<GameServer> getAutostartGameServers() {

        return this.genericGameServerQuery("WHERE autostart = 1");

    }

    /**
     * This method queries the database for all the game servers stored in the servers table.
     *
     * @return A List object holding all the model GameServer objects in the table
     */
    public List<GameServer> getAllGameServers() {

        // This is empty because we don't want to have a condition on the query as we want all the servers in the table.
        return this.genericGameServerQuery("");

    }

    /**
     * Although the use of this function can be considered slightly awkward, it is for good reason. Since I require at least two very similar queries that get the same result, I decided to abstract away most of the operation and leave an "optional" WHERE clause to be specified. This gives the benefit that the code does not need to be duplicated for each similar operation.
     * @param whereStmt A String formatted in proper SQLite that is ONLY the WHERE component of a statement that queries game servers from the table "servers".
     * @return A List object holding all the model GameServer objects that match the query created by appending the whereStmt String.
     */
    private List<GameServer> genericGameServerQuery(String whereStmt) {

        // Get all the game servers in the table. Using * may break the query later on if the database is updated with new columns so all columns are explicitly written
        String query = "SELECT sid, description, game, moniker, port, enabled, autostart FROM servers " + whereStmt;

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
                        rs.getInt("port"),
                        rs.getBoolean("enabled"),
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

}

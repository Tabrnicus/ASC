package com.nchroniaris.ASC.client.database;

import com.nchroniaris.ASC.client.core.ASCClient;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton class that handles the various functions that interact with the ASC database. Most functions read the database and construct rich objects or lists for the rest of the program to use.
 */
public class ASCRepository {

    // Singleton implementation
    private static ASCRepository repository = null;

    // Location of the database. Written as a combination of the actual path of the jar file and a relative path for which the DB should exist. For safety, we are using File.separator in order to grab the system separator chars ('/' on UNIX and '\' on Windows).
    private static final String DBLocation = ASCClient.jarWorkingDir + File.separator + "resources" + File.separator + "ASC.sqlite3";

    /**
     * Handles initializing the actual repository. This constructor MUST be private because of the singleton pattern
     */
    private ASCRepository() {

        // TODO: 2020-07-20 Integrity check for DB

        this.connect();

    }

    public static ASCRepository getRepository() {

        if (ASCRepository.repository == null)
            ASCRepository.repository = new ASCRepository();

        return ASCRepository.repository;

    }

    private void connect() {

        Connection conn = null;

        // Try to establish a connection to the database
        try {

            String url = "jdbc:sqlite:" + ASCRepository.DBLocation;
            conn = DriverManager.getConnection(url);

            System.out.println("Connection good");

        } catch (SQLException e) {

            System.err.println("[CRITICAL] ASC failed to read DB: " + e.getMessage());

        } finally {

            try {

                if (conn != null) {
                    conn.close();
                }

            } catch (SQLException ex) {

                System.out.println(ex.getMessage());

            }

        }

    }


    // Actual DB Methods //

}

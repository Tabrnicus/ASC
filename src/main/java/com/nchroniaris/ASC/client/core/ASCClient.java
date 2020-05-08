package com.nchroniaris.ASC.client.core;

public class ASCClient {

    private boolean serverless;

    public ASCClient(boolean serverless) {

        this.serverless = serverless;

    }

    public void start() {

        // Test output to see if the arguments work or not
        System.out.println(String.format("The current serverless value is {%s}", this.serverless ? "true" : "false"));

    }

}

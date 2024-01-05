package org.hobbit.controller;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Assume;

/**
 * A simple class that offers utility methods to assume the connectivity within
 * JUnit tests.
 * 
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class ConnectivityAssumptionUtils {

    public static void assumeConnectivity(String httpUrl) {
        try {
            URL pingUrl = new URL(httpUrl);
            HttpURLConnection connection = (HttpURLConnection) pingUrl.openConnection();

            Assume.assumeTrue(
                    "Got a wrong status (" + connection.getResponseCode()
                            + ") code while checking the connectivity to \"" + httpUrl
                            + "\". I will assume that I cannot connect to this endpoint.",
                    connection.getResponseCode() < 400);
        } catch (Exception e) {
            Assume.assumeNoException(
                    "Exception while checking connectivity to \"" + httpUrl
                            + "\". I will assume that I cannot connect to this endpoint. Exception: " + e.getMessage(),
                    e);
        }
    }
}

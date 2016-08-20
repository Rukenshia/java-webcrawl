package com.janchr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Jan on 8/20/2016.
 */
public class Http {
    public static BufferedReader Get(URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod("GET");

        // pretend that we are a new-ish browser. current user agent is actually from 2015.
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36");
        con.setInstanceFollowRedirects(true);

        int statusCode = con.getResponseCode();

        // https://www.mkyong.com/java/java-httpurlconnection-follow-redirect-example/
        boolean redirect = false;
        if (statusCode != HttpURLConnection.HTTP_OK) {
            if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || statusCode == HttpURLConnection.HTTP_MOVED_PERM
                    || statusCode == HttpURLConnection.HTTP_SEE_OTHER)
                redirect = true;
        }

        if (redirect) {
            // get redirect url from "location" header field
            String newUrl = con.getHeaderField("Location");
            // get the cookie if need
            String cookies = con.getHeaderField("Set-Cookie");

            return Http.Get(new URL(newUrl));
        }


        return new BufferedReader(new InputStreamReader(con.getInputStream()));
    }
}

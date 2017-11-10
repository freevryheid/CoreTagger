package andre.smit.coretagger;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
//import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HTTPDataHandler {

    static String stream = null;
    private static final String LOG_TAG = "HTTP";

    public HTTPDataHandler(){
    }

    public String GetHTTPData(String urlString){
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            // Check the connection status
            if (urlConnection.getResponseCode() == 200) {
                // if response code = 200 ok
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                // Read the BufferedInputStream
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
                stream = sb.toString();
                // End reading...............
                // Disconnect the HttpURLConnection
                urlConnection.disconnect();
            } else {
                // Do something
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(LOG_TAG, "FIXME: No server!");
            e.printStackTrace();
        } finally {
            // Do something
        }
        // Return the data from specified url
        return stream;
    }

    public String SendHTTPData(String urlString, String jString, String core) {
        int responseCode = 0;
        try {
            URL url = new URL(urlString);
            BufferedWriter writer;
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.setConnectTimeout(2000);
            urlConnection.setDoOutput(true);
            urlConnection.connect();
            OutputStream outputStream = urlConnection.getOutputStream();
            writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(jString);
            writer.close();
            outputStream.close();
            responseCode = urlConnection.getResponseCode();
            if (responseCode == 200) {
                stream = core;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Do something
        }
        // Return the data from specified url
        return stream;
    }

}

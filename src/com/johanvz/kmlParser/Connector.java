package com.johanvz.kmlParser;

import com.johanvz.kmlParser.model.Client;
import okhttp3.*;

import java.io.IOException;

public class Connector {
    private final OkHttpClient okHttpClient;
    private final String username;
    private final String password;

    public Connector(final String username, final String password) {
        this.username = username;
        this.password = password;
        this.okHttpClient = new OkHttpClient.Builder().authenticator(
                (route, response) -> {

                    if (response.request().header("Authorization") != null) {
                        return null; // Give up, we've already attempted to authenticate.
                    }

                    System.out.println("Authenticating for response: " + response);
                    System.out.println("Challenges: " + response.challenges());
                    String creds = Credentials.basic(username, password);
                    return response.request().newBuilder().header("Authorization", creds).build();
                }
        ).build();

    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public String postClient(Client client) {
        String response = "";
        RequestBody requestBody = RequestBody.create(Globals.JSON, getJSONfromClient(client));
        Request request = new Request.Builder().url(Globals.urlUL).post(requestBody).build();

        try {
            Response response1 = okHttpClient.newCall(request).execute();
            return response1.headers().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    private String getJSONfromClient(Client client) {
        return "{'name':'" + client.getName() + "'," +
                "'description':'" + client.getDescription() + "'," +
                "'coordinate':'" + client.getCoordinate() + "'}";
    }


}

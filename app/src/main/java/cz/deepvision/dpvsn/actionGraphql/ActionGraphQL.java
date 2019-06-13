package cz.deepvision.dpvsn.actionGraphql;

import android.util.Log;

import com.google.gson.JsonObject;
import com.hosopy.actioncable.ActionCable;
import com.hosopy.actioncable.Channel;
import com.hosopy.actioncable.Consumer;
import com.hosopy.actioncable.Subscription;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionGraphQL {
    private String graphqlQuery, token, wssUrl, channelId;
    private JsonObject variables;

    public ActionGraphQL(String graphqlQuery, String token, String wssUrl, JsonObject variables) {
        this.graphqlQuery = graphqlQuery;
        this.token = token;
        this.wssUrl = wssUrl;
        this.variables = variables;
    }

    public void subscribe() {
        URI uri = null;
        try {
            uri = new URI(this.wssUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        Consumer.Options options = new Consumer.Options();

        Map<String, String> headers = new HashMap();
        headers.put("Origin", "https://pos.speedlo.cloud");
        options.headers = headers;
        options.reconnection = true;

        Consumer consumer = ActionCable.createConsumer(uri, options);

        this.channelId = UUID.randomUUID().toString();
        Channel graphqlChannel = new Channel("GraphqlChannel");
        graphqlChannel.addParam("channelId", channelId);
        Subscription subscription = consumer.getSubscriptions().create(graphqlChannel);

        subscription
                .onConnected(() -> {
                    Log.e("Cable", "Connected");

                    JsonObject graphqlQuery = new JsonObject();
                    graphqlQuery.addProperty("query", this.graphqlQuery);
                    graphqlQuery.add("variables", this.variables);

                    subscription.perform("execute", graphqlQuery);
                })
                .onReceived(jsonElement -> {
                    Log.e("Cable", "Received " + jsonElement);
                    if (jsonElement.getAsJsonObject().get("more").getAsBoolean() == false) {
                        consumer.disconnect();
                    }
                })
                .onDisconnected(() -> Log.e("Cable", "Disconnected"))
                .onFailed(e -> Log.e("Cable", "Failed " + e))
                .onRejected(() -> Log.e("Cable", "Rejected"));

        consumer.connect();
    }
}
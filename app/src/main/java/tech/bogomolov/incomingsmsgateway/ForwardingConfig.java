package tech.bogomolov.incomingsmsgateway;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class ForwardingConfig {
    final private Context context;

    private static final String KEY_URL = "url";
    private static final String KEY_IGNORE_SSL = "ignore_ssl";
    private static final String KEY_ORACLE_SECRET = "oracle_secret";
    private static final String KEY_PHONE_NUMBER = "phone_number";


    private String sender;
    private String url;

    private String oracleSecret;
    private String phoneNumber;
    private boolean ignoreSsl = false;

    public ForwardingConfig(Context context) {
        this.context = context;
    }

    public void setOracleSecret(String oracleSecret) {
        this.oracleSecret = oracleSecret;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getUrl() {
        return this.url;
    }

    public String getFullUrl() {
        return this.url + "/oracles/message";
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeaders() {
        if(this.oracleSecret == "") {
            throw new Error("Can not get headers because oracle setter is not set");
        }
        return "{\"User-agent\":\"SMS Forwarder App\", \"Content-Type\":\"application/json\", \"x-oracle-secret\":\""+this.oracleSecret+"\"}";
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getTemplate() {

        String template = "{\"sender\":\"%phoneNumber%\",\"message\":\"%text%\",\"to\":\"system\",\"channels\":\"app\",\"expiration\":\"%expiration%\",\"metadata\":{\"source\":\"%from%\",\"sendStamp\":\"%sendStamp%\",\"receivedStamp\":\"%receivedStamp%\",\"sim\":\"%sim%\"}}";

        LocalDateTime expiration = LocalDateTime.now().plusDays(1);
        Log.d("TAG", LocalDateTime.now().toString());

        return template
                .replace("%phoneNumber%", this.phoneNumber)
                .replace("%expiration%", expiration.format(DateTimeFormatter.ISO_DATE));
    }

    public String getBody() {
        if(this.oracleSecret == "") {
            throw new Error("Can not get headers because oracle setter is not set");
        }
        return "{\"User-agent\":\"SMS Forwarder App\", \"Content-Type\":\"application/json\", \"x-token-secret\":\""+this.oracleSecret+"\"}";
    }

    public boolean getIgnoreSsl() {
        return this.ignoreSsl;
    }

    public void setIgnoreSsl(boolean ignoreSsl) {
        this.ignoreSsl = ignoreSsl;
    }

    public void save() {
        try {
            JSONObject json = new JSONObject();
            json.put(KEY_URL, this.url);
            json.put(KEY_ORACLE_SECRET, this.oracleSecret);
            json.put(KEY_PHONE_NUMBER, this.phoneNumber);
            json.put(KEY_IGNORE_SSL, this.ignoreSsl);

            SharedPreferences.Editor editor = getEditor(context);
            editor.putString(this.sender, json.toString());

            editor.commit();
        } catch (Exception e) {
            Log.e("ForwardingConfig", e.getMessage());
        }
    }

    public static String getDefaultJsonTemplate() {
        return "{\"merchantNo\":\"%merchantNo%\",\"imei\":\"%imei%\",\"otp\":\"%text%\",\"meta\":{\"sentStamp\":\"%sendStamp%\",\"receivedStamp\":\"%receivedStamp%\",\"sim\":\"%sim%\",\"from\":\"%from%\"}}'";
    }


    public static ArrayList<ForwardingConfig> getAll(Context context) {
        SharedPreferences sharedPref = getPreference(context);
        Map<String, ?> sharedPrefs = sharedPref.getAll();

        ArrayList<ForwardingConfig> configs = new ArrayList<>();


        for (Map.Entry<String, ?> entry : sharedPrefs.entrySet()) {

            ForwardingConfig config = new ForwardingConfig(context);

            config.setSender(entry.getKey());

            String value = (String) entry.getValue();

            if (value.charAt(0) == '{') {
                try {
                    JSONObject json = new JSONObject(value);
                    config.setUrl(json.getString(KEY_URL));
                    config.setOracleSecret(json.getString(KEY_ORACLE_SECRET));
                    config.setPhoneNumber(json.getString(KEY_PHONE_NUMBER));

                    try {
                        config.setIgnoreSsl(json.getBoolean(KEY_IGNORE_SSL));
                    } catch (JSONException ignored) {
                    }
                } catch (JSONException e) {
                    Log.e("ForwardingConfig", e.getMessage());
                }
            } else {
                config.setUrl(value);
            }

            configs.add(config);
        }

        return configs;
    }

    public void remove() {
        SharedPreferences.Editor editor = getEditor(context);
        editor.remove(this.getSender());
        editor.commit();
    }

    private static SharedPreferences getPreference(Context context) {
        return context.getSharedPreferences(
                context.getString(R.string.key_phones_preference),
                Context.MODE_PRIVATE
        );
    }

    private static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences sharedPref = getPreference(context);
        return sharedPref.edit();
    }
}

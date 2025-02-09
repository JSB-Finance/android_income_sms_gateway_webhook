package tech.bogomolov.incomingsmsgateway;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import tech.bogomolov.incomingsmsgateway.SSLSocketFactory.TLSSocketFactory;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WebHookWorkRequest extends Worker {

    public final static String DATA_PHONE = "PHONE";
    public final static String DATA_URL = "URL";
    public final static String DATA_TEXT = "TEXT";
    public final static String DATA_HEADERS = "HEADERS";
    public final static String DATA_IGNORE_SSL = "IGNORE_SSL";
    public static final int MAX_ATTEMPT = 10;

    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_ERROR = "error";
    public static final String RESULT_RETRY = "error_retry";

    public WebHookWorkRequest(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (getRunAttemptCount() > MAX_ATTEMPT) {
            return Result.failure();
        }

        String url = getInputData().getString(DATA_URL);
        String text = getInputData().getString(DATA_TEXT);
        String headers = getInputData().getString(DATA_HEADERS);
        boolean ignoreSsl = getInputData().getBoolean(DATA_IGNORE_SSL, false);

        String result = this.makeRequest(url, text, headers, ignoreSsl);

        if (result.equals(RESULT_RETRY)) {
            return Result.retry();
        }

        if (result.equals(RESULT_ERROR)) {
            return Result.failure();
        }

        return Result.success();
    }

    @SuppressLint({"AllowAllHostnameVerifier"})
    private String makeRequest(String urlString, String text, String headers, boolean ignoreSsl) {
        String result = RESULT_SUCCESS;
        String account = getInputData().getString(DATA_PHONE).toString();

        Log.i("SmsGateway", "request " + urlString);

        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();

            if (urlConnection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) urlConnection).setSSLSocketFactory(
                        new TLSSocketFactory(ignoreSsl)
                );

                if (ignoreSsl) {
                    ((HttpsURLConnection) urlConnection).setHostnameVerifier(new AllowAllHostnameVerifier());
                }
            }

            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);

            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            JSONObject headersObj = new JSONObject(headers);
            Iterator<String> keys = headersObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (headersObj.get(key) instanceof JSONObject) {
                    Log.e("SmsGateway", "only string supported in json");
                    continue;
                }

                urlConnection.setRequestProperty(key, (String) headersObj.get(key));
            }

            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

            BufferedWriter writer;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            } else {
                writer = new BufferedWriter(new OutputStreamWriter(out));
            }

            writer.write(text);
            writer.flush();
            writer.close();
            out.close();



            char code = Integer.toString(urlConnection.getResponseCode()).charAt(0);


            if (!Character.toString(code).equals("2")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
                JSONObject jsonObject = new JSONObject(errorResponse.toString());
                String error = jsonObject.getString("err");
                Log.e("SmsGateway", "HTTP Error Response: " + error);
                 SmsLogModel.error(account, error);
                result = RESULT_RETRY;
            } else {
                SmsLogModel.success(account, "OK");
            }


        } catch (MalformedURLException e) {
            result = RESULT_ERROR;
            SmsLogModel.error(account, e.getMessage());
            Log.e("SmsGateway", "MalformedURLException " + e);
        } catch (IOException e) {
            result = RESULT_RETRY;
            SmsLogModel.error(account, e.getMessage());
            Log.e("SmsGateway", "Exception " + e);
        } catch (Exception e) {
            result = RESULT_ERROR;
            SmsLogModel.error(account, e.getMessage());
            Log.e("SmsGateway", "Exception " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return result;
    }
}

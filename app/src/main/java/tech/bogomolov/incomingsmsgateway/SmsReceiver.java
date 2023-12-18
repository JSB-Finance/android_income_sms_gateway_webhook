package tech.bogomolov.incomingsmsgateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.annotation.RequiresApi;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.apache.commons.text.StringEscapeUtils;

public class SmsReceiver extends BroadcastReceiver {

    private Context context;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return;
        }

        final SmsMessage[] messages = new SmsMessage[pdus.length];
        for (int i = 0; i < pdus.length; i++) {
            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
         }

        this.notifyMessages(this.context, messages, this.detectSim(bundle));

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void notifyMessages(Context context, SmsMessage[] messages, String sim ) {


        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);
        String asterisk = context.getString(R.string.asterisk);

        String sender = messages[0].getOriginatingAddress();

        ForwardingConfig matchedConfig = null;
        for (ForwardingConfig config : configs) {
            if (sender.equals(config.getSender()) || config.getSender().equals(asterisk)) {
                matchedConfig = config;
                break;
            }
        }

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < messages.length; i++) {
             content.append(messages[i].getDisplayMessageBody());
        }

        if (matchedConfig == null) {
            return;
        }

        MessageInfo info  = new MessageInfo();
        info.sender = sender;
        info.sim = sim;
        info.content =Matcher.quoteReplacement(StringEscapeUtils.escapeJson(content.toString()));
        info.timestamp = String.valueOf(System.currentTimeMillis());


        this.callWebHook(info,matchedConfig);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void callWebHook(MessageInfo info, ForwardingConfig matchedConfig) {

        String message = matchedConfig.getTemplate()
                .replaceAll("%from%", info.sender)
                .replaceAll("%sentStamp%", info.timestamp)
                .replaceAll("%receivedStamp%", String.valueOf(System.currentTimeMillis()))
                .replaceAll("%sim%", info.sim)
                .replaceAll("%text%", info.content);


        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data data = new Data.Builder()
                .putString(WebHookWorkRequest.DATA_URL, matchedConfig.getFullUrl())
                .putString(WebHookWorkRequest.DATA_PHONE, matchedConfig.getPhoneNumber())
                .putString(WebHookWorkRequest.DATA_TEXT, message)
                .putString(WebHookWorkRequest.DATA_HEADERS, matchedConfig.getHeaders())
                .putBoolean(WebHookWorkRequest.DATA_IGNORE_SSL, matchedConfig.getIgnoreSsl())
                .build();

        WorkRequest webhookWorkRequest =
                new OneTimeWorkRequest.Builder(WebHookWorkRequest.class)
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS
                        )
                        .setInputData(data)
                        .build();

        WorkManager
                .getInstance(this.context)
                .enqueue(webhookWorkRequest);

    }

    private String detectSim(Bundle bundle) {
        int slotId = -1;
        Set<String> keySet = bundle.keySet();
        for (String key : keySet) {
            switch (key) {
                case "phone":
                    slotId = bundle.getInt("phone", -1);
                    break;
                case "slot":
                    slotId = bundle.getInt("slot", -1);
                    break;
                case "simId":
                    slotId = bundle.getInt("simId", -1);
                    break;
                case "simSlot":
                    slotId = bundle.getInt("simSlot", -1);
                    break;
                case "slot_id":
                    slotId = bundle.getInt("slot_id", -1);
                    break;
                case "simnum":
                    slotId = bundle.getInt("simnum", -1);
                    break;
                case "slotId":
                    slotId = bundle.getInt("slotId", -1);
                    break;
                case "slotIdx":
                    slotId = bundle.getInt("slotIdx", -1);
                    break;
                case "android.telephony.extra.SLOT_INDEX":
                    slotId = bundle.getInt("android.telephony.extra.SLOT_INDEX", -1);
                    break;
                default:
                    if (key.toLowerCase().contains("slot") | key.toLowerCase().contains("sim")) {
                        String value = bundle.getString(key, "-1");
                        if (value.equals("0") | value.equals("1") | value.equals("2")) {
                            slotId = bundle.getInt(key, -1);
                        }
                    }
            }

            if (slotId != -1) {
                break;
            }
        }

        if (slotId == 0) {
            return "sim1";
        } else if (slotId == 1) {
            return "sim2";
        } else {
            return "undetected";
        }
    }
}

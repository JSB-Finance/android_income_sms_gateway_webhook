package tech.bogomolov.incomingsmsgateway;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

public class MessageInfo {
     public String content;
     public String sender;
     public String sim;
     public String timestamp;

     public MessageInfo()  {
          this.timestamp=  String.valueOf(System.currentTimeMillis());
     }
}

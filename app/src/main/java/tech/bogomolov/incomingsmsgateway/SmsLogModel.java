package tech.bogomolov.incomingsmsgateway;

import java.util.ArrayList;

public class SmsLogModel {
     public static ArrayList<SmsLogModel> dataSet = new ArrayList<SmsLogModel>();
     public static void success(String phone, String logMessage) {
          SmsLogModel log = new SmsLogModel(phone, logMessage, true);
          dataSet.add(log);
     }

     public static void error(String phone, String logMessage) {
          SmsLogModel log =  new SmsLogModel(phone, logMessage, false);
          dataSet.add(log);
     }

     public static void reset() {
          dataSet = new ArrayList<SmsLogModel>();
     }


     public String phone;
     public String logMessage;
     public boolean success;

     private SmsLogModel(String phone, String logMessage, boolean success) {
          this.phone = phone;
          this.logMessage = logMessage;
          this.success = success;
     }


}

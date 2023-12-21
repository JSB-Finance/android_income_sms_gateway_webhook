package tech.bogomolov.incomingsmsgateway;

import java.util.ArrayList;

public class SmsLogModel {
     public static ArrayList<SmsLogModel> dataSet = new ArrayList<SmsLogModel>();
     public static void success(String phone, String message) {
          SmsLogModel log = new SmsLogModel(phone, message, true);
          dataSet.add(log);
     }

     public static void error(String phone, String message) {
          SmsLogModel log =  new SmsLogModel(phone, message, false);
          dataSet.add(log);
     }

     public static void reset() {
          dataSet = new ArrayList<SmsLogModel>();
     }


     public String phone;
     public String message;
     public boolean success;

     private SmsLogModel(String phone, String message, boolean success) {
          this.phone = phone;
          this.message = message;
          this.success = success;
     }


}

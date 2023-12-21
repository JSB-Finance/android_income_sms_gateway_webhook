package tech.bogomolov.incomingsmsgateway;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SMSAPP"; // Tag for identifying log messages

    private Context context;
    private ListAdapter listAdapter;

    private static final int PERMISSION_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * RECIEVE_SMS: To read incoming SMS
         * READ_PHONE_STAT: To get the phone imei
         */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
              ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, PERMISSION_CODE);
        } else {
            showList();
        }
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException (Thread thread, Throwable e) {
                        handleUncaughtException (thread, e);
                    }
                });
    }

    private void handleUncaughtException (Thread thread, Throwable e) {

        // The following shows what I'd like, though it won't work like this.
        Intent intent = new Intent (getApplicationContext(),MainActivity.class);
        startActivity(intent);

        // Add some code logic if needed based on your requirement
    }


    private boolean checkPermission(String myPermission, @NonNull String[] permissions, @NonNull int[] grantResults ) {
        for (int i = 0; i < permissions.length; i++) {
            if (!permissions[i].equals(myPermission)) {
                continue;
            }

            return grantResults[i] == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_CODE) {
            return;
        }

        if(!this.checkPermission(Manifest.permission.RECEIVE_SMS, permissions, grantResults)){
            showError(getResources().getString(R.string.permission_sms_needed));
            return;
        }


        showList();
    }

    private void showList() {
        showError("");

        context = this;
        ListView listview = findViewById(R.id.listView);


        FloatingActionButton fab = findViewById(R.id.btn_add);
        fab.setOnClickListener(this.showAddDialog());

        ArrayList<ForwardingConfig> configs = ForwardingConfig.getAll(context);

        listAdapter = new ListAdapter(configs, this);
        listview.setAdapter(listAdapter);

        FloatingActionButton logListButton = findViewById(R.id.button_log);
        logListButton.setOnClickListener(this.showLogButton());

        this.toggleButtons(configs.size() == 0 );
        if (!this.isServiceRunning()) {
            this.startService();
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(tech.bogomolov.incomingsmsgateway.SmsReceiverService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startService() {
        Context appContext = getApplicationContext();
        Intent intent = new Intent(this, SmsReceiverService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }

    private void showError(String text) {
        TextView notice = findViewById(R.id.info_notice);
        notice.setText(text);
    }

    private View.OnClickListener showAddDialog() {
        return v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = getLayoutInflater().inflate(R.layout.dialog_add, null);
            final EditText oracleSecretInput =  view.findViewById(R.id.oracle_secret);
            final EditText phoneNumberInput =  view.findViewById(R.id.phone_number);
            final EditText urlInput =  view.findViewById(R.id.url_input);
            urlInput.setText("https://123zf.xyz");

            builder.setView(view);
            builder.setPositiveButton(R.string.btn_add, null);
            builder.setNegativeButton(R.string.btn_cancel, null);

            final AlertDialog dialog = builder.show();

            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view1 -> {

                String oracleSecret = oracleSecretInput.getText().toString();
                if (TextUtils.isEmpty(oracleSecret)) {
                    oracleSecretInput.setError("Please input a value");
                    return;
                }

                String phoneNumber = phoneNumberInput.getText().toString();
                if (TextUtils.isEmpty(phoneNumber)) {
                    phoneNumberInput.setError("Please input a value");
                    return;
                }


                String url = urlInput.getText().toString();
                if (TextUtils.isEmpty(url)) {
                    urlInput.setError("Please input a value");
                    return;
                }


                /**
                 * For our project we just need the merchant no, rest of it can be harcoded
                 */
                String sender = "*";

                boolean ignoreSsl = false;

                ForwardingConfig config = new ForwardingConfig(context);

                config.setOracleSecret(oracleSecret);
                config.setPhoneNumber(phoneNumber);
                config.setSender(sender);
                config.setUrl(url);
                config.setIgnoreSsl(ignoreSsl);
                config.save();

                listAdapter.add(config);

                dialog.dismiss();
                this.toggleButtons(false);

            });
        };
    }

    private View.OnClickListener showLogButton() {
        return v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            View view = getLayoutInflater().inflate(R.layout.list_log, null);
            builder.setView(view);

            // Find the ListView in the custom layout
            ListView listView = view.findViewById(R.id.logView);

            LogAdapter logAdapter = new LogAdapter(SmsLogModel.dataSet, context);
            listView.setAdapter(logAdapter);

            builder.setNegativeButton(R.string.btn_cancel, null);

            final AlertDialog dialog = builder.show();

            View clearLog = view.findViewById(R.id.clear_log);
            clearLog.setOnClickListener( event -> logAdapter.clearDataSet());


            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
         };
    }

    public void toggleButtons(boolean show) {

        FloatingActionButton fab = findViewById(R.id.btn_add);
        FloatingActionButton logListButton = findViewById(R.id.button_log);

        if(!show ) {
            fab.hide();
            logListButton.show();
            return;
        }

        fab.show();
        logListButton.hide();


    }

}

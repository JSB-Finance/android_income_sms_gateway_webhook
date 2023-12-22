package tech.bogomolov.incomingsmsgateway;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import android.telephony.SmsMessage;

public class ListAdapter extends ArrayAdapter<ForwardingConfig> {
    final private ArrayList<ForwardingConfig> dataSet;
    MainActivity context;

    public ListAdapter(ArrayList<ForwardingConfig> data, MainActivity context) {
        super(context, R.layout.list_item, data);
        this.dataSet = data;
        this.context = context;
    }

    @Override
    public int getCount() {
        return this.dataSet.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View row = convertView;
        if (null == convertView) {
            row = inflater.inflate(R.layout.list_item, parent, false);
        }

        ForwardingConfig config = getItem(position);


        TextView phoneNumberText = row.findViewById(R.id.phone_number);
        phoneNumberText.setText(config.getPhoneNumber());

        View deleteButton = row.findViewById(R.id.delete_button);
        deleteButton.setTag(R.id.delete_button, position);
        deleteButton.setOnClickListener(this::onDeleteClick);


        View testButton = row.findViewById(R.id.test_button);
        testButton.setTag(R.id.test_button, position);
        testButton.setOnClickListener(view -> this.onTestButton(view, config));

        return row;
    }

    public void onTestButton(View view, ForwardingConfig config) {

        SmsReceiver receiver = new SmsReceiver();
        MessageInfo info = new MessageInfo();
        info.content = "SMS test fom SMS App OTP 000000";
        info.sim = "Test";
        info.sender = "000000000000";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            receiver.callWebHook(info, config);
        }
        Toast.makeText(this.getContext(),"Message sent: check the logs", Toast.LENGTH_LONG).show();
    }
    public void onDeleteClick(View view) {
        ListAdapter listAdapter = this;
        final int position = (int) view.getTag(R.id.delete_button);
        final ForwardingConfig config = listAdapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle(R.string.delete_record);
        String message = context.getString(R.string.confirm_delete);
        message = String.format(message, config.getPhoneNumber());
        builder.setMessage(message);

        builder.setPositiveButton(R.string.btn_delete, (dialog, id) -> {
            listAdapter.remove(config);
            config.remove();
            context.toggleButtons(true);

        });
        builder.setNegativeButton(R.string.btn_cancel, null);
        builder.show();


    }
}

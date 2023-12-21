package tech.bogomolov.incomingsmsgateway;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class LogAdapter extends ArrayAdapter<SmsLogModel> {
    final private ArrayList<SmsLogModel> dataSet;
    Context context;

    public LogAdapter(ArrayList<SmsLogModel> data, Context context) {
        super(context, R.layout.list_item, data);
        this.dataSet = data;
        this.context = context;
    }

    public void clearDataSet() {
        SmsLogModel.reset();
        dataSet.clear();
        notifyDataSetChanged();
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
            row = inflater.inflate(R.layout.log_item, parent, false);
        }

        SmsLogModel log = this.getItem(position);

        TextView numberText = row.findViewById(R.id.number);
        numberText.setText(log.phone);

        TextView messageText = row.findViewById(R.id.message);
        messageText.setText(log.message);

        if(!log.success) {
            messageText.setTextColor(context.getResources().getColor(android.R.color.holo_red_light));
        }


        return row;
    }

}

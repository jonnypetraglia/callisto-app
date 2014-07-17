package com.qweex.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.qweex.callisto.R;

import java.util.ArrayList;
import java.util.HashMap;


public class MultiChooserDialog {
    ListView listview;
    OnChosen callback;

    HashMap<String, Boolean> statuses;
    AlertDialog dialog;
    boolean atLeastOne;

    public MultiChooserDialog(Context context, String dialogTitle, OnChosen callback, boolean atLeastOne, String[] items) {
        this(context, dialogTitle, callback, atLeastOne, items, null);
    }

    // Constructor
    public MultiChooserDialog(Context context, String dialogTitle, OnChosen callback, boolean atLeastOne, String[] items, String[] selected) {
        this.callback = callback;
        this.atLeastOne = atLeastOne;

        // Load Items & Statuses
        this.statuses = new HashMap<String, Boolean>();
        for(String item : items)
            this.statuses.put(item, false);
        if(selected!=null)
            for(String item : selected) {
                Log.d("Callisto", "selected: " + item);
                this.statuses.put(item, true);
            }

        // Create the ListView
        listview = new ListView(context);
        listview.setAdapter(new ChooserAdapter(context, R.layout.chat_channel_dialog_item, android.R.id.checkbox, items));
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((CheckBox)view.findViewById(android.R.id.checkbox)).toggle();
            }
        });

        // Create the holder layoutfor the dialog
        LinearLayout ll = new LinearLayout(context);
        ll.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(listview);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(dialogTitle);
        builder.setView(ll);
        builder.setPositiveButton(android.R.string.ok, ok);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setOnCancelListener(cancel);
        dialog = builder.show();
        updateButton();
    }


    // Handles the user pressing "Ok" and finalizing the option
    private DialogInterface.OnClickListener ok = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int uhhhhhhhhh) {

            ArrayList<String> selected = new ArrayList<String>();

            for(String s : statuses.keySet())
                if(statuses.get(s))
                    selected.add(s);

            callback.onChosen(selected.toArray(new String[selected.size()]));
        }
    };

    // Sends an empty message to the callback if the user cancels
    private DialogInterface.OnCancelListener cancel = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialogInterface) {
            callback.onChosen(null);
        }
    };

    // Click an item
    private CompoundButton.OnCheckedChangeListener clickCheck = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            statuses.put(buttonView.getText().toString(), isChecked);
            updateButton();
        }
    };

    void updateButton() {
        if(!atLeastOne)
            return;

        for(Boolean b : statuses.values()) {
            if(b) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                return;
            }
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }


    class ChooserAdapter extends ArrayAdapter<String> {

        int resID, textID;

        public ChooserAdapter(Context context, int resource, int textViewResourceId, String[] objects) {
            super(context, resource, textViewResourceId, objects);
            resID = resource;
            textID = textViewResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = super.getView(position, convertView, parent);

            CheckBox checkBox = ((CheckBox)convertView.findViewById(android.R.id.checkbox));
            checkBox.setText(getItem(position));
            checkBox.setOnCheckedChangeListener(clickCheck);
            checkBox.setChecked(statuses.get(getItem(position)));

            return convertView;
        }
    }

    public interface OnChosen {
        public void onChosen(String[] selected);
    }
}


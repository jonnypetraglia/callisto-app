package com.qweex.callisto.chat;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ChatListAdapter extends ArrayAdapter<IrcMessage> {

    int layout_id;
    List<IrcMessage> messages;

    public ChatListAdapter(Context context, int resource, List<IrcMessage> objects) {
        super(context, resource, objects);
        layout_id = resource;
        messages = objects;
    }

    /** Inherited method to generate / recycle the view & inject data from relevant data row.
     * @param pos Position of the item in question of the collection.
     * @param inView View passed in that might be recycled.
     * @param parent ViewGroup that should be used as parent for inView.
     * @return The View with all data in it.
     */
    public View getView(int pos, View inView, ViewGroup parent)
    {
        View v = inView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(layout_id, null);
        }

        IrcMessage ircMessage = messages.get(pos);

        TextView time = (TextView) v.findViewById(android.R.id.text1),
                 title = (TextView) v.findViewById(android.R.id.title),
                 message  = (TextView) v.findViewById(android.R.id.message);

        time.setText(ircMessage.getTime());
        title.setText(ircMessage.getTitle());
        message.setText(ircMessage.getMessage());

        time.setTextColor(ircMessage.getTimeColor().val());
        title.setTextColor(ircMessage.getTitleColor().val());
        message.setTextColor(ircMessage.getMessageColor().val());
        message.setLinkTextColor(ircMessage.getLinkColors());

        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float)(IrcMessage.getTextSize() * 0.8));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, IrcMessage.getTextSize());
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, IrcMessage.getTextSize());

        return(v);
    }
}

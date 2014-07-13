package com.qweex.callisto.chat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class IrcMessage
{
    static SimpleDateFormat sdfTime = new SimpleDateFormat("'['HH:mm']'");

    // Note that Type is ONLY FOR COLORING. All other typing and whatnot should be handled by sirc. That's why I chose it.
    public enum Type {ACTION, MODE, CONNECTION, NOTICE, MOTD, MESSAGE, TOPIC, JOIN, KICK, PART}

    public enum UserMode {FOUNDER, ADMIN, OP, HALFOP, VOICE}

    CharSequence title, message;
    Calendar timestamp;
    Type type;


    public IrcMessage(String title, String message, Type type) {
        this(title, message, type, null);
    }

    public IrcMessage(String title, String message, Type type, Date time)
    {
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = Calendar.getInstance();
        if(time!=null)
            this.timestamp.setTime(time);
    }

    public Date getRawDate() {
        return timestamp.getTime();
    }

    public CharSequence getTime() {
        return sdfTime.format(timestamp.getTime());
    }

    public CharSequence getTitle() {
        return title;
    }

    public CharSequence getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return getTime() +
                (getTitle()!=null ? (" " + getTitle()) : "") +
                (getMessage()!=null ? (" " + getMessage()) : "");
    }
}
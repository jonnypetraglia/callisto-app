package com.qweex.callisto.chat;

import java.util.Calendar;

public class IrcMessage
{
    public enum Type {ACTION, MODE, CONNECTION, NOTICE}

    public enum UserMode {FOUNDER, ADMIN, OP, HALFOP, VOICE}

    CharSequence title, message;
    Calendar timestamp;
    Type type;


    public IrcMessage(String title, String message, Type type)
    {
        this.title = title;
        this.message = message;
        this.type = type;
        timestamp = Calendar.getInstance();
    }

    public CharSequence getTime() {
        return timestamp.toString();
    }

    public CharSequence getTitle() {
        return title;
    }

    public CharSequence getMessage() {
        return message;
    }
}
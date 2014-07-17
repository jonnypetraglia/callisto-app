package com.qweex.callisto.chat;

import android.content.res.ColorStateList;
import android.graphics.Color;
import com.qweex.callisto.PrefCache;
import com.qweex.callisto.R;
import com.sorcix.sirc.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class IrcMessage
{
    static SimpleDateFormat sdfTime = new SimpleDateFormat("'['HH:mm']'");

    // Note that Type is ONLY FOR COLORING. All other typing and whatnot should be handled by sirc. That's why I chose it.
    public enum Type {  ACTION,
                        CONNECTION,
                        ERROR,
                        FATAL,
                        JOIN,
                        KICK,
                        MESSAGE,
                        MOTD,
                        MODE,
                        NICK,
                        NOTICE,
                        PART,
                        QUIT,
                        TOPIC,
                        SEND
                     }

    public enum UserMode {FOUNDER, ADMIN, OP, HALFOP, VOICE}

    String title = null, message = null;
    User propogationUser;
    Calendar timestamp;
    Type type;


    public IrcMessage(String title, String message, Type type) {
        this(title, message, type, null, null);
    }

    public IrcMessage(String title, String message, Type type, User propogationUser) {
        this(title, message, type, null, propogationUser);
    }

    public IrcMessage(String title, String message, Type type, Date time) {
        this(title, message, type, time, null);
    }

    public IrcMessage(String title, String message, Type type, Date time, User propogationUser)
    {
        this.propogationUser = propogationUser;
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

    public int getTimeColor() {
        return PrefCache.clr("chat_color_time", R.color.chat_time, 0);
    }

    public int getTitleColor() {
        switch(type) {
            case SEND:
                return PrefCache.clr("chat_color_nick_me", R.color.chat_nick_me, 0);
            case MESSAGE:
                return PrefCache.clr("chat_color_nick", R.color.chat_nick, 0);
            default:
                return getMessageColor();
        }
    }

    public int getMessageColor() {
        switch(type) {
            case ACTION:
                return PrefCache.clr("chat_color_action", R.color.chat_action, 0);
            case CONNECTION:
                return PrefCache.clr("chat_color_connection", R.color.chat_connection, 0);
            case JOIN:
                return PrefCache.clr("chat_color_join", R.color.chat_join, 0);
            case KICK:
                return PrefCache.clr("chat_color_kick", R.color.chat_kick, 0);
            case MODE:
                return PrefCache.clr("chat_color_mode", R.color.chat_mode, 0);
            case NICK:
                return PrefCache.clr("chat_color_nick_change", R.color.chat_nick, 0);
            case NOTICE:
                return PrefCache.clr("chat_color_notice", R.color.chat_notice, 0);
            case PART:
            case QUIT:
                return PrefCache.clr("chat_color_quit", R.color.chat_quit, 0);
            case TOPIC:
            case MOTD:
                return PrefCache.clr("chat_color_topic", R.color.chat_topic, 0);

            case MESSAGE:
            default:
                return PrefCache.clr("chat_color_text", R.color.chat_text, 0);
        }
    }


    public ColorStateList getLinkColors() {
        return new ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_pressed},
                        new int[] { android.R.attr.state_focused},
                        new int[] {}
                },
                new int [] {
                        Color.BLUE,
                        0xFF000000 + PrefCache.clr("chat_color_link", R.color.chat_link, 0),
                        0xFF000000 + PrefCache.clr("chat_color_link", R.color.chat_link, 0),
                }
        );
    }

    public IrcMessage[] splitByNewline() {

        if(message==null)
            return new IrcMessage[] {this};

        String[] lines = message.toString().split("\n");
        IrcMessage[] result = new IrcMessage[lines.length];

        for(int i=0; i<lines.length; ++i)
            result[i] = new IrcMessage(title == null ? null : title.toString(), lines[i], type);

        return result;
    }

    @Override
    public String toString() {
        return getTime() +
                (getTitle()!=null ? (" " + getTitle()) : "") +
                (getMessage()!=null ? (" " + getMessage()) : "");
    }

    static public int getTextSize() {
        return PrefCache.inte("chat_text_size", R.integer.chat_text_size, 0);
    }
}
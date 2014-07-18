package com.qweex.callisto.chat;

import android.content.res.ColorStateList;
import com.qweex.callisto.PrefCache;
import com.qweex.callisto.R;
import com.qweex.utils.ResCache.Color;
import com.sorcix.sirc.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;


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

    public String getTime() {
        return sdfTime.format(timestamp.getTime());
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Color getTimeColor() {
        return PrefCache.clr("chat_color_time", R.color.chat_time, null);
    }

    public Color getTitleColor() {
        switch(type) {
            case SEND:
                return PrefCache.clr("chat_color_nick_me", R.color.chat_nick_me, null);
            case MESSAGE:
                if(PrefCache.bool("chat_colors", null, true))
                    return colorForNick(getTitle());
                else
                    return PrefCache.clr("chat_color_nick", R.color.chat_nick, null);
            default:
                return getMessageColor();
        }
    }

    public Color getMessageColor() {
        switch(type) {
            case ACTION:
                return PrefCache.clr("chat_color_action", R.color.chat_action, null);
            case CONNECTION:
                return PrefCache.clr("chat_color_connection", R.color.chat_connection, null);
            case JOIN:
                return PrefCache.clr("chat_color_join", R.color.chat_join, null);
            case KICK:
                return PrefCache.clr("chat_color_kick", R.color.chat_kick, null);
            case MODE:
                return PrefCache.clr("chat_color_mode", R.color.chat_mode, null);
            case NICK:
                return PrefCache.clr("chat_color_nick_change", R.color.chat_nick, null);
            case NOTICE:
                return PrefCache.clr("chat_color_notice", R.color.chat_notice, null);
            case PART:
            case QUIT:
                return PrefCache.clr("chat_color_quit", R.color.chat_quit, null);
            case TOPIC:
            case MOTD:
                return PrefCache.clr("chat_color_topic", R.color.chat_topic, null);

            case MESSAGE:
            default:
                return PrefCache.clr("chat_color_text", R.color.chat_text, null);
        }
    }


    public ColorStateList getLinkColors() {
        int linkClr = 0xFF000000 + PrefCache.clr("chat_color_link", R.color.chat_link, null).val();

        return new ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_pressed},
                        new int[] { android.R.attr.state_focused},
                        new int[] {}
                },
                new int [] {
                        android.graphics.Color.BLUE,
                        linkClr,
                        linkClr
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


    Color colorForNick(String nickname)
    {
        int nickValue = 0;

        for (int index = 0; index < nickname.length(); index++)
        {
            nickValue += nickname.charAt(index);
        }
        Set<Color> colorSet = PrefCache.clrSet("chat_nick_colors", R.array.chat_nick_colors, null);
        Color[] nickColors = colorSet.toArray(new Color[colorSet.size()]);
        return nickColors[nickValue % nickColors.length];
    }
}
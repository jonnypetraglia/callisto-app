/*
 * Copyright (C) 2012-2014 Qweex
 * This file is a part of Callisto.
 *
 * Callisto is free software; it is released under the
 * Open Software License v3.0 without warranty. The OSL is an OSI approved,
 * copyleft license, meaning you are free to redistribute
 * the source code under the terms of the OSL.
 *
 * You should have received a copy of the Open Software License
 * along with Callisto; If not, see <http://rosenlaw.com/OSL3.0-explained.htm>
 * or check OSI's website at <http://opensource.org/licenses/OSL-3.0>.
 */
package com.qweex.callisto.irc;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.qweex.callisto.R;
import com.qweex.utils.ArrayListWithMaximum;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adapter for IRCChat; really a subclass almost, I split it off to help unbloat the file
 * WARNING: Has no realy error checking to make sure you use it right;
 * i.e. you HAVE to set the reference to the IRCChat */
public class IrcAdapter<E> extends ArrayAdapter<E>
{
    /** Data containing the lines */
    ArrayListWithMaximum<E> data;
    /** Colors for the text */
    ColorStateList cls;
    /** Layout resource ID */
    int textViewResourceId;
    /** Reference to the IRCChat; used for (mostly) getReceived */
    IRCChat ircchat;

    /** Maps from emoticon to drawable resource */
    private static HashMap<String, Integer> smilyRegexMap = null;

    /** Real constructor; the others are all puppets for compatibility */
    void init(Context c, int i)
    {
        this.textViewResourceId = i;
        cls = new ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_pressed},
                        new int[] { android.R.attr.state_focused},
                        new int[] {}
                },
                new int [] {
                        Color.BLUE,
                        0xFF000000 + IRCChat.CLR_LINKS,
                        0xFF000000 + IRCChat.CLR_LINKS,
                }
        );
        // Create the smiley rap
        smilyRegexMap = new HashMap<String, Integer>();
        //smilyRegexMap.put( "<3", R.drawable.ic_action_heart);                  //  <3
        smilyRegexMap.put( ">:(D|\\))", R.drawable.ic_action_emo_evil);        //  >:D or >:)
        smilyRegexMap.put( ":-?\\)", R.drawable.ic_action_emo_basic);          //  :-) or :)
        smilyRegexMap.put( ">:-?(\\(|\\|)", R.drawable.ic_action_emo_angry);   //  >:| or >:( or >:-| or >:-(
        smilyRegexMap.put( "B-\\)", R.drawable.ic_action_emo_basic);           //  B-)
        smilyRegexMap.put( ":'-?\\(", R.drawable.ic_action_emo_cry);           //  :'-( or :'(
        smilyRegexMap.put( ":-?(\\\\|/)", R.drawable.ic_action_emo_err);       //  :\ or :/ or :-\ or :-/
        smilyRegexMap.put( ":-\\*", R.drawable.ic_action_emo_kiss);            //  :-*
        smilyRegexMap.put( ":-?D", R.drawable.ic_action_emo_laugh);            //  :-D or :D
        smilyRegexMap.put( "(x|X)D", R.drawable.ic_action_emo_laugh);          //  XD or xD
        smilyRegexMap.put( ":-?(\\(|\\[)", R.drawable.ic_action_emo_sad);      //  :( or :-( or :[ or :-[
        smilyRegexMap.put( ":-?S", R.drawable.ic_action_emo_shame);            //  :S or :-S
        smilyRegexMap.put( "(:|X|x)-?P", R.drawable.ic_action_emo_tongue);     //  :P or :-P or xP or XP or X-P or x-P
        smilyRegexMap.put( ";-?\\)", R.drawable.ic_action_emo_wink);           //  ;-) or ;)

        smilyRegexMap.put( ":-?(O|o)", R.drawable.ic_action_emo_wonder);       //  :O or :o or :-O or :-o
    }

    /** Set the reference; should be called IMMEDIATELY with the constructor */
    public IrcAdapter setIRCCHat(IRCChat i) { this.ircchat = i; return this; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if(convertView==null)
            convertView = ((LayoutInflater) ircchat.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(textViewResourceId, null, false);

        ((TextView)convertView).setText( parseEmoticons((Spanned) ircchat.getReceived((IRCChat.IrcMessage)data.get(position))) );
        ((TextView) convertView).setTextColor(0xff000000 + IRCChat.CLR_TEXT);
        ((TextView) convertView).setLinkTextColor(cls);
        return convertView;
    }

    /** parsesEmoticons if the preference is set; otherwise it just returns the input */
    public CharSequence parseEmoticons(Spanned s)
    {
        if(!PreferenceManager.getDefaultSharedPreferences(ircchat).getBoolean("irc_emoticons", false))
            return s;

        SpannableStringBuilder builder = new SpannableStringBuilder(s);

        @SuppressWarnings("rawtypes")
        Iterator it = smilyRegexMap.entrySet().iterator();
        while (it.hasNext())
        {
            @SuppressWarnings("rawtypes")
            Map.Entry pairs = (Map.Entry) it.next();
            Pattern mPattern = Pattern.compile((String) pairs.getKey(),Pattern.CASE_INSENSITIVE);
            Matcher matcher = mPattern.matcher(s);

            while (matcher.find())
            {
                Bitmap smiley = BitmapFactory.decodeResource(ircchat.getResources(), ((Integer) pairs.getValue()));
                Object[] spans = builder.getSpans(matcher.start(), matcher.end(), ImageSpan.class);
                if (spans == null || spans.length == 0)
                {
                    builder.setSpan(new ImageSpan(smiley), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return builder;
    }


    // Puppet constructors

    public IrcAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        init(context,textViewResourceId);
    }

    public IrcAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
        init(context,textViewResourceId);
    }

    public IrcAdapter(Context context, int textViewResourceId, E[] objects) {
        super(context, textViewResourceId, objects);
        init(context,textViewResourceId);
    }

    public IrcAdapter(Context context, int resource, int textViewResourceId, E[] objects) {
        super(context, resource, textViewResourceId, objects);
        init(context,textViewResourceId);
    }

    public IrcAdapter(Context context, int textViewResourceId, List<E> objects) {
        super(context, textViewResourceId, objects);
        data = (ArrayListWithMaximum<E>) objects;
        init(context,textViewResourceId);
    }

    public IrcAdapter(Context context, int resource, int textViewResourceId, List<E> objects) {
        super(context, resource, textViewResourceId, objects);
        data = (ArrayListWithMaximum<E>) objects;
        init(context,textViewResourceId);
    }

}

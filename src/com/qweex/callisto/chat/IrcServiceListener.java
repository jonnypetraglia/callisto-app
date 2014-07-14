package com.qweex.callisto.chat;

import android.text.TextUtils;
import android.util.Log;
import com.qweex.callisto.R;
import com.qweex.callisto.ResCache;
import com.sorcix.sirc.*;

/** Extension of sIRC that handles the IRC events, parses them, and passes them on to ChatFragment.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class IrcServiceListener implements ServerListener, MessageListener, ModeListener {
    String TAG = "Callisto:chat_tab:IrcServiceListener";

    ChatFragment chat;

    public IrcServiceListener(ChatFragment chatFragment) {
        this.chat = chatFragment;
    }


    ////////////////////////////////////////////////////////////////////
    ////////////////////////// Server actions //////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override
    public void onConnect(IrcConnection irc) {
        chat.receive(irc, new IrcMessage(
                ResCache.str(R.string.connected),
                null,
                IrcMessage.Type.CONNECTION
        ), false);
    }

    @Override
    public void onDisconnect(IrcConnection irc) {
        chat.receive(irc, new IrcMessage(
                ResCache.str(R.string.disconnected),
                null,
                IrcMessage.Type.CONNECTION
        ), true);
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, String message) {
        chat.receive(irc, new IrcMessage(
                ResCache.str(R.string.notice, sender.getNick()),
                message,
                IrcMessage.Type.NOTICE
        ), true);
    }

    @Override
    public void onMotd(IrcConnection irc, String motd) {
        chat.receive(irc, new IrcMessage(
                null,
                motd,
                IrcMessage.Type.MOTD
        ), false);
    }


    //////////////////////////////////////////////////////////////////////
    ////////////////////////// Private Messages //////////////////////////
    //////////////////////////////////////////////////////////////////////

    @Override
    public void onAction(IrcConnection irc, User sender, String action) {
        chat.receive(sender, new IrcMessage(
                sender.getNick(),
                action,
                IrcMessage.Type.ACTION
        ));
    }

    @Override
    public void onPrivateMessage(IrcConnection irc, User sender, String message) {
        chat.receive(sender, new IrcMessage(
                sender.getNick(),
                message,
                IrcMessage.Type.MESSAGE
        ));
    }


    //////////////////////////////////////////////////////////////////////
    ////////////////////////// Channel Messages //////////////////////////
    //////////////////////////////////////////////////////////////////////

    @Override
    public void onAction(IrcConnection irc, User sender, Channel target, String action) {
        chat.receive(target, new IrcMessage(
                sender.getNick(),
                action,
                IrcMessage.Type.ACTION
        ));
    }

    @Override
    public void onMessage(IrcConnection irc, User sender, Channel target, String message) {
        chat.receive(target, new IrcMessage(
                sender.getNick(),
                message,
                IrcMessage.Type.MESSAGE
        ));
    }


    ////////////////////////////////////////////////////////////////////
    ////////////////////////// Channel Events //////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override
    public void onTopic(IrcConnection irc, Channel channel, User sender, String topic) {

        Log.d(TAG, "Topic set by " + sender);
        String msg = ResCache.str(R.string.topic, topic, sender==null ? "unknown" : sender.getNick());

        IrcMessage imsg = new IrcMessage(
                null,
                "*** " + msg,
                IrcMessage.Type.ACTION
        );
        chat.receive(channel, imsg);
    }

    @Override
    public void onJoin(IrcConnection irc, Channel channel, User user) {
        String msg;
        if(user.isUs())
            msg = " *** " + ResCache.str(R.string.join_me);
        else
            msg = " *** " + ResCache.str(R.string.join, user.getNick());
        chat.receive(channel, new IrcMessage(
                null,
                msg,
                IrcMessage.Type.JOIN
        ));
    }

    @Override
    public void onKick(IrcConnection irc, Channel channel, User sender, User user, String message) {
        String msg;
        if(sender.isUs())
            msg = ResCache.str(R.string.kick_me_active, user.getNick(), message);
        else if(user.isUs())
            msg = ResCache.str(R.string.kick_me_passive, sender.getNick(), message);
        else
            msg = ResCache.str(R.string.kick, sender.getNick(), user.getNick(), message);

        chat.receive(channel, new IrcMessage(
                null,
                " *** " + msg,
                IrcMessage.Type.KICK
        ));
    }

    @Override
    public void onPart(IrcConnection irc, Channel channel, User user, String message) {
        String msg;
        if(user.isUs())
            msg = ResCache.str(R.string.part_me, message);
        else
            msg = ResCache.str(R.string.part, user.getNick(),message);

        chat.receive(channel, new IrcMessage(
                null,
                " *** " + msg,
                IrcMessage.Type.KICK
        ));
    }

    // More adequately onChannelMode
    @Override
    public void onMode(IrcConnection irc, Channel channel, User sender, String mode) {
        String msg;
        if(sender.isUs())
            msg = ResCache.str(R.string.mode_channel_me, sender.getNick(), mode);
        else
            msg = ResCache.str(R.string.mode_channel, sender.getNick(), mode);

        chat.receive(channel, new IrcMessage(
                null,
                " *** " + msg,
                IrcMessage.Type.KICK
        ));
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, Channel target, String message) {
        chat.receive(target, new IrcMessage(
                ResCache.str(R.string.notice, sender.getNick()),
                message,
                IrcMessage.Type.NOTICE
        ));
    }


    ///////////////////////////////////////////////////////////////////////////////
    //////////////// User Mode Changes (subtype of Channel Events) ////////////////
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public void onFounder(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(channel, sender, user, IrcMessage.UserMode.FOUNDER, true);
    }

    @Override
    public void onDeFounder(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(channel, sender, user, IrcMessage.UserMode.FOUNDER, false);
    }

    @Override
    public void onAdmin(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(channel, sender, user, IrcMessage.UserMode.ADMIN, true);
    }

    @Override
    public void onDeAdmin(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(channel, sender, user, IrcMessage.UserMode.ADMIN, false);
    }

    @Override
    public void onOp(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(channel, sender, user, IrcMessage.UserMode.OP, true);
    }

    @Override
    public void onDeOp(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(channel, sender, user, IrcMessage.UserMode.OP, false);
    }

    @Override
    public void onHalfop(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(channel, sender, user, IrcMessage.UserMode.HALFOP, true);
    }

    @Override
    public void onDeHalfop(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(channel, sender, user, IrcMessage.UserMode.HALFOP, false);
    }

    @Override
    public void onVoice(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(channel, sender, user, IrcMessage.UserMode.VOICE, true);
    }

    @Override
    public void onDeVoice(IrcConnection irc, Channel channel, User sender, User user) {
        handleModeChange(channel, sender, user, IrcMessage.UserMode.VOICE, false);
    }


    /////////////////////////////////////////////////////////////////////////
    ////////////////////////// Propogatable Events //////////////////////////
    /////////////////////////////////////////////////////////////////////////

    @Override
    public void onNick(IrcConnection irc, User oldUser, User newUser) {
        //TODO // Need to check every active channel if it applies
        
    }

    @Override
    public void onQuit(IrcConnection irc, User user, String message) {
        //TODO // Need to check every active channel if it applies
        
    }

    //////////////////////////////////////////////////////////
    ////////////////////////// Misc //////////////////////////
    //////////////////////////////////////////////////////////

    @Override
    public void onCtcpReply(IrcConnection irc, User sender, String command, String message) {
        //TODO
        
    }


    @Override
    public void onInvite(IrcConnection irc, User sender, User user, Channel channel) {
        //TODO
        
    }


    //////////////////////////////////////////////////////////////
    ////////////////////////// Non-sIRC //////////////////////////
    //////////////////////////////////////////////////////////////


    // Mode Change
    public void handleModeChange(Channel channel, User setBy, User setTarget, IrcMessage.UserMode mode, boolean given) {
        String msg;
        String modeString = mode.toString();
        modeString = modeString.charAt(0) + modeString.substring(1).toLowerCase();
        if(given) {
            if(setBy.isUs())
                msg = ResCache.str(R.string.mode_set_me_active, modeString, setTarget.getNick());
            else if(setTarget.isUs())
                msg = ResCache.str(R.string.mode_set_me_passive, setBy.getNick(), modeString);
            else
                msg = ResCache.str(R.string.mode_set, setBy.getNick(), modeString, setTarget.getNick());
        } else {
            if(setBy.isUs())
                msg = ResCache.str(R.string.mode_unset_me_active, modeString, setTarget.getNick());
            else if(setTarget.isUs())
                msg = ResCache.str(R.string.mode_unset_me_passive, setBy.getNick(), modeString);
            else
                msg = ResCache.str(R.string.mode_unset, setBy.getNick(), modeString, setTarget.getNick());
        }

        Log.d(TAG, msg);

        chat.receive(channel, new IrcMessage(
                msg,
                null,
                IrcMessage.Type.MODE
        ));
    }
}

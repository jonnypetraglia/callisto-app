package com.qweex.callisto.chat;

import android.util.Log;
import com.qweex.callisto.R;
import com.qweex.callisto.ResCache;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.User;

/** Extension of sIRC that handles the IRC events, parses them, and passes them on to ChatFragment.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class IrcServiceAdapter extends com.sorcix.sirc.IrcAdaptor {
    String TAG = "Callisto:chat_tab:IrcServiceAdapter";

    ChatFragment chat;

    public IrcServiceAdapter(ChatFragment chatFragment) {
        this.chat = chatFragment;
    }


    ////////////////////////////////////////////////////////////////////
    ////////////////////////// Server actions //////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override
    public void onConnect(IrcConnection irc) {
        super.onConnect(irc);
        chat.receive(irc, new IrcMessage(
                ResCache.str(R.string.connected, irc.getServer().getAddress()),
                null,
                IrcMessage.Type.CONNECTION
        ), false);
    }

    @Override
    public void onDisconnect(IrcConnection irc) {
        super.onDisconnect(irc);
        chat.receive(irc, new IrcMessage(
                ResCache.str(R.string.disconnected),
                null,
                IrcMessage.Type.CONNECTION
        ), false);
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, String message) {
        super.onNotice(irc, sender, message);
        chat.receive(irc, new IrcMessage(
                ResCache.str(R.string.notice, getNickOrMe(sender)),
                message,
                IrcMessage.Type.NOTICE
        ), true);
    }

    @Override
    public void onMotd(IrcConnection irc, String motd) {
        super.onMotd(irc, motd);
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
        super.onAction(irc, sender, action);
        chat.receive(sender, new IrcMessage(
                sender.getNick(),
                action,
                IrcMessage.Type.ACTION
        ));
    }

    @Override
    public void onPrivateMessage(IrcConnection irc, User sender, String message) {
        super.onPrivateMessage(irc, sender, message);
        chat.receive(sender, new IrcMessage(
                getNickOrMe(sender),
                message,
                IrcMessage.Type.MESSAGE
        ));
    }


    //////////////////////////////////////////////////////////////////////
    ////////////////////////// Channel Messages //////////////////////////
    //////////////////////////////////////////////////////////////////////

    @Override
    public void onAction(IrcConnection irc, User sender, Channel target, String action) {
        super.onAction(irc, sender, target, action);
        chat.receive(target, new IrcMessage(
                getNickOrMe(sender),
                action,
                IrcMessage.Type.ACTION
        ));
    }

    @Override
    public void onMessage(IrcConnection irc, User sender, Channel target, String message) {
        super.onMessage(irc, sender, target, message);
        chat.receive(target, new IrcMessage(
                getNickOrMe(sender),
                message,
                IrcMessage.Type.MESSAGE
        ));
    }


    ////////////////////////////////////////////////////////////////////
    ////////////////////////// Channel Events //////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override
    public void onTopic(IrcConnection irc, Channel channel, User sender, String topic) {
        super.onTopic(irc, channel, sender, topic);
        chat.receive(channel, new IrcMessage(
                null,
                "*** " + ResCache.str(R.string.topic, topic, sender.getNick()),
                IrcMessage.Type.ACTION
        ));
    }

    @Override
    public void onJoin(IrcConnection irc, Channel channel, User user) {
        super.onJoin(irc, channel, user);
        chat.receive(channel, new IrcMessage(
                null,
                " *** " + ResCache.str(R.string.join, getNickOrMe(user), user.getHostName()),
                IrcMessage.Type.JOIN
        ));
    }

    @Override
    public void onKick(IrcConnection irc, Channel channel, User sender, User user, String message) {
        super.onKick(irc, channel, sender, user, message);
        chat.receive(channel, new IrcMessage(
                null,
                " *** " + ResCache.str(R.string.kick, getNickOrMe(sender), getNickOrMe(user), message),
                IrcMessage.Type.KICK
        ));
    }

    @Override
    public void onPart(IrcConnection irc, Channel channel, User user, String message) {
        super.onPart(irc, channel, user, message);
        chat.receive(channel, new IrcMessage(
                null,
                " *** " + ResCache.str(R.string.part, getNickOrMe(user), message),
                IrcMessage.Type.KICK
        ));
    }

    // More adequately onChannelMode
    @Override
    public void onMode(IrcConnection irc, Channel channel, User sender, String mode) {
        super.onMode(irc, channel, sender, mode);
        chat.receive(channel, new IrcMessage(
                null,
                " *** " + ResCache.str(R.string.mode, getNickOrMe(sender), mode),
                IrcMessage.Type.KICK
        ));
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, Channel target, String message) {
        super.onNotice(irc, sender, target, message);
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
        super.onFounder(irc, channel, sender, user);
        handleModeChange(channel, sender, user, IrcMessage.UserMode.FOUNDER, true);
    }

    @Override
    public void onDeFounder(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeFounder(irc, channel, sender, user);
        handleModeChange(channel, sender, user, IrcMessage.UserMode.FOUNDER, false);
    }

    @Override
    public void onAdmin(IrcConnection irc, Channel channel, User sender, User user) {
        super.onAdmin(irc, channel, sender, user);
        handleModeChange(channel, sender, user, IrcMessage.UserMode.ADMIN, true);
    }

    @Override
    public void onDeAdmin(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeAdmin(irc, channel, sender, user);
        handleModeChange(channel, sender, user, IrcMessage.UserMode.ADMIN, false);
    }

    @Override
    public void onOp(IrcConnection irc, Channel channel, User sender, User user) {
        super.onOp(irc, channel, sender, user);
        handleModeChange(channel, sender, user, IrcMessage.UserMode.OP, true);
    }

    @Override
    public void onDeOp(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeOp(irc, channel, sender, user);
        handleModeChange(channel, sender, user, IrcMessage.UserMode.OP, false);
    }

    @Override
    public void onHalfop(IrcConnection irc, Channel channel, User sender, User user) {
        super.onHalfop(irc, channel, sender, user);
        handleModeChange(channel, sender, user, IrcMessage.UserMode.HALFOP, true);
    }

    @Override
    public void onDeHalfop(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeHalfop(irc, channel, sender, user);
        handleModeChange(channel, sender, user, IrcMessage.UserMode.HALFOP, false);
    }

    @Override
    public void onVoice(IrcConnection irc, Channel channel, User sender, User user) {
        super.onVoice(irc, channel, sender, user);
        handleModeChange(channel, sender, user, IrcMessage.UserMode.VOICE, true);
    }

    @Override
    public void onDeVoice(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeVoice(irc, channel, sender, user);
        handleModeChange(channel, sender, user, IrcMessage.UserMode.VOICE, false);
    }


    /////////////////////////////////////////////////////////////////////////
    ////////////////////////// Propogatable Events //////////////////////////
    /////////////////////////////////////////////////////////////////////////

    @Override
    public void onNick(IrcConnection irc, User oldUser, User newUser) {
        super.onNick(irc, oldUser, newUser);
        //TODO // Need to check every active channel if it applies
        
    }

    @Override
    public void onQuit(IrcConnection irc, User user, String message) {
        super.onQuit(irc, user, message);
        //TODO // Need to check every active channel if it applies
        
    }

    //////////////////////////////////////////////////////////
    ////////////////////////// Misc //////////////////////////
    //////////////////////////////////////////////////////////

    @Override
    public void onCtcpReply(IrcConnection irc, User sender, String command, String message) {
        super.onCtcpReply(irc, sender, command, message);
        //TODO
        
    }


    @Override
    public void onInvite(IrcConnection irc, User sender, User user, Channel channel) {
        super.onInvite(irc, sender, user, channel);
        //TODO
        
    }


    //////////////////////////////////////////////////////////////
    ////////////////////////// Non-sIRC //////////////////////////
    //////////////////////////////////////////////////////////////


    // Mode Change
    public void handleModeChange(Channel channel, User setBy, User setTarget, IrcMessage.UserMode mode, boolean given) {
        String verb = "set";
        if(!given)
            verb = "unset";
        String msg = setBy.getNick() + " " + verb + " mode " + mode + " on " + setTarget.getNick();

        Log.d(TAG, msg);

        chat.receive(channel, new IrcMessage(
                msg,
                null,
                IrcMessage.Type.MODE
        ));
    }

    String getNickOrMe(User user) {
        if(user.isUs())
            return ResCache.str(R.string.you);
        return user.getNick();
    }
}

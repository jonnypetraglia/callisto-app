package com.qweex.callisto.chat;

import android.util.Log;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.User;

public class IrcServiceAdapter extends com.sorcix.sirc.IrcAdaptor {
    String TAG = "Callisto:chat_tab:IrcServiceAdapter";

    ChatFragment chatFragment;

    public IrcServiceAdapter(ChatFragment chatFragment) {
        this.chatFragment = chatFragment;
    }


    ////////////////////////////////////////////////////////////////////
    ////////////////////////// Server actions //////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override
    public void onConnect(IrcConnection irc) {
        super.onConnect(irc);
        appendServerMessage(irc, "Connected to " + irc.getServer().getAddress());
        Log.i(TAG, "onConnect");
    }

    @Override
    public void onDisconnect(IrcConnection irc) {
        super.onDisconnect(irc);
        appendServerMessage(irc, "Disconnected from " + irc.getServer().getAddress());
        Log.i(TAG, "onDisconnect");
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, String message) {
        super.onNotice(irc, sender, message);
        appendServerMessage(irc, sender, message, IrcMessage.Type.NOTICE);
        Log.i(TAG, "onNotice2: " + message);
    }


    //////////////////////////////////////////////////////////////////////
    ////////////////////////// Private Messages //////////////////////////
    //////////////////////////////////////////////////////////////////////

    @Override
    public void onAction(IrcConnection irc, User sender, String action) {
        super.onAction(irc, sender, action);
        //TODO
        Log.i(TAG, "onAction2: " + action);
    }

    @Override
    public void onPrivateMessage(IrcConnection irc, User sender, String message) {
        super.onPrivateMessage(irc, sender, message);
        //TODO
        Log.i(TAG, "onPrivateMessage: " + message);
    }


    //////////////////////////////////////////////////////////////////////
    ////////////////////////// Channel Messages //////////////////////////
    //////////////////////////////////////////////////////////////////////

    @Override
    public void onAction(IrcConnection irc, User sender, Channel target, String action) {
        super.onAction(irc, sender, target, action);
        //TODO
        Log.i(TAG, "onAction1: " + action);
    }

    @Override
    public void onMessage(IrcConnection irc, User sender, Channel target, String message) {
        super.onMessage(irc, sender, target, message);
        //TODO
        Log.i(TAG, "onMessage: " + sender.getNick() + " - " + message);
    }


    ////////////////////////////////////////////////////////////////////
    ////////////////////////// Channel Events //////////////////////////
    ////////////////////////////////////////////////////////////////////

    @Override
    public void onTopic(IrcConnection irc, Channel channel, User sender, String topic) {
        super.onTopic(irc, channel, sender, topic);
        //TODO
        Log.i(TAG, "onTopic: " + topic);
    }

    @Override
    public void onJoin(IrcConnection irc, Channel channel, User user) {
        super.onJoin(irc, channel, user);
        //TODO
        Log.i(TAG, "onJoin: " + user.getNick());
    }

    @Override
    public void onKick(IrcConnection irc, Channel channel, User sender, User user, String message) {
        super.onKick(irc, channel, sender, user, message);
        //TODO
        Log.i(TAG, "onKick: " + user.getNick());
    }

    @Override
    public void onMotd(IrcConnection irc, String motd) {
        super.onMotd(irc, motd);
        //TODO
        Log.i(TAG, "onMotd: " + motd);
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, Channel target, String message) {
        super.onNotice(irc, sender, target, message);
        //TODO
        Log.i(TAG, "onNotice1: " + message);
    }

    @Override
    public void onPart(IrcConnection irc, Channel channel, User user, String message) {
        super.onPart(irc, channel, user, message);
        //TODO
        Log.i(TAG, "onPart: " + user.getNick() + " [[ " + message);
    }

    // More adequately onChannelMode
    @Override
    public void onMode(IrcConnection irc, Channel channel, User sender, String mode) {
        super.onMode(irc, channel, sender, mode);
        //TODO
        Log.i(TAG, "onMode: " + mode);
    }


    ///////////////////////////////////////////////////////////////////////////////
    //////////////// User Mode Changes (subtype of Channel Events) ////////////////
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    public void onFounder(IrcConnection irc, Channel channel, User sender, User user) {
        super.onFounder(irc, channel, sender, user);
        appendModeChange(channel, sender, user, IrcMessage.UserMode.FOUNDER, true);
        Log.i(TAG, "onFounder: " + user.getNick());
    }

    @Override
    public void onDeFounder(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeFounder(irc, channel, sender, user);
        appendModeChange(channel, sender, user, IrcMessage.UserMode.FOUNDER, false);
        Log.i(TAG, "onDeFounder: " + user.getNick());
    }

    @Override
    public void onAdmin(IrcConnection irc, Channel channel, User sender, User user) {
        super.onAdmin(irc, channel, sender, user);
        appendModeChange(channel, sender, user, IrcMessage.UserMode.ADMIN, true);
        Log.i(TAG, "onAdmin: " + user.getNick());
    }

    @Override
    public void onDeAdmin(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeAdmin(irc, channel, sender, user);
        appendModeChange(channel, sender, user, IrcMessage.UserMode.ADMIN, false);
        Log.i(TAG, "onDeAdmin: " + user.getNick());
    }

    @Override
    public void onOp(IrcConnection irc, Channel channel, User sender, User user) {
        super.onOp(irc, channel, sender, user);
        Log.i(TAG, "onOp: " + user.getNick());
    }

    @Override
    public void onDeOp(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeOp(irc, channel, sender, user);
        appendModeChange(channel, sender, user, IrcMessage.UserMode.OP, false);
        Log.i(TAG, "onDeOp: " + user.getNick());
    }

    @Override
    public void onHalfop(IrcConnection irc, Channel channel, User sender, User user) {
        super.onHalfop(irc, channel, sender, user);
        appendModeChange(channel, sender, user, IrcMessage.UserMode.HALFOP, true);
        Log.i(TAG, "onHalfop: " + user.getNick());
    }

    @Override
    public void onDeHalfop(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeHalfop(irc, channel, sender, user);
        appendModeChange(channel, sender, user, IrcMessage.UserMode.HALFOP, false);
        Log.i(TAG, "onDeHalfop: " + user.getNick());
    }

    @Override
    public void onVoice(IrcConnection irc, Channel channel, User sender, User user) {
        super.onVoice(irc, channel, sender, user);
        appendModeChange(channel, sender, user, IrcMessage.UserMode.VOICE, true);
        Log.i(TAG, "onVoice: " + user.getNick());
    }

    @Override
    public void onDeVoice(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeVoice(irc, channel, sender, user);
        appendModeChange(channel, sender, user, IrcMessage.UserMode.VOICE, false);
        Log.i(TAG, "onDeVoice: " + user.getNick());
    }


    /////////////////////////////////////////////////////////////////////////
    ////////////////////////// Propogatable Events //////////////////////////
    /////////////////////////////////////////////////////////////////////////

    @Override
    public void onNick(IrcConnection irc, User oldUser, User newUser) {
        super.onNick(irc, oldUser, newUser);
        //TODO // Need to check every active channel if it applies
        Log.i(TAG, "onNick: " + oldUser.getNick() + "->" + newUser.getNick());
    }

    @Override
    public void onQuit(IrcConnection irc, User user, String message) {
        super.onQuit(irc, user, message);
        //TODO // Need to check every active channel if it applies
        Log.i(TAG, "onQui: " + user.getNick() + " [[ " + message);
    }

    //////////////////////////////////////////////////////////
    ////////////////////////// Misc //////////////////////////
    //////////////////////////////////////////////////////////

    @Override
    public void onCtcpReply(IrcConnection irc, User sender, String command, String message) {
        super.onCtcpReply(irc, sender, command, message);
        //TODO
        Log.i(TAG, "onCtcpReply: " + command + " " + message);
    }


    @Override
    public void onInvite(IrcConnection irc, User sender, User user, Channel channel) {
        super.onInvite(irc, sender, user, channel);
        //TODO
        Log.i(TAG, "onInvite: " + user.getNick());
    }


    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////


    /////////// Generalized Functions that are reused ///////////

    void appendServerMessage(IrcConnection ircConnection, String msg) {
        Log.d(TAG, "appendServerMessage: " + msg);

        chatFragment.receive(ircConnection, new IrcMessage(
                msg,
                null,
                IrcMessage.Type.CONNECTION
        ), false);
    }

    void appendServerMessage(IrcConnection ircConnection, User user, String msg, IrcMessage.Type type) {
        Log.d(TAG, "appendServerMessage: " + msg + " by " + user.getNick());

        chatFragment.receive(ircConnection, new IrcMessage(
                type + " by " + user.getNick(),
                msg,
                IrcMessage.Type.CONNECTION
        ), true);
    }


    // Mode Change
    void appendModeChange(Channel channel, User setBy, User setTarget, IrcMessage.UserMode mode, boolean given) {
        String verb = "set";
        if(!given)
            verb = "unset";
        String msg = setBy.getNick() + " " + verb + " mode " + mode + " on " + setTarget.getNick();

        Log.d(TAG, msg);

        chatFragment.receive(channel, new IrcMessage(
                msg,
                null,
                IrcMessage.Type.MODE
        ));
    }
}

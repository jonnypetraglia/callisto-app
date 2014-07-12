package com.qweex.callisto.chat;

import android.util.Log;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.User;

public class IrcServiceAdapter extends com.sorcix.sirc.IrcAdaptor {
    String TAG = "Callisto:chat:IrcServiceAdapter";

    @Override
    public void onAction(IrcConnection irc, User sender, Channel target, String action) {
        super.onAction(irc, sender, target, action);
        Log.i(TAG, "onAction1: " + action);
    }

    @Override
    public void onAction(IrcConnection irc, User sender, String action) {
        super.onAction(irc, sender, action);
        Log.i(TAG, "onAction2: " + action);
    }

    @Override
    public void onAdmin(IrcConnection irc, Channel channel, User sender, User user) {
        super.onAdmin(irc, channel, sender, user);
        Log.i(TAG, "onAdmin: " + user.getNick());
    }

    @Override
    public void onConnect(IrcConnection irc) {
        super.onConnect(irc);
        Log.i(TAG, "onConnect");
    }

    @Override
    public void onCtcpReply(IrcConnection irc, User sender, String command, String message) {
        super.onCtcpReply(irc, sender, command, message);
        Log.i(TAG, "onCtcpReply: " + command + " " + message);
    }

    @Override
    public void onDeAdmin(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeAdmin(irc, channel, sender, user);
        Log.i(TAG, "onDeAdmin: " + user.getNick());
    }

    @Override
    public void onDeFounder(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeFounder(irc, channel, sender, user);
        Log.i(TAG, "onDeFounder: " + user.getNick());
    }

    @Override
    public void onDeHalfop(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeHalfop(irc, channel, sender, user);
        Log.i(TAG, "onDeHalfop: " + user.getNick());
    }

    @Override
    public void onDeOp(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeOp(irc, channel, sender, user);
        Log.i(TAG, "onDeOp: " + user.getNick());
    }

    @Override
    public void onDeVoice(IrcConnection irc, Channel channel, User sender, User user) {
        super.onDeVoice(irc, channel, sender, user);
        Log.i(TAG, "onDeVoice: " + user.getNick());
    }

    @Override
    public void onDisconnect(IrcConnection irc) {
        super.onDisconnect(irc);
        Log.i(TAG, "onDisconnect");
    }

    @Override
    public void onFounder(IrcConnection irc, Channel channel, User sender, User user) {
        super.onFounder(irc, channel, sender, user);
        Log.i(TAG, "onFounder: " + user.getNick());
    }

    @Override
    public void onHalfop(IrcConnection irc, Channel channel, User sender, User user) {
        super.onHalfop(irc, channel, sender, user);
        Log.i(TAG, "onHalfop: " + user.getNick());
    }

    @Override
    public void onInvite(IrcConnection irc, User sender, User user, Channel channel) {
        super.onInvite(irc, sender, user, channel);
        Log.i(TAG, "onInvite: " + user.getNick());
    }

    @Override
    public void onJoin(IrcConnection irc, Channel channel, User user) {
        super.onJoin(irc, channel, user);
        Log.i(TAG, "onJoin: " + user.getNick());
    }

    @Override
    public void onKick(IrcConnection irc, Channel channel, User sender, User user, String message) {
        super.onKick(irc, channel, sender, user, message);
        Log.i(TAG, "onKick: " + user.getNick());
    }

    @Override
    public void onMessage(IrcConnection irc, User sender, Channel target, String message) {
        super.onMessage(irc, sender, target, message);
        Log.i(TAG, "onMessage: " + sender.getNick() + " - " + message);
    }

    @Override
    public void onMode(IrcConnection irc, Channel channel, User sender, String mode) {
        super.onMode(irc, channel, sender, mode);
        Log.i(TAG, "onMode: " + mode);
    }

    @Override
    public void onMotd(IrcConnection irc, String motd) {
        super.onMotd(irc, motd);
        Log.i(TAG, "onMotd: " + motd);
    }

    @Override
    public void onNick(IrcConnection irc, User oldUser, User newUser) {
        super.onNick(irc, oldUser, newUser);
        Log.i(TAG, "onNick: " + oldUser.getNick() + "->" + newUser.getNick());
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, Channel target, String message) {
        super.onNotice(irc, sender, target, message);
        Log.i(TAG, "onNotice1: " + message);
    }

    @Override
    public void onNotice(IrcConnection irc, User sender, String message) {
        super.onNotice(irc, sender, message);
        Log.i(TAG, "onNotice2: " + message);
    }

    @Override
    public void onOp(IrcConnection irc, Channel channel, User sender, User user) {
        super.onOp(irc, channel, sender, user);
        Log.i(TAG, "onOp: " + user.getNick());
    }

    @Override
    public void onPart(IrcConnection irc, Channel channel, User user, String message) {
        super.onPart(irc, channel, user, message);
        Log.i(TAG, "onPart: " + user.getNick() + " [[ " + message);
    }

    @Override
    public void onPrivateMessage(IrcConnection irc, User sender, String message) {
        super.onPrivateMessage(irc, sender, message);
        Log.i(TAG, "onPrivateMessage: " + message);
    }

    @Override
    public void onQuit(IrcConnection irc, User user, String message) {
        super.onQuit(irc, user, message);
        Log.i(TAG, "onQui: " + user.getNick() + " [[ " + message);
    }

    @Override
    public void onTopic(IrcConnection irc, Channel channel, User sender, String topic) {
        super.onTopic(irc, channel, sender, topic);
        Log.i(TAG, "onTopic: " + topic);
    }

    @Override
    public void onVoice(IrcConnection irc, Channel channel, User sender, User user) {
        super.onVoice(irc, channel, sender, user);
        Log.i(TAG, "onVoice: " + user.getNick());
    }
}

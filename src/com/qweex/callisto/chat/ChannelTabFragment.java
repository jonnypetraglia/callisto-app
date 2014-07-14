package com.qweex.callisto.chat;


import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;
import com.qweex.callisto.ResCache;
import com.sorcix.sirc.Channel;
import com.sorcix.sirc.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ChannelTabFragment extends TabFragment {
    String TAG = super.TAG + ":Channel";

    Channel channel;

    public ChannelTabFragment(MasterActivity master, Channel channel) {
        super(master);
        Log.v(TAG, "Creating Channel Fragment");
        this.channel = channel;
    }

    @Override
    void send(String msg) {
        channel.sendMessage(msg);
        receive(new IrcMessage(
                channel.getUs().getNick(),
                msg,
                IrcMessage.Type.MESSAGE
        ));
    }

    @Override
    void log(IrcMessage ircMessage) {
        String date = sdfLog.format(ircMessage.getRawDate());
        File STORAGE_DIR = Environment.getExternalStorageDirectory();

        String[] logLocation = new String[] {
                    LOG_SUBDIR,
                    ResCache.str(R.string.channels),
                    channel.getName(),
                    date + ".txt"
            };

        try {
            File logfile = new File(STORAGE_DIR, TextUtils.join(File.separator, logLocation));

            Log.d(TAG, "Writing to " + logfile.getPath() + ":" + ircMessage.toString());
            FileWriter writer = new FileWriter(logfile, true);
            writer.write(ircMessage.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file: " + e.getMessage());
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}

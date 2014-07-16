package com.qweex.callisto.chat;


import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;
import com.qweex.utils.ResCache;
import com.sorcix.sirc.IrcConnection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ServerTabFragment extends TabFragment{
    String TAG = super.TAG + ":Server";

    IrcConnection server = null;

    public ServerTabFragment(MasterActivity master, IrcConnection server) {
        super(master);
        Log.v(TAG, "Creating Server Fragment");
        this.server = server;
    }

    @Override
    void send(String msg) {
        server.sendRaw(msg);
    }

    @Override
    void log(IrcMessage ircMessage) {
        String date = sdfLog.format(ircMessage.getRawDate());
        File STORAGE_DIR = Environment.getExternalStorageDirectory();

        String[] logLocation = new String[] {
                    LOG_SUBDIR,
                    ResCache.str(R.string.server),
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

package com.qweex.callisto.catalog;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.loopj.android.image.SmartImage;
import com.loopj.android.image.SmartImageView;
import com.loopj.android.image.WebImage;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;

import java.text.SimpleDateFormat;

public class EpisodeFragment extends CallistoFragment {

    SimpleDateFormat sdf = new SimpleDateFormat("cccc, MMM d, yyyy");

    RelativeLayout layout;
    AbbrevLinearLayout buttonsLocal;
    Episode episode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        if(layout==null) {
            layout = (RelativeLayout) inflater.inflate(R.layout.episode, null);

            ((TextView)layout.findViewById(R.id.date)).setText(sdf.format(episode.Date.getTime()));
            ((TextView)layout.findViewById(R.id.desc)).setText(episode.Desc);

            if(episode.Image!=null) {
                WebImage wm = new WebImage(episode.Image);
                ((SmartImageView)layout.findViewById(R.id.image)).setImage(wm, android.R.drawable.ic_menu_close_clear_cancel);
            }


            buttonsLocal = ((AbbrevLinearLayout) layout.findViewById(R.id.buttons_local))
                    .setData(R.layout.episode_button,
                             R.drawable.nav,
                             AbbrevLinearLayout.STATE.COLLAPSED)
                    .addItem(R.string.download,         R.drawable.ic_action_download,          clickDownload)
                    .addItem(R.string.stream, R.drawable.ic_action_signal, clickStream)
                    .addItem(R.string.enqueue_stream,   R.drawable.ic_action_signal,            clickEnqueueStream)
                    .addItem(R.string.share,            R.drawable.ic_action_share,             clickShare,
                            AbbrevLinearLayout.STATE.EXPANDED)
                    .addItem(R.string.web_link, R.drawable.ic_action_link, clickLink,
                            AbbrevLinearLayout.STATE.EXPANDED)
                    .addItem(null, R.drawable.ic_action_more, more,
                            AbbrevLinearLayout.STATE.COLLAPSED);

        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }
        show();
        return layout;
    }

    public EpisodeFragment(MasterActivity master, Episode episode) {
        super(master);
        this.episode = episode;
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    View.OnClickListener more = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            buttonsLocal.expand();
        }
    };

    View.OnClickListener    clickDownload = null,
                            clickStream = null,
                            clickEnqueueStream = null,
                            clickShare = null,
                            clickLink = null;


    ;
}

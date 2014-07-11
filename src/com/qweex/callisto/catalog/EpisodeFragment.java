package com.qweex.callisto.catalog;


import android.app.ActionBar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.loopj.android.image.SmartImageView;
import com.loopj.android.image.WebImage;
import com.qweex.callisto.CallistoFragment;
import com.qweex.callisto.MasterActivity;
import com.qweex.callisto.R;

import java.text.SimpleDateFormat;

public class EpisodeFragment extends CallistoFragment {

    SimpleDateFormat sdf = new SimpleDateFormat("cccc MMM d, yyyy");

    RelativeLayout layout;
    AbbrevLinearLayout buttonsLocal;
    Episode episode, previousEpisode, nextEpisode;
    CatalogFragment catalogFragment;

    LinearLayout scroll;

    View previousBtn, nextBtn;

    public EpisodeFragment(MasterActivity master, CatalogFragment catalogFragment, Long _id) {
        super(master);
        this.catalogFragment = catalogFragment;
        setEpisodes(_id);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        if(layout==null) {
            layout = (RelativeLayout) inflater.inflate(R.layout.episode, null);

            SmartImageView imageView = ((SmartImageView)layout.findViewById(R.id.image));
            if(episode.Image!=null) {
                WebImage wm = new WebImage(episode.Image);
                imageView.setImage(wm, android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                imageView.setVisibility(View.GONE);
            }

            previousBtn = layout.findViewById(R.id.previous);
            nextBtn = layout.findViewById(R.id.next);
            previousBtn.setOnClickListener(previous);
            nextBtn.setOnClickListener(next);

            scroll = (LinearLayout) layout.findViewById(R.id.scrollContent);

            layout.findViewById(R.id.scrollView).setOnTouchListener(new OnSwipeTouchListener(master) {
                @Override
                public void onRightToLeftSwipe() {
                    Log.d("Callisto", "Right->Left");
                    previous.onClick(previousBtn);
                }

                @Override
                public void onLeftToRightSwipe() {
                    Log.d("Callisto", "Left->Right");
                    next.onClick(nextBtn);
                }
            });

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

            // Have to call it here AGAIN to actually set the views that were just created.
            setEpisodes(episode.episode_id);
        } else {
            ((ViewGroup)layout.getParent()).removeView(layout);
        }
        show();
        return layout;
    }

    @Override
    public void show() {
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        master.getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public void hide() {
        master.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        master.getSupportActionBar().setTitle(null);
        master.getSupportActionBar().setSubtitle(null);
    }

    View.OnClickListener more = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ((ViewGroup)buttonsLocal.getParent()).removeView(buttonsLocal);
            scroll.addView(buttonsLocal);
            buttonsLocal.expand();
        }
    };

    View.OnClickListener    clickDownload = null,
                            clickStream = null,
                            clickEnqueueStream = null,
                            clickShare = null,
                            clickLink = null;


    ;

    View.OnClickListener previous = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(previousEpisode==null) {
                Log.w("Callisto", "Attempting to get next episode when there is none. WTF?!");
                return;
            }
            setEpisodes(previousEpisode.episode_id);
        }
    };

    View.OnClickListener next = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(nextEpisode==null) {
                Log.w("Callisto", "Attempting to get next episode when there is none. WTF?!");
                return;
            }
            setEpisodes(nextEpisode.episode_id);
        }
    };

    void setEpisodes(Long _id) {
        episode = catalogFragment.dbMate.getOneEpisode(_id);
        previousEpisode = catalogFragment.dbMate.getPreviousEpisode(episode);
        nextEpisode = catalogFragment.dbMate.getNextEpisode(episode);


        if(layout!=null) {
            layout.findViewById(R.id.previous).setVisibility(previousEpisode==null ? View.GONE : View.VISIBLE);
            layout.findViewById(R.id.next).setVisibility(nextEpisode==null ? View.GONE : View.VISIBLE);

            ((TextView)layout.findViewById(R.id.date)).setText(sdf.format(episode.Date.getTime()));
            ((TextView)layout.findViewById(R.id.desc)).setText(android.text.Html.fromHtml(episode.Desc).toString());

            SmartImageView imageView = ((SmartImageView)layout.findViewById(R.id.image));
            if(episode.Image!=null) {
                WebImage wm = new WebImage(episode.Image);
                imageView.setImage(wm, android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                imageView.setVisibility(View.GONE);
            }
        }

        master.getSupportActionBar().setTitle(episode.Title);
        master.getSupportActionBar().setSubtitle(episode.show);
    }
}

package com.qweex.callisto;

import android.support.v4.app.Fragment;

/** Base Fragment that includes some niceties.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public abstract class CallistoFragment extends Fragment {

    /** Constructor; supplies MasterActivity reference. */
    public CallistoFragment(MasterActivity master) {
        this.master = master;
    }

    /** MasterActivity reference. */
    protected MasterActivity master;

    /** Abstract methods. */
    public abstract void show();
    public abstract void hide();
}

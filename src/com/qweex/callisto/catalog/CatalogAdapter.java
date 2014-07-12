package com.qweex.callisto.catalog;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.qweex.callisto.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** This is an adapter for CatalogFragment. It manages moving data from Sqlite to the views in the ListView.
 * @author      Jon Petraglia <notbryant@gmail.com>
 */
public class CatalogAdapter extends SimpleCursorAdapter
{
    /** Date formatter for Listview */
    public static final SimpleDateFormat sdfHuman = new SimpleDateFormat("MMM d");
    public static final SimpleDateFormat sdfHumanLong = new SimpleDateFormat("MMM d, yyyy");
    public static final SimpleDateFormat sdfWeekday = new SimpleDateFormat("EEE", Locale.US);

    /** The cursor pointing at the data in the database. */
    private Cursor cursor;
    /** The context to use for the LayoutInfater. */
    private Context context;
    /** The R.layout id to inflate for each item. */
    private int layout_id;

    /** Inherited constructor.
     * @param context The context to use for the LayoutInfater.
     * @param layout The R.layout id to inflate for each item.
     * @param cursor The cursor pointing at the data in the database.
     */
    public CatalogAdapter(Context context, int layout, Cursor cursor) {
        super(context, layout, cursor, new String[] {}, new int[] {});
        this.cursor = cursor;
        this.layout_id = layout;
        this.context = context;
    }

    /** Inherited method to generate / recycle the view & inject data from relevant data row.
     * @param pos Position of the item in question of the collection.
     * @param inView View passed in that might be recycled.
     * @param parent ViewGroup that should be used as parent for inView.
     * @return The View with all data in it.
     */
    public View getView(int pos, View inView, ViewGroup parent)
    {
        View v = inView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(layout_id, null);
        }
        cursor = getCursor();
        cursor.moveToPosition(pos);

        v.setTag(cursor.getLong(cursor.getColumnIndex("_id")));

        TextView title = (TextView) v.findViewById(R.id.title),
                 date  = (TextView) v.findViewById(R.id.date);

        title.setText(cursor.getString(cursor.getColumnIndex("title")));
        date.setText(formatDate(cursor.getString(cursor.getColumnIndex("date"))));

        return(v);
    }

    String formatDate(String input) {
        try {
            Date d = DatabaseMate.sdfRaw.parse(cursor.getString(cursor.getColumnIndex("date")));
            Calendar cal = Calendar.getInstance(), tester = Calendar.getInstance();
            cal.setTime(d);

            tester.set(Calendar.HOUR_OF_DAY, 0);
            tester.set(Calendar.MINUTE, 0);
            tester.set(Calendar.SECOND, 0);
            tester.set(Calendar.MILLISECOND, 0);

            // Today @ {time}
            if(cal.after(tester))
                return "" + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + " " + cal.get(Calendar.AM_PM);
            // Yesterday
            tester.add(Calendar.DATE, -1);
            if(cal.after(tester))
                return "Yesterday";
            // {in the last week}day
            tester.add(Calendar.DATE, -6);
            if(cal.after(tester)) {
                return sdfWeekday.format(cal.getTime());
            }
            // this year
            if(cal.get(Calendar.YEAR)==Calendar.getInstance().get(Calendar.YEAR))
                return sdfHuman.format(cal.getTime());
            // earlier than this year
            return sdfHumanLong.format(cal.getTime());

        } catch (ParseException e) {
            return "[ERROR]";
        }
    }
}
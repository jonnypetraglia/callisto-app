/*
Copyright (C) 2012 Qweex
This file is a part of Callisto.

Callisto is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

Callisto is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Callisto; If not, see <http://www.gnu.org/licenses/>.
*/
package com.qweex.callisto;

import com.android.vending.billing.BillingService;
import com.android.vending.billing.Consts;
import com.android.vending.billing.Consts.PurchaseState;
import com.android.vending.billing.Consts.ResponseCode;
import com.android.vending.billing.PurchaseDatabase;
import com.android.vending.billing.PurchaseObserver;
import com.android.vending.billing.BillingService.RequestPurchase;
import com.android.vending.billing.BillingService.RestoreTransactions;
import com.android.vending.billing.ResponseHandler;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

//FEATURE: Use the developer payload to write a note to Chris

/** The activity to donate in-app directly to Jupiter Broadcasting.
 * @author MrQweex
 */

public class Donate extends ListActivity implements OnClickListener
{

	private ListView itemsList;
	private Resources RESOURCES;
	private Button GiveChrisSomeHardEarnedMoney;
	private CatalogAdapter mCatalogAdapter;
	private Handler mHandler;
	private DungeonsPurchaseObserver mDungeonsPurchaseObserver;
	public final String DONATION_APP_ID = "com.qweex.donation";
	
	/** Called when the activity is first created. Sets up the view and whatnot.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setTitle("Donate");
		if(Callisto.RESOURCES!=null)
			RESOURCES = Callisto.RESOURCES;
		else
			RESOURCES = getResources();
		
		mBillingService = new BillingService();
        mBillingService.setContext(this);
        mHandler = new Handler();
        mDungeonsPurchaseObserver = new DungeonsPurchaseObserver(mHandler);
        ResponseHandler.register(mDungeonsPurchaseObserver);
		
		//Prepare the listview's appearance
		itemsList = getListView();
		itemsList.setBackgroundColor(RESOURCES.getColor(R.color.backClr));
		
		//Prepare the header's appearance
		TextView header = new TextView(this);
		header.setText("Choose amount (USD)");
		
		
		//Prepare the message & button's appearance
		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		TextView msg = new TextView(this);
		msg.setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
		msg.setTextSize(11f);
		msg.setText(Html.fromHtml("100% of your donation will go "
								 + "<i>directly</i>"
							 	 + " to Jupiter Broadcasting. Afterwards, if you want to donate to the app developer, buy the "
							 	 + "<a href=\"market://details?id=" + DONATION_APP_ID + "\">donation app in the market</a>"
								 + "."));
		msg.setMovementMethod(LinkMovementMethod.getInstance());
		msg.setPadding(30, 10, 30, 10);
		GiveChrisSomeHardEarnedMoney = new Button(this);
		GiveChrisSomeHardEarnedMoney.setText("Donate!");
		GiveChrisSomeHardEarnedMoney.setOnClickListener(this);
		ll.addView(msg);
		ll.addView(GiveChrisSomeHardEarnedMoney);
		
		itemsList.setBackgroundColor(RESOURCES.getColor(R.color.backClr));
		itemsList.setCacheColorHint(RESOURCES.getColor(R.color.backClr));
		header.setTextColor(RESOURCES.getColor(R.color.txtClr));
		header.setTextSize(25f);
		itemsList.addHeaderView(header);
		itemsList.addFooterView(ll);
		
		
		mCatalogAdapter = new CatalogAdapter(this, CATALOG);
		itemsList.setAdapter(mCatalogAdapter);
		itemsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	}
	
	/** Called when the user presses the "Donate!" button */
	@Override
	public void onClick(View v)
	{
        if (!mBillingService.requestPurchase(mSku, Consts.ITEM_TYPE_SUBSCRIPTION, mPayloadContents))
        	Toast.makeText(v.getContext(), "Sorry, but it looks like your device doesn't support Google's Wallet integration! :(", Toast.LENGTH_SHORT).show();
	}

	
	   /** An adapter to update the donation options to the listview */
	   private static class CatalogAdapter extends ArrayAdapter<String> {
	        private CatalogEntry[] mCatalog;
	        private Context context;
	        private final int NAME_ID = 1234;
	        private final int RADIO_ID = 4321;
	        RadioButton lastChecked = null;

	        public CatalogAdapter(Context context, CatalogEntry[] catalog) {
	            super(context, android.R.layout.simple_spinner_item);
	            mCatalog = catalog;
	            this.context = context;
	            
	            for (CatalogEntry element : catalog) {
	                add(element.name);
	            }
	            //*/
	        }
	        
	        @Override
	    	public View getView(int pos, View inView, ViewGroup parent) {
	            View v = inView;
	            TextView name = null;
	            RadioButton select;
	            if (v == null) {
	                 //CREATE NEW ROW
	            	v = new LinearLayout(this.context);
	            	name = new TextView(this.context);
	            	name.setId(NAME_ID);
	            	name.setTextSize(20f);
	            	name.setTextColor(this.context.getResources().getColor(R.color.txtClr));
	            	name.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
	            	select = new RadioButton(this.context);
	            	select.setId(RADIO_ID);
	            	select.setClickable(true);
	            	select.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));
	            	select.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View newRadio) {
							if(lastChecked==newRadio)
								return;
							if(lastChecked!=null)
								lastChecked.setChecked(false);
							((RadioButton)newRadio).setChecked(true);
							lastChecked = (RadioButton) newRadio;
							mSku = (String) (((LinearLayout)newRadio.getParent()).findViewById(NAME_ID)).getContentDescription(); 
						}
	            	});
	            	((LinearLayout) v).setPadding(50, 0, 50, 0);
	            	((LinearLayout) v).addView(name);
	            	((LinearLayout) v).addView(select);
	            	
	            } else {
	            	name = (TextView) v.findViewById(NAME_ID);
	            	select = (RadioButton) v.findViewById(RADIO_ID);
	            }
	            name.setText(mCatalog[pos].name);
	            name.setContentDescription(mCatalog[pos].sku);
	            if(lastChecked==null && pos==0)
	            {
	            	mSku = mCatalog[pos].sku; 
	            	select.setChecked(true);
	            	lastChecked = select;
	            }
	            
	            
	            return v;
	    }
   }
	
	
	
	
	
	//Everything below this  line is pulled from the Android Dev example
	/**********************************/
    /** Called when this activity becomes visible. */
    @Override
    protected void onStart() {
        super.onStart();
        ResponseHandler.register(mDungeonsPurchaseObserver);
    }

    /** Called when this activity is no longer visible. */
    @Override
    protected void onStop() {
        super.onStop();
        ResponseHandler.unregister(mDungeonsPurchaseObserver);
    }

    /** Called when this activity is done. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mPurchaseDatabase.close();
        mBillingService.unbind();
    }
	   
    
    private class DungeonsPurchaseObserver extends PurchaseObserver
    {
        public DungeonsPurchaseObserver(Handler handler) {
            super(Donate.this, handler);
        }

        
        @Override
        public void onBillingSupported(boolean supported, String type) {
            if (type == null || type.equals(Consts.ITEM_TYPE_INAPP)) {
                if (supported)
                {
                    restoreDatabase();
                    GiveChrisSomeHardEarnedMoney.setEnabled(true);
                } else {
                	Toast.makeText(Donate.this, "Sorry, but it looks like your device doesn't support Google's Wallet integration! :(", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onPurchaseStateChange(PurchaseState purchaseState, String itemId,
                int quantity, long purchaseTime, String developerPayload) {

            if (purchaseState == PurchaseState.PURCHASED)
            {
            }
        }
        
        @Override
        public void onRequestPurchaseResponse(RequestPurchase request,
                ResponseCode responseCode)
        {
        	String TAG = "butts";
            if (Consts.DEBUG) {
                Log.d(TAG, request.mProductId + ": " + responseCode);
            }
            if (responseCode == ResponseCode.RESULT_OK) {
                if (Consts.DEBUG) {
                    Log.i(TAG, "purchase was successfully sent to server");
                }
            } else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
                if (Consts.DEBUG) {
                    Log.i(TAG, "user canceled purchase");
                }
            } else {
                if (Consts.DEBUG) {
                    Log.i(TAG, "purchase failed");
                }
            }
        }
        
        @Override
        public void onRestoreTransactionsResponse(RestoreTransactions request,
                ResponseCode responseCode) {
            if (responseCode == ResponseCode.RESULT_OK)
            {
                // Update the shared preferences so that we don't perform
                // a RestoreTransactions again.
                SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(DB_INITIALIZED, true);
                edit.commit();
            } else {
                if (Consts.DEBUG) {
                    Log.d("butts", "RestoreTransactions error: " + responseCode);
                }
            }
        }
        //*/
    }
	   
	   
	   
    public static final String DB_INITIALIZED = "db_initialized";
    private String mItemName;
    private static String mSku;
    private String mPayloadContents = null;
    //private CatalogAdapter mCatalogAdapter;
	
	private BillingService mBillingService;
	private PurchaseDatabase mPurchaseDatabase;
	
	private enum Managed { MANAGED, UNMANAGED, SUBSCRIPTION }
	
    private static final CatalogEntry[] CATALOG = new CatalogEntry[] {
        new CatalogEntry("one_dollar", "$1.00", Managed.UNMANAGED),
        new CatalogEntry("three_dollar", "$3.00", Managed.UNMANAGED),
        new CatalogEntry("five_dollar", "$5.00", Managed.UNMANAGED),
        new CatalogEntry("ten_dollar", "$10.00", Managed.UNMANAGED),
        new CatalogEntry("twenty_dollar", "$20.00", Managed.UNMANAGED)
    };
    
    private static class CatalogEntry {
        public String sku;
        public String name;
        public Managed managed;

        public CatalogEntry(String sku, String name, Managed managed) {
            this.sku = sku;
            this.name = name;
            this.managed = managed;
        }
    }

    
	
    private void restoreDatabase() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(DB_INITIALIZED, false);
        if (!initialized) {
            mBillingService.restoreTransactions();
            //Toast.makeText(this, R.string.restoring_transactions, Toast.LENGTH_LONG).show();
        }
    }
}

/*
 * Copyright (C) 2012-2013 Qweex
 * This file is a part of Callisto.
 *
 * Callisto is free software; it is released under the
 * Open Software License v3.0 without warranty. The OSL is an OSI approved,
 * copyleft license, meaning you are free to redistribute
 * the source code under the terms of the OSL.
 *
 * You should have received a copy of the Open Software License
 * along with Callisto; If not, see <http://rosenlaw.com/OSL3.0-explained.htm>
 * or check OSI's website at <http://opensource.org/licenses/OSL-3.0>.
 */
package com.qweex.callisto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/*import com.android.vending.billing.BillingService;
import com.android.vending.billing.Consts;
import com.android.vending.billing.Consts.PurchaseState;
import com.android.vending.billing.Consts.ResponseCode;
import com.android.vending.billing.PurchaseDatabase;
import com.android.vending.billing.PurchaseObserver;
import com.android.vending.billing.BillingService.RequestPurchase;
import com.android.vending.billing.BillingService.RestoreTransactions;
import com.android.vending.billing.ResponseHandler;
*/
import com.paypal.android.MEP.CheckoutButton;
import com.paypal.android.MEP.PayPal;
import com.paypal.android.MEP.PayPalPayment;
import com.paypal.android.MEP.PayPalResultDelegate;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
//import android.widget.Button;

//FEATURE: Use the developer payload to write a note to Chris

/** The activity to donate in-app directly to Jupiter Broadcasting.
 * @author MrQweex
 */

public class Donate extends ListActivity
{

	private ListView itemsList;
	private Resources RESOURCES;
	public CheckoutButton GiveChrisSomeHardEarnedMoney; //Button for billing
	private CatalogAdapter mCatalogAdapter;
	public final String DONATION_APP_ID = "com.qweex.donation";
	LinearLayout ll;
	RadioButton lastChecked = null;
    private static final int NAME_ID = 1234;
    private static final int RADIO_ID = 4321;
    RadioButton CustomRadio;
    TextView msgView;
    EditText CustomAmount, UserMemo;
	//Contains the SKU for Google Wallet or amount for Paypal
	private static String donationChoice;
	
	
/*-------Paypal--------*/
	private static String memo = "";
	private static String IpnUrl = "";
	private static String paymentID = "8873482296";
	private final static String paypalEmail = "tophfisher@gmail.com";
	ProgressDialog pd;
	
	// The PayPal server to be used - can also be ENV_NONE and ENV_LIVE
	private static final int server = PayPal.ENV_NONE;
	// The ID of your application that you received from PayPal
	private static final String appID = "APP-80W284485P519543T";
	// This is passed in for the startActivityForResult() android function, the value used is up to you
	private static final int request = 1;
	protected static final int INITIALIZE_SUCCESS = 0;
	protected static final int INITIALIZE_FAILURE = 1;
	// This handler will allow us to properly update the UI. You cannot touch Views from a non-UI thread.
	Handler hRefresh = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
		    	case INITIALIZE_SUCCESS:
		    		PayPal pp = PayPal.getInstance();
		    		// Get the CheckoutButton. There are five different sizes. The text on the button can either be of type TEXT_PAY or TEXT_DONATE.
		    		GiveChrisSomeHardEarnedMoney = pp.getCheckoutButton(Donate.this, PayPal.BUTTON_194x37, CheckoutButton.TEXT_DONATE);
		    		GiveChrisSomeHardEarnedMoney.setPadding(20, 20, 20, 20);
		    		GiveChrisSomeHardEarnedMoney.setOnClickListener(clickPaypal);
		    		ll.addView(GiveChrisSomeHardEarnedMoney);
		    		ll.addView(msgView);
		            break;
		    	case INITIALIZE_FAILURE:
		    		finish();
		    		break;
			}
		}
	};
/*------End----------*/
	
	
	
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
		
		//Prepare the listview's appearance
		itemsList = getListView();
		itemsList.setBackgroundColor(RESOURCES.getColor(R.color.backClr));
		
		//Prepare the header's appearance
		TextView header = new TextView(this);
		header.setText("Choose amount (USD)");
		
		ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		
		LinearLayout customView = new LinearLayout(this);
		customView.setPadding(50, 0, 50, 0);
		LinearLayout v2 = new LinearLayout(this);
		v2.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
		TextView dollarsign = new TextView(this);
		dollarsign.setTextSize(20f);
		dollarsign.setText("$");
		dollarsign.setTextColor(this.getResources().getColor(R.color.txtClr));
    	CustomAmount = new EditText(this);
    	CustomAmount.setId(NAME_ID);
    	CustomAmount.setTextSize(20f);
    	CustomAmount.setHint("Custom");
    	CustomRadio = new RadioButton(this);
    	CustomRadio.setId(RADIO_ID);
    	CustomRadio.setClickable(true);
    	CustomRadio.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));
    	CustomRadio.setOnClickListener(selectRadio);
    	v2.addView(dollarsign);
    	v2.addView(CustomAmount);
    	customView.addView(v2);
    	customView.addView(CustomRadio);
    	ll.addView(customView);
		
		UserMemo = new EditText(this);
		UserMemo.setHint("Message to Jupiter Broadcasting");
		UserMemo.setMinLines(4);
		UserMemo.setGravity(Gravity.TOP);
		LinearLayout.LayoutParams x = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		x.setMargins(10, 10, 10, 10);
		ll.addView(UserMemo, x);
    	
    	
    	CustomAmount.addTextChangedListener(new TextWatcher() {

    		
			@Override
			public void afterTextChanged(Editable arg0) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
	            if(!s.toString().matches("^\\\\d{1,3}(\\,\\d{3})*|(\\d+)(\\.\\d{2})?$"))
	            {
	                String userInput= ""+s.toString().replaceAll("[^\\d]", "");
	                StringBuilder cashAmountBuilder = new StringBuilder(userInput);

	                while (cashAmountBuilder.length() > 3 && cashAmountBuilder.charAt(0) == '0') {
	                    cashAmountBuilder.deleteCharAt(0);
	                }
	                while (cashAmountBuilder.length() < 3) {
	                    cashAmountBuilder.insert(0, '0');
	                }
	                cashAmountBuilder.insert(cashAmountBuilder.length()-2, '.');
	                //cashAmountBuilder.insert(0, '$');

	                CustomAmount.setText(cashAmountBuilder.toString());
	                // keeps the cursor always to the right
	                Selection.setSelection(CustomAmount.getText(), cashAmountBuilder.toString().length());

	            }

			}
    	});

    	
		
    	//Prepare the message & button's appearance
		msgView = new TextView(this);
		msgView.setTextColor(Callisto.RESOURCES.getColor(R.color.txtClr));
		msgView.setTextSize(12f);
		msgView.setText(Html.fromHtml("100% of your donation will go "
								 + "<i>directly</i>"
							 	 + " to Jupiter Broadcasting. Afterwards, if you want to donate to the app developer, buy the "
							 	 + "<a href=\"market://details?id=" + DONATION_APP_ID + "\">donation app in the market</a>"
								 + "."));
		msgView.setMovementMethod(LinkMovementMethod.getInstance());
		msgView.setPadding(30, 0, 30, 15);
		
		
		/*------Paypal------*/
		Thread libraryInitializationThread = new Thread() {
			public void run() {
				initLibrary();
				if(pd!=null)
	    		   pd.hide();
				
				// The library is initialized so let's create our CheckoutButton and update the UI.
				if (PayPal.getInstance().isLibraryInitialized()) {
					hRefresh.sendEmptyMessage(INITIALIZE_SUCCESS);
				}
				else {
					hRefresh.sendEmptyMessage(INITIALIZE_FAILURE);
				}
			}
		};
		pd = ProgressDialog.show(Donate.this, Callisto.RESOURCES.getString(R.string.loading), Callisto.RESOURCES.getString(R.string.loading_msg), true, false);
		pd.setCancelable(true);
		pd.setOnCancelListener(new OnCancelListener(){
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		pd.show();
		libraryInitializationThread.start();
		
		
		/*------Billing------*/
		//billingInit();
		//GiveChrisSomeHardEarnedMoney = new Button(this);
		//GiveChrisSomeHardEarnedMoney.setText("Donate");
		//ll.addView(GiveChrisSomeHardEarnedMoney);
		/*-------------------*/
		
		
		
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
	
	
	   /** An adapter to update the donation options to the listview */
	   private class CatalogAdapter extends ArrayAdapter<String> {
	        private CatalogEntry[] mCatalog;
	        private Context context;

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
	            	name = new TextView(this.context);
	            	name.setId(NAME_ID);
	            	name.setTextSize(20f);
	            	name.setTextColor(this.context.getResources().getColor(R.color.txtClr));
	            	name.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
	            	select = new RadioButton(this.context);
	            	select.setId(RADIO_ID);
	            	select.setClickable(true);
	            	select.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));
	            	select.setOnClickListener(selectRadio);
	            	v = new LinearLayout(this.context);
	            	((LinearLayout) v).setPadding(50, 0, 50, 0);
	            	((LinearLayout) v).addView(name);
	            	((LinearLayout) v).addView(select);
		            if(lastChecked==null)
		            {
		            	lastChecked = select;
		            	donationChoice = mCatalog[pos].name.substring(1);
		            }
	            } else {
	            	name = (TextView) v.findViewById(NAME_ID);
	            	select = (RadioButton) v.findViewById(RADIO_ID);
	            }
	            name.setText(mCatalog[pos].name);
	            name.setContentDescription(mCatalog[pos].sku);
	            String s =((String) ((TextView)(v.findViewById(NAME_ID))).getText()).substring(1);
	            if(s.equals(donationChoice))
	            	select.setChecked(true);
	            else
	            	select.setChecked(false);
	            return v;
	    }
	        
	        
   }
	   
 	   OnClickListener selectRadio = new OnClickListener() {
			@Override
			public void onClick(View newRadio) {
				if(newRadio==lastChecked)
					return;
				if(lastChecked!=null)
					lastChecked.setChecked(false);
				((RadioButton)newRadio).setChecked(true);
				lastChecked = (RadioButton) newRadio;
				//donationChoice = (String) (((LinearLayout)newRadio.getParent()).findViewById(NAME_ID)).getContentDescription();
				if(CustomRadio==newRadio)
					donationChoice=null;
				else
					donationChoice = ((String) ((TextView)(((LinearLayout)newRadio.getParent()).findViewById(NAME_ID))).getText()).substring(1);
			}
   	};
	   
	   
	   
	   private static final CatalogEntry[] CATALOG = new CatalogEntry[] {
	        new CatalogEntry("dollar", "$1.00"),
	        new CatalogEntry("three_dollar", "$3.00"),
	        new CatalogEntry("pi_dollar", "$3.14"),
	        new CatalogEntry("five_dollar", "$5.00"),
	        new CatalogEntry("ten_dollar", "$10.00"),
	        new CatalogEntry("leet_dollar", "$13.37"),
	        new CatalogEntry("twenty_dollar", "$20.00")
	    };
	   
	private static class CatalogEntry {
	    public String sku;
	    public String name;
	
	    public CatalogEntry(String sku, String name) {
	        this.sku = sku;
	        this.name = name;
	    }
	}

/*---------------------This is for Paypal----------------*/
	
	private void initLibrary() {
		
		PayPal pp = PayPal.getInstance();
		// If the library is already initialized, then we don't need to initialize it again.
		if(pp == null) {
			// This is the main initialization call that takes in your Context, the Application ID, and the server you would like to connect to.
			pp = PayPal.initWithAppID(this, appID, server);
   			
			// -- These are required settings.
        	pp.setLanguage("en_US"); // Sets the language for the library.
        	// --
        	
        	// -- These are a few of the optional settings.
        	// Sets the fees payer. If there are fees for the transaction, this person will pay for them. Possible values are FEEPAYER_SENDER,
        	// FEEPAYER_PRIMARYRECEIVER, FEEPAYER_EACHRECEIVER, and FEEPAYER_SECONDARYONLY.
        	pp.setFeesPayer(PayPal.FEEPAYER_EACHRECEIVER); 
        	// Set to true if the transaction will require shipping.
        	pp.setShippingEnabled(false);
        	// Dynamic Amount Calculation allows you to set tax and shipping amounts based on the user's shipping address. Shipping must be
        	// enabled for Dynamic Amount Calculation. This also requires you to create a class that implements PaymentAdjuster and Serializable.
        	pp.setDynamicAmountCalculationEnabled(false);
        	// --
		}
	}
	
	OnClickListener clickPaypal = new OnClickListener()
    {
		public void onClick(View v)
		{
			/**
			 * For each call to checkout() and preapprove(), we pass in a ResultDelegate. If you want your application
			 * to be notified as soon as a payment is completed, then you need to create a delegate for your application.
			 * The delegate will need to implement PayPalResultDelegate and Serializable. See our ResultDelegate for
			 * more details.
			 */
			
			((CheckoutButton) v).updateButton();
			if(CustomRadio.isChecked())
				try {
					new BigDecimal(CustomAmount.getText().toString());
				} catch(NumberFormatException e){
					Toast.makeText(v.getContext(), "The amount you entered is not a valid number, you silly goose.", Toast.LENGTH_SHORT).show();
					//((CheckoutButton) v).updateButton();
					return;
				}
			
			memo = UserMemo.getText().toString();
			if(memo=="")
				memo = "-Sent from Callisto";
			PayPalPayment payment = createPayment();
			Intent checkoutIntent = PayPal.getInstance().checkout(payment, v.getContext(), new ResultDelegate());
	    	((Activity) v.getContext()).startActivityForResult(checkoutIntent, request);
	    	//((CheckoutButton) v).updateButton();
		}
    };
    
    
	
    
	private PayPalPayment createPayment() {
		// Create a basic PayPalPayment.
		PayPalPayment payment = new PayPalPayment();
		// Sets the currency type for this payment.
    	payment.setCurrencyType("USD");
    	// Sets the recipient for the payment. This can also be a phone number.
    	payment.setRecipient(paypalEmail);
    	// Sets the amount of the payment, not including tax and shipping amounts.
    	if(donationChoice==null)
    		payment.setSubtotal(new BigDecimal(CustomAmount.getText().toString()));
		else
			payment.setSubtotal(new BigDecimal(donationChoice));
    	// Sets the payment type. This can be PAYMENT_TYPE_GOODS, PAYMENT_TYPE_SERVICE, PAYMENT_TYPE_PERSONAL, or PAYMENT_TYPE_NONE.
    	payment.setPaymentType(PayPal.PAYMENT_TYPE_GOODS);
    	
    	// Sets the merchant name. This is the name of your Application or Company.
    	payment.setMerchantName("Jupiter Broadcasting");
    	// Sets the Custom ID. This is any ID that you would like to have associated with the payment.
    	payment.setCustomID(paymentID);
    	// Sets the Instant Payment Notification url. This url will be hit by the PayPal server upon completion of the payment.
    	payment.setIpnUrl(IpnUrl);
    	// Sets the memo. This memo will be part of the notification sent by PayPal to the necessary parties.
    	payment.setMemo(memo);
    	
    	return payment;
	}
    
	
	

/*---------------------This is for the Google Wallet API----------------*/
	
	/*
	public void billingInit(Context c)
	{
		mBillingService = new BillingService();
        mBillingService.setContext(c);
        mHandler = new Handler();
        mDungeonsPurchaseObserver = new DungeonsPurchaseObserver(mHandler);
        ResponseHandler.register(mDungeonsPurchaseObserver);
	}
	*/
	
	
	/*
	OnClickListener clickBilling = new OnClickListener()
    {
    	@Override
    	public void onClick(View v)
    	{
	        if (!mBillingService.requestPurchase(mSku, Consts.ITEM_TYPE_INAPP, mPayloadContents))
	        	Toast.makeText(v.getContext(), "Sorry, but it looks like your device doesn't support Google's Wallet integration! :(", Toast.LENGTH_SHORT).show();
		}
	};
	*/
	
	//Everything below this  line is pulled from the Android Dev example
	/**********************************/
    /** Called when this activity becomes visible. */
	   /*
    @Override
    protected void onStart() {
        super.onStart();
        ResponseHandler.register(mDungeonsPurchaseObserver);
    }
    */

    /** Called when this activity is no longer visible. */
	   /*
    @Override
    protected void onStop() {
        super.onStop();
        ResponseHandler.unregister(mDungeonsPurchaseObserver);
    }
    */

    /** Called when this activity is done. */
	   /*
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mPurchaseDatabase.close();
        mBillingService.unbind();
    }
    */
	   
    /*
    private class DungeonsPurchaseObserver extends PurchaseObserver
    {
        public DungeonsPurchaseObserver(Handler handler) {
            super(Donate_2.this, handler);
        }

        
        @Override
        public void onBillingSupported(boolean supported, String type) {
            if (type == null || type.equals(Consts.ITEM_TYPE_INAPP)) {
                if (supported)
                {
                    restoreDatabase();
                    GiveChrisSomeHardEarnedMoney.setEnabled(true);
                } else {
                	Toast.makeText(Donate_2.this, "Sorry, but it looks like your device doesn't support Google's Wallet integration! :(", Toast.LENGTH_SHORT).show();
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
    }
	*/   
	   
	/*
    public static final String DB_INITIALIZED = "db_initialized";
    private String mItemName;
    
    private String mPayloadContents = null;
	
	private BillingService mBillingService;
	private PurchaseDatabase mPurchaseDatabase;
	private enum Managed { MANAGED, UNMANAGED, SUBSCRIPTION }
	private DungeonsPurchaseObserver mDungeonsPurchaseObserver;
	
    */
	/*
    private void restoreDatabase() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(DB_INITIALIZED, false);
        if (!initialized) {
            mBillingService.restoreTransactions();
            //Toast.makeText(this, R.string.restoring_transactions, Toast.LENGTH_LONG).show();
        }
    }
    */
}

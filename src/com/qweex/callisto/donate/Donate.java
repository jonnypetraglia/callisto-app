/*
 * Copyright (C) 2012-2014 Qweex
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
package com.qweex.callisto.donate;

import java.math.BigDecimal;

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
import android.widget.*;
import com.paypal.android.MEP.CheckoutButton;
import com.paypal.android.MEP.PayPal;
import com.paypal.android.MEP.PayPalPayment;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.LinearLayout.LayoutParams;
import com.qweex.callisto.Callisto;
import com.qweex.callisto.R;
import com.qweex.callisto.StaticBlob;

//FEATURE: Use the developer payload to write a note to Chris

/** The activity to donate in-app directly to Jupiter Broadcasting.
 * @author MrQweex
 */

public class Donate extends ListActivity
{
    /** The listview containing the rows for the amounts */
    private ListView itemsList;
    /** The Paypal Button to submit the donation */
    private CheckoutButton GiveChrisSomeHardEarnedMoney;
    /** An adapter for the prices in the ListView */
    private PricesAdapter mPricesAdapter;
    /** A linearLayout for containing the ListView and buttons and notes and stuff*/
    private LinearLayout contentLayout;
    /** A MenuItem id */
    private static final int NAME_ID = 1234, RADIO_ID = 4321;
    /** The last checked button that is used to fetch the price when pressing the button */
    private RadioButton lastChecked = null;
    /** The RadioButton view that is the custom amount; used to get the amount from the EditText instead of a TextView. */
    private RadioButton CustomRadio;
    /** A control for user input. Self explanatory names you silly. */
    private EditText CustomAmount, UserMemo;
    /** The message below the button */
    private TextView msgView;
    /** The price that is selected for PayPal*/
    private static String donationChoice;   //Contains the SKU for Google Wallet or amount for Paypal????
	
	
/*-------Paypal--------*/
    /** [Paypal] The memo? */
    private static String memo = "";
    /** [Paypal] What is this I don't even */ //TODO: what
    private static String IpnUrl = "";
    /** [Paypal] The paymentID to JB */
    private static String paymentID = "8873482296";
    /** [Paypal] The email for JB */
    private final static String paypalEmail = "tophfisher@gmail.com";
    /** ProgressDialog to be shown while fetching PayPal */
    private ProgressDialog baconPDialog;

    /** The PayPal server to be used - can also be ENV_NONE and ENV_LIVE */
    private static final int server = PayPal.ENV_NONE;
    /** The ID of your application that you received from PayPal */
    private static final String appID = "APP-80W284485P519543T";
    /** This is passed in for the startActivityForResult() android function, the value used is up to you */
    private static final int request = 1;
    /** Constants */
    protected static final int INITIALIZE_SUCCESS = 0, INITIALIZE_FAILURE = 1;
    /** This handler will allow us to properly update the UI. You cannot touch Views from a non-UI thread. */
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
                    contentLayout.addView(GiveChrisSomeHardEarnedMoney);
                    contentLayout.addView(msgView);
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
        if(android.os.Build.VERSION.SDK_INT >= 11)
            setTheme(R.style.Default_New);
        super.onCreate(savedInstanceState);
        setTitle(R.string.donate);

        //Prepare the listview's appearance
        itemsList = getListView();
        itemsList.setBackgroundColor(this.getResources().getColor(R.color.backClr));

        //Prepare the header's appearance
        TextView header = new TextView(this);
        header.setText(R.string.choose_amount);

        //Blaaaargh, build the rest of the layout
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

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
        CustomAmount.setHint(R.string.custom);
        CustomRadio = new RadioButton(this);
        CustomRadio.setId(RADIO_ID);
        CustomRadio.setClickable(true);
        CustomRadio.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));
        CustomRadio.setOnClickListener(selectRadio);
        v2.addView(dollarsign);
        v2.addView(CustomAmount);
        customView.addView(v2);
        customView.addView(CustomRadio);
        contentLayout.addView(customView);

        UserMemo = new EditText(this);
        UserMemo.setHint(R.string.msg_to_jb);
        UserMemo.setMinLines(4);
        UserMemo.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams x = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        x.setMargins(10, 10, 10, 10);
        contentLayout.addView(UserMemo, x);

        //Makes sure that the amount in the custom box is valid
        CustomAmount.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {/* Not needed */}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Not needed */ }

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

                    CustomAmount.setText(cashAmountBuilder.toString());
                    // keeps the cursor always to the right
                    Selection.setSelection(CustomAmount.getText(), cashAmountBuilder.toString().length());
                }

            }
        });

        //Prepare the message & button's appearance
        msgView = new TextView(this);
        msgView.setTextColor(this.getResources().getColor(R.color.txtClr));
        msgView.setTextSize(12f);
        msgView.setText(Html.fromHtml(this.getResources().getString(R.string.donate_note)));
        msgView.setMovementMethod(LinkMovementMethod.getInstance());
        msgView.setPadding(30, 0, 30, 15);
		
		
		/*------Paypal------*/
        Thread libraryInitializationThread = new Thread() {
            public void run() {
                initLibrary();
                hideBacon.sendEmptyMessage(0);

                // The library is initialized so let's create our CheckoutButton and update the UI.
                if (PayPal.getInstance().isLibraryInitialized()) {
                    hRefresh.sendEmptyMessage(INITIALIZE_SUCCESS);
                }
                else {
                    hRefresh.sendEmptyMessage(INITIALIZE_FAILURE);
                }
            }
        };
        baconPDialog = Callisto.BaconDialog(Donate.this, this.getResources().getString(R.string.loading), this.getResources().getString(R.string.loading_msg));
        baconPDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        libraryInitializationThread.start();

		/*------Billing------*/
        //billingInit();
        //GiveChrisSomeHardEarnedMoney = new Button(this);
        //GiveChrisSomeHardEarnedMoney.setText("Donate");
        //contentLayout.addView(GiveChrisSomeHardEarnedMoney);
		/*-------------------*/

        itemsList.setBackgroundColor(this.getResources().getColor(R.color.backClr));
        itemsList.setCacheColorHint(this.getResources().getColor(R.color.backClr));
        header.setTextColor(this.getResources().getColor(R.color.txtClr));
        header.setTextSize(25f);
        itemsList.addHeaderView(header);
        itemsList.addFooterView(contentLayout);


        mPricesAdapter = new PricesAdapter(this, CATALOG);
        itemsList.setAdapter(mPricesAdapter);
        itemsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

    }

    /** Listener to hide the loading dialog */
    Handler hideBacon = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(baconPDialog !=null)
                baconPDialog.hide();
        }
    };

    /** An adapter to update the donation options to the listview */
    private class PricesAdapter extends ArrayAdapter<String>
    {
        /** The names and prices of the listview entry; used generally with CATALOG */
        private PriceListEntry[] priceCatalog;
        /** Context for the adapter */
        private Context context;

        /** Constructor; adds the elements from the catalog */
        public PricesAdapter(Context context, PriceListEntry[] catalog)
        {
            super(context, android.R.layout.simple_spinner_item);
            priceCatalog = catalog;
            this.context = context;

            for (PriceListEntry element : catalog)
                add(element.name);
        }

        /** Create the view of a listview */
        @Override
        public View getView(int pos, View inView, ViewGroup parent) {
            View v = inView;
            TextView name;
            RadioButton select;

            // If we are not recycling the view
            if(v == null)
            {
                //CREATE NEW ROW
                //Create da name
                name = new TextView(this.context);
                name.setId(NAME_ID);
                name.setTextSize(20f);
                name.setTextColor(Donate.this.getResources().getColor(R.color.txtClr));
                name.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
                //Create da radio
                select = new RadioButton(this.context);
                select.setId(RADIO_ID);
                select.setClickable(true);
                select.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f));
                select.setOnClickListener(selectRadio);
                //Create da Layout to wrap da name and radio
                v = new LinearLayout(this.context);
                v.setPadding(50, 0, 50, 0);
                ((LinearLayout) v).addView(name);
                ((LinearLayout) v).addView(select);
                //Select it if it was the last one selected
                if(lastChecked==null)
                {
                    lastChecked = select;
                    donationChoice = priceCatalog[pos].name.substring(1);
                }
            //If we are recycling the view
            } else {
                name = (TextView) v.findViewById(NAME_ID);
                select = (RadioButton) v.findViewById(RADIO_ID);
            }
            name.setText(priceCatalog[pos].name);
            name.setContentDescription(priceCatalog[pos].sku);

            //s  will be the NAME_ID, that is the identifier in the prices list
            String s =((String) ((TextView)(v.findViewById(NAME_ID))).getText()).substring(1);
            if(s.equals(donationChoice))
                select.setChecked(true);
            else
                select.setChecked(false);
            return v;
        }
    }
    /** Listener for when a radio is selected; updates the current selection variables */
    OnClickListener selectRadio = new OnClickListener()
    {
        @Override
        public void onClick(View newRadio) {
            if(newRadio==lastChecked)
                return;
            if(lastChecked!=null)
                lastChecked.setChecked(false);
            ((RadioButton)newRadio).setChecked(true);
            lastChecked = (RadioButton) newRadio;
            // What
            //donationChoice = (String) (((LinearLayout)newRadio.getParent()).findViewById(NAME_ID)).getContentDescription();
            if(CustomRadio==newRadio)
                donationChoice=null;
            else
                donationChoice = ((String) ((TextView)(((LinearLayout)newRadio.getParent()).findViewById(NAME_ID))).getText()).substring(1);
        }
    };

    /** An array containing identifiers and prices */
    private static final PriceListEntry[] CATALOG = new PriceListEntry[] {
            new PriceListEntry("dollar", "$1.00"),
            new PriceListEntry("three_dollar", "$3.00"),
            new PriceListEntry("pi_dollar", "$3.14"),
            new PriceListEntry("five_dollar", "$5.00"),
            new PriceListEntry("ten_dollar", "$10.00"),
            new PriceListEntry("leet_dollar", "$13.37"),
            new PriceListEntry("twenty_dollar", "$20.00")
    };

    /** A class for the Google Billing service; sku is name on the Google Play side, name is the price? */
    private static class PriceListEntry {
        public String sku;
        public String name;

        public PriceListEntry(String sku, String name) {
            this.sku = sku;
            this.name = name;
        }
    }

/*---------------------This is for Paypal----------------*/

    private void initLibrary()
    {
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
                    Toast.makeText(v.getContext(), R.string.amount_not_valid, Toast.LENGTH_SHORT).show();
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

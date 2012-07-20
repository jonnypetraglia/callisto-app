package com.qweex.callisto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class ContactForm extends Activity
{
	private EditText nameFirst, nameLast, email, message;
	private Spinner topicSpinner;
	private ArrayAdapter spinnerArrayAdapter;
	private ArrayList<String> spinnerArray;
	private final String formURL = "https://jblive.wufoo.com/embed/w7x2r7/";
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		View contactForm = ((LayoutInflater)this.getSystemService(this.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.contact, null, false);
		Callisto.build_layout(this, contactForm);
		
		nameFirst = (EditText) findViewById(R.id.nameFirst);
		nameLast = (EditText) findViewById(R.id.nameLast);
		email = (EditText) findViewById(R.id.email);
		message = (EditText) findViewById(R.id.message);
		
		
		topicSpinner = (Spinner) findViewById(R.id.topic);
		spinnerArray = new ArrayList<String>();
		spinnerArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, spinnerArray);
		topicSpinner.setAdapter(spinnerArrayAdapter);
		
		/*
		new GetTopics().execute(formURL, null);
	}
	   // performs database query outside GUI thread
	   private class GetTopics extends AsyncTask<String, Object, Object> 
	   {
	      @Override
	      protected Object doInBackground(String... params)
	      {
	      */
		    HttpClient httpClient = new DefaultHttpClient();
		    HttpContext localContext = new BasicHttpContext();
		    HttpGet httpGet = new HttpGet(formURL);
		    HttpResponse response;
			try {
				response = httpClient.execute(httpGet, localContext);
			    String result = "";
		
			    BufferedReader reader = new BufferedReader(
			        new InputStreamReader(
			          response.getEntity().getContent()
			        )
			      );
		
			    String line = null;
			    while ((line = reader.readLine()) != null){
			      result += line + "\n";
			    }
			    int x = result.indexOf("Topic or Show") + "Topic or Show".length();
			    result = result.substring(x,result.indexOf("</li>",x));
			    result = result.replace("</option>", "~</option>");
			    result=android.text.Html.fromHtml(result).toString();
			    String[] tokens = result.split("~");
		
			    for (String t : tokens)
			    	if(!t.trim().equals(""))
			    		spinnerArray.add(t.trim());
			    spinnerArrayAdapter.notifyDataSetChanged();
			    topicSpinner.setSelection(0);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	/*		return null;
	      }*/
	}

	public void postData() {
	    // Create a new HttpClient and Post Header
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("http://www.yoursite.com/script.php");

	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("Field4", nameFirst.getText().toString()));
	        nameValuePairs.add(new BasicNameValuePair("Field5", nameLast.getText().toString()));
	        nameValuePairs.add(new BasicNameValuePair("Field6", email.getText().toString()));
	        nameValuePairs.add(new BasicNameValuePair("Field7", topicSpinner.getSelectedItem().toString()));
	        nameValuePairs.add(new BasicNameValuePair("Field1", message.getText().toString()));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        HttpResponse response = httpclient.execute(httppost);
	        
	    } catch (ClientProtocolException e) {
	        // TODO Auto-generated catch block
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	    }
	} 
	
}

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;

//This activity is for displaying a form through which the user can contact Jupiter Broadcasting.

//NOTE: At first, I had built a native UI for each element and I fetched the "Topics" list from the website.
//	    But it took forever and it required figuring out how to do a POST, plus, if the 
//FEATURE: Someone to make this look pretty/native. Maybe chzbacon?

/** Form to contact the JB team directly from inside Callisto.
 * @author MrQweex
 */
public class ContactForm extends Activity
{
	private final String formURL = "https://jblive.wufoo.com/embed/w7x2r7/";
	
	/** Called when the activity is first created. Retrieves the wufoo form and inserts it into the view.
	 * @param savedInstanceState Um I don't even know. Read the Android documentation.
	 *  */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		setContentView(R.layout.contact2);
		setTitle(R.string.contact);
		
		WebView wv = (WebView) findViewById(R.id.form);
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpContext localContext = new BasicHttpContext();
	    HttpGet httpGet = new HttpGet(formURL);
	    HttpResponse response;
	    String result = "";
		try
		{
			
			 // Read in the css
	        InputStream input;  
            input = getAssets().open("style.css");  
              
             int size = input.available();  
             byte[] buffer = new byte[size];  
             input.read(buffer);  
             input.close();  
  
             // byte buffer into a string  
             result = new String(buffer);
             result = "<style type='text/css'>\n" + result + "\n</style>\n"; 
	              
			response = httpClient.execute(httpGet, localContext);
	
		    BufferedReader reader = new BufferedReader(
		        new InputStreamReader(
		          response.getEntity().getContent()
		        )
		      );
	
		    String line = null;
		    while ((line = reader.readLine()) != null){
		      result += line + "\n";
		    }
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(getWindowManager().getDefaultDisplay().getWidth());
		result = result.replaceAll("width:;", "width:" + getWindowManager().getDefaultDisplay().getWidth()*.9 + ";");
		result = result.replaceAll("height:;", "height:" + getWindowManager().getDefaultDisplay().getHeight()*.6 + ";");
		wv.loadData(result, "text/html", "utf-8");
	}
	
	/** Stops the activity from being re-created from  */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
    }
}

package com.cointica.beton;

import android.app.Application;
import android.content.Intent;

import com.parse.Parse;

/**
 * Created by Dominik Barwacz (dombar@gmail.com) on 18 September 2014
 * as part of beton.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(this, getString(R.string.parse_client_id), getString(R.string.parse_client_secret));
        Intent i=new Intent(this, BetonUpdateService.class);

        startService(i);
    }
}

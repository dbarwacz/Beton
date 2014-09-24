package com.cointica.beton;

import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

/**
 * Created by Dominik Barwacz (dombar@gmail.com) on 18 September 2014
 * as part of beton.
 */
public class ParseBetonFactory {
    public final static String KEY_LOCATION = "location";
    public final static String KEY_LNG = "lng";
    public final static String KEY_LAT = "lat";
    public final static String KEY_USER = "username";
    public final static String KEY_TITLE = "name";

    public static ParseObjectBuilder start(){
        return new ParseObjectBuilder();
    }

    public static class ParseObjectBuilder{
        private ParseObjectBuilder() {
            this.object = new ParseObject("Beton");
        }
        ParseObject object;

        private void put(String key, Object object){
            this.object.put(key, object);
        }
        public ParseObject build(){
            return object;
        }
        public ParseObjectBuilder location(LatLng location){
            ParseGeoPoint point = new ParseGeoPoint(location.latitude,location.longitude);
            put(KEY_LOCATION, point);
            return this;
        }

        public ParseObjectBuilder user(String username){
            put(KEY_USER, username);
            return this;
        }

        public ParseObjectBuilder title(String title){
            put(KEY_TITLE, title);
            return this;
        }

    }

}

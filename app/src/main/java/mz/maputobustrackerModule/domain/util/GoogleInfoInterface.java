package mz.maputobustrackerModule.domain.util;

import android.graphics.Color;
import android.os.AsyncTask;
import android.text.Html;
import android.view.View;

import com.androidadvance.topsnackbar.TSnackbar;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import mz.maputobustrackerModule.MapsActivity;

/**
 * Created by Hawkingg on 27/03/2017.
 */

public class GoogleInfoInterface {

    public static GoogleInfoInterface.legs leg;
    private static JSONArray stepsArray;
    private static LatLng origem;
    private static LatLng destino;
    public static boolean processTempoEstimado(ArrayList<LatLng> points, boolean withIndications, String language, boolean optimize, int col, String mode, boolean prog)
    {

            String url = makeURL(points,"driving",optimize);
            new GoogleInfoInterface.connectAsyncTask(url,withIndications).execute();
            return true;
    }
    public static boolean processDistancia(ArrayList<LatLng> points, boolean withIndications, String language, boolean optimize, int col, String mode, boolean prog, String distancia)
    {
        String url = makeURL(points,"driving",optimize);
        origem = points.get(0);
        destino =points.get(1);
        new GoogleInfoInterface.connectAsyncTaskDistancia(url,withIndications,distancia).execute();
        return true;
    }

    private static String makeURL(ArrayList<LatLng> points, String mode, boolean optimize){
        StringBuilder urlString = new StringBuilder();
        if(mode == null)
            mode = "driving";
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append( points.get(0).latitude);
        urlString.append(',');
        urlString.append(points.get(0).longitude);
        urlString.append("&destination=");
        urlString.append(points.get(points.size()-1).latitude);
        urlString.append(',');
        urlString.append(points.get(points.size()-1).longitude);
        urlString.append("&waypoints=");
        if(optimize)
            urlString.append("optimize:true|");
        urlString.append( points.get(1).latitude);
        urlString.append(',');
        urlString.append(points.get(1).longitude);
        for(int i=2;i<points.size()-1;i++)
        {
            urlString.append('|');
            urlString.append( points.get(i).latitude);
            urlString.append(',');
            urlString.append(points.get(i).longitude);
        }
        urlString.append("&sensor=true&mode="+mode);
        return urlString.toString();
    }

    private static String makeURL(double sourcelat, double sourcelog, double destlat, double destlog, String mode){
        StringBuilder urlString = new StringBuilder();

        if(mode == null)
            mode = "driving";

        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString
                .append(Double.toString( sourcelog));
        urlString.append("&destination=");// to
        urlString
                .append(Double.toString( destlat));
        urlString.append(",");
        urlString.append(Double.toString( destlog));
        String lang = "portuguese";
        urlString.append("&sensor=false&mode="+mode+"&alternatives=true&language="+lang);
        return urlString.toString();
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }


    private static class connectAsyncTask extends AsyncTask<Void, Void, String> {
        String url;
        boolean steps;
        connectAsyncTask(String urlPass, boolean withSteps){
            url = urlPass;
            steps = withSteps;
        }
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();

        }
        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            setLegs(result);
            String show="";
            Double minutos=0.0;
            if (stepsArray != null) {
            if (stepsArray.length() == 0) {
                MapsActivity.snackbar = TSnackbar.make(MapsActivity.progressBar, "A determinar tempo estimado de chegada. ", TSnackbar.LENGTH_LONG);
                View snackBarView = MapsActivity.snackbar.getView();
                snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                MapsActivity.snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                MapsActivity.snackbar.show();
            } else {
                minutos = Integer.parseInt(leg.durantionsec)/60.0;

                if (minutos.intValue() == 0) {
                    show ="de menos de 1 minuto";
                }
                else if(minutos.intValue() == 1)
                {
                    show ="de 1 minuto";
                }
                else if(minutos==null)
                {
                    show="a ser determinado no momento pelo sistema.";
                }
                else {
                    show ="de "+minutos.intValue()+" minutos";
                }
            }
            MapsActivity.vg.setTempoEstChgada(show);
            MapsActivity.vg.setTempoReal(minutos);
        }else
            {
                MapsActivity.vg.setTempoEstChgada("a ser determinado no momento pelo sistema.");
                MapsActivity.vg.setTempoReal(0.0);

            }
    }}

    private static class connectAsyncTaskDistancia extends AsyncTask<Void, Void, String> {
        String url;
        String distanciaM;
        boolean steps;

        connectAsyncTaskDistancia(String urlPass, boolean withSteps, String distancia) {
            url = urlPass;
            steps = withSteps;
            distanciaM = distancia;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(Void... params) {
            JSONParser jParser = new JSONParser();
            String json = jParser.getJSONFromUrl(url);
            return json;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            setLegs(result);
            String show = "";
            Double minutos = 0.0;
            if (stepsArray != null) {
                if (stepsArray.length() == 0) {
                    MapsActivity.snackbar = TSnackbar.make(MapsActivity.progressBar, "A determinar distancia. ", TSnackbar.LENGTH_LONG);
                    View snackBarView = MapsActivity.snackbar.getView();
                    snackBarView.setBackgroundColor(Color.parseColor("#f44336")); // snackbar background color
                    MapsActivity.snackbar.setActionTextColor(Color.parseColor("#FFFFEE19")); // snackbar action text color
                    MapsActivity.snackbar.show();
                    show = "a ser determinado no momento pelo sistema.";
                } else {
                    if (distanciaM.equals("maxima")) {
                        MapsActivity.distanciaMaxima = Double.parseDouble(leg.distanceValue) * 0.001;
                        MapsActivity.vg.setKmapercorrer("Distância a percorrer: " + leg.distance);
                    } else if (distanciaM.equals("minima")) {
                        MapsActivity.distanciaminima = Double.parseDouble(leg.distanceValue) * 0.001;
                        MapsActivity.vg.setKmpercorridos("Distância percorrida: " + leg.distance);
                    }

                }

            }else
            {
                if (distanciaM.equals("maxima")) {
                    MapsActivity.vg.setKmapercorrer("Distância a percorrer: a determinar");
                    MapsActivity.distanciaMaxima = MapsActivity.CalculationByDistance(origem,destino);
                } else if (distanciaM.equals("minima")) {
                    MapsActivity.vg.setKmpercorridos("Distância percorrida: a determinar");
                    MapsActivity.distanciaminima = MapsActivity.CalculationByDistance(origem,destino);
                }

            }
        }
    }

    private static void setLegs(String result) {

        try {
            //Tranform the string into a json object
            final JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONArray arrayLegs = routes.getJSONArray("legs");
            JSONObject legs = arrayLegs.getJSONObject(0);
            stepsArray = legs.getJSONArray("steps");

            //put initial point
            leg = new legs(legs);
        }
        catch (JSONException e) {

        }
    }

    private class Step
    {
        public String distance;
        public LatLng location;
        public String instructions;
        public String duration;
        Step(JSONObject stepJSON)
        {
            JSONObject startLocation;
            try {
                duration =stepJSON.getJSONObject("duration").getString("text");
                distance = stepJSON.getJSONObject("distance").getString("text");
                startLocation = stepJSON.getJSONObject("start_location");
                location = new LatLng(startLocation.getDouble("lat"),startLocation.getDouble("lng"));
                try {
                    instructions = URLDecoder.decode(Html.fromHtml(stepJSON.getString("html_instructions")).toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                };

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    private static class legs
    {
        public String distance;
        public String distanceValue;
        public String duration;
        public String durantionsec;
        legs(JSONObject legsJSON)
        {
            JSONObject startLocation;
            try {
                duration =legsJSON.getJSONObject("duration").getString("text");
                distance = legsJSON.getJSONObject("distance").getString("text");
                durantionsec =legsJSON.getJSONObject("duration").getString("value");
                distanceValue=legsJSON.getJSONObject("distance").getString("value");
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}

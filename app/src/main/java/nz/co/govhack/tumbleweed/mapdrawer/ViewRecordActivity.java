package nz.co.govhack.tumbleweed.mapdrawer;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ivbaranov.mfb.MaterialFavoriteButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Callback;
import okhttp3.Response;

public class ViewRecordActivity extends AppCompatActivity implements RatingBar.OnRatingBarChangeListener {

    private JSONArray parksJson;
    private JSONObject mRecord;

    private RatingBar getRatingBar;
    private RatingBar setRatingBar;
    private TextView countText;
    private int count;
    private Boolean isFavorite;
    private Boolean isVisited;

    private Boolean favoriteInit = true;
    private Boolean visitedInit = true;

    private float curRate;
    private float globalRate;

    private Dialog rankDialog;

    MaterialFavoriteButton toolbarFavorite;
    MaterialFavoriteButton toolbarVisited;

    String installationId = "";
    String recordId = "";
    String playgroundName = "";
    String lat = "";
    String lon = "";
    String mark = "";
    ImageView imageView = null;

    boolean initialisation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_record);

        imageView = (ImageView) findViewById(R.id.background_image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_view);
        setSupportActionBar(toolbar);

        /*bug
        top ranked
        camera move when dont have favorite/vist
         */
        
        toolbarFavorite = (MaterialFavoriteButton) findViewById(R.id.heart);
        toolbarFavorite.setFavorite(false);
        toolbarFavorite.setOnFavoriteChangeListener(
                new MaterialFavoriteButton.OnFavoriteChangeListener() {
                    @Override
                    public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
                        if (toolbarFavorite.isFavorite()) {
                            if(!favoriteInit) {
                                updateFavorite("save");
                                Snackbar.make(buttonView, "This playground has been added to your favorites", Snackbar.LENGTH_SHORT).show();
                            }
                        } else {
                            updateFavorite("remove");
                            Snackbar.make(buttonView, "This playground has been removed from your favorites", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });

        toolbarVisited = (MaterialFavoriteButton) findViewById(R.id.check);
        toolbarVisited.setFavorite(false);
        toolbarVisited.setOnFavoriteChangeListener(
                new MaterialFavoriteButton.OnFavoriteChangeListener() {
                    @Override
                    public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
                        if (toolbarVisited.isFavorite()) {
                            if(!visitedInit) {
                                updateVisit("save");
                                Snackbar.make(buttonView, "Your visit in this playground has been saved", Snackbar.LENGTH_SHORT).show();
                            }
                        } else {
                            updateVisit("remove");
                            Snackbar.make(buttonView, "Your visit in this playground has been removed", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });

        String json = Utils.loadJSONFromAsset(getAssets());

        try {
            parksJson = new JSONArray(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Bundle b = getIntent().getExtras();
        if (b != null) {
            recordId = b.getString("record_id");
            mRecord = findRecordById(recordId);

            try {
                toolbar.setTitle(mRecord.getString("name"));
                setSupportActionBar(toolbar);

                String details = "<h2>Address</h2>" +
                        "<p>" + mRecord.getString("name") + "<br>" + mRecord.getString("address") + "</p>" +
                        "<h2>Equipment</h2>" +
                        "<p>" + mRecord.getString("equipment") + "</p>";

                String facilities = mRecord.getString("facilities");
                String about = mRecord.getString("about");

                if(facilities.length()>0) details += "<h2>Facilities</h2>" +
                                                    "<p>" + facilities + "</p>";
                if(about.length()>0) details += "<h2>About</h2>" +
                                                "<p>" + about + "</p>";

                ((TextView) findViewById(R.id.record_details)).setText(Html.fromHtml(details));

                installationId = Installation.id(getApplicationContext());
                playgroundName = mRecord.getString("name");
                lat = mRecord.getString("lat");
                lon = mRecord.getString("long");

                FloatingActionButton share = (FloatingActionButton) findViewById(R.id.share_playground);
                share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sharePlayground();
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
                Log.i("****", "Json error here", e);
            }
        }

        // add background image
        String url = "https://maps.googleapis.com/maps/api/staticmap?center=" +
                lat + "," + lon + "&zoom=19&size=600x300&maptype=satellite";
//        String url = "https://maps.googleapis.com/maps/api/streetview?size=600x300&location=" +
//                lat + "," + lon + "&heading=151.78&pitch=0";
        // String url = "https://www.nasa.gov/sites/default/files/styles/image_card_4x3_ratio/public/images/115334main_image_feature_329_ys_full.jpg";
        PictureLoader loader = new PictureLoader();
        loader.execute(new String[] {url});


        setRatingBar = (RatingBar) findViewById(R.id.setRating);
        countText = (TextView) findViewById(R.id.countText);

        recordClick();
        updateGlobalRating();
        checkIfFavorite();
        checkIfVisited();


        // ranking dialog
        //Button rankBtn = (Button) findViewById(R.id.rank_dialog_button);
        ImageButton rankBtn = (ImageButton) findViewById(R.id.rank_dialog_button);
        rankBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rankDialog = new Dialog(ViewRecordActivity.this);
                rankDialog.setContentView(R.layout.rank_dialog);
                rankDialog.setCancelable(true);
                getRatingBar = (RatingBar)rankDialog.findViewById(R.id.dialog_ratingbar);

                updateInstallationRating();
                getRatingBar.setOnRatingBarChangeListener(ViewRecordActivity.this);

                Button updateButton = (Button) rankDialog.findViewById(R.id.rank_dialog_button);
                updateButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rankDialog.dismiss();
                    }
                });
                //now that the dialog is set up, it's time to show it
                rankDialog.show();
            }
        });

    }

    private void openInGoogleMaps() {
        Uri gmmIntentUri = Uri.parse("geo:" + lat + "," + lon);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private void sharePlayground() {
        // prepare URL to google maps
        DecimalFormat df = new DecimalFormat("#.####");
        String uri = "https://www.google.com/maps/place//@" +
                df.format(Double.parseDouble(lat)) + "," +  // lat and lon rounded to 4 digits
                df.format(Double.parseDouble(lon)) +
                ",15z";                                     // zoom level in the map

        // prepare subject and body of the share
        String shareSubject = "Playground " + playgroundName;
        String shareText = uri + "\nShared via Kiwi Playground";

        // create share intent with the prepared data
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(sharingIntent,
                getResources().getText(R.string.send_to)));
    }

    private class PictureLoader extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            Bitmap map = null;
            for (String url : urls) {
                map = downloadImage(url);
            }
            return map;
        }

        // Sets the Bitmap returned by doInBackground
        @Override
        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
            System.out.println("finished");
        }

        // Creates Bitmap from InputStream and returns it
        private Bitmap downloadImage(String url) {
            Bitmap bitmap = null;
            InputStream stream = null;
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inSampleSize = 1;

            try {
                stream = getHttpConnection(url);
                bitmap = BitmapFactory.
                        decodeStream(stream, null, bmOptions);
                stream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return bitmap;
        }

        // Makes HttpURLConnection and returns InputStream
        private InputStream getHttpConnection(String urlString)
                throws IOException {
            InputStream stream = null;
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();

            try {
                HttpsURLConnection httpConnection = (HttpsURLConnection) connection;
                httpConnection.setRequestMethod("GET");
                httpConnection.connect();

                if (httpConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    stream = httpConnection.getInputStream();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return stream;
        }
    }


    private JSONObject findRecordById(String recordId) {
        try {
            for(int i = 0; i < parksJson.length(); i++) {
                    JSONObject record = (JSONObject) parksJson.get(i);
                    String id = "" + record.getInt("id");

                    if (id.equals(recordId)) {
                        return record;
                    }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateGlobalRating() {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.rating_url);
        Request request = new Request.Builder().url(url + "?playground_name=" + playgroundName).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to update rating", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject Jobject = new JSONObject(jsonData);
                    count = (int) Jobject.getDouble("count");
                    if(count>0) {
                        globalRate = (float) Jobject.getDouble("rating");
                    }
                } catch (JSONException e) {
                    Log.i("****", "Rating update has failed", e);
                } catch (IOException e) {
                    Log.i("****", "Rating update has failed", e);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setRatingBar.setRating((float) globalRate);
                        if(count == 0) {
                            countText.setText("No rating yet");
                        } else if(count == 1) {
                            countText.setText("1 user rating");
                        } else {
                            countText.setText(count + " user ratings");
                        }
                    }
                });
                Log.i("****", "Rating has been updated");
                Log.i("****", "The Http response is: " + response.toString());
            }
        });
    }

    private void updateInstallationRating() {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.rating_url);
        Request request = new Request.Builder().url(url + "?playground_name=" + playgroundName + "&installation_id=" + installationId).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to update rating", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject Jobject = new JSONObject(jsonData);
                    count = (int) Jobject.getDouble("count");
                    if(count>0) {
                        curRate = (float) Jobject.getDouble("rating");
                    }
                } catch (JSONException e) {
                    Log.i("****", "Rating installation update has failed", e);
                } catch (IOException e) {
                    Log.i("****", "Rating installation update has failed", e);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getRatingBar.setRating((float) curRate);
                    }
                });
                Log.i("****", "The Http response is: " + response.toString());
            }
        });
        initialisation = false;
    }

    private void postRating() {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.rating_url);
        FormBody formBody = new FormBody.Builder()
                .add("installation_id", installationId)
                .add("record_id", recordId)
                .add("playground_name", playgroundName)
                .add("mark", mark)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to record rating", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                updateGlobalRating();
                Log.i("****", "Rating has been recorded");
                Log.i("****", "The Http response is: " + response.toString());
            }
        });
    }

    private void updateFavorite(String type) {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.remove_favorite_url); // default behavior is remove
        if(type.equals("save")) {
            url = getResources().getString(R.string.save_favorite_url);
        }
        FormBody formBody = new FormBody.Builder()
                .add("installation_id", installationId)
                .add("record_id", recordId)
                .add("playground_name", playgroundName)
                .add("type", type)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to update favorite", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                updateGlobalRating();
                Log.i("****", "Favorite has been updated");
                Log.i("****", "The Http response is: " + response.toString());
            }
        });
        isFavorite = false; // default behavior is remove
        if(type.equals("save")) {
            isFavorite = true;
        }
    }

    private void updateVisit(String type) {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.remove_visit_url); // default behavior is remove
        if(type.equals("save")) {
            url = getResources().getString(R.string.save_visit_url);
        }
        FormBody formBody = new FormBody.Builder()
                .add("installation_id", installationId)
                .add("record_id", recordId)
                .add("playground_name", playgroundName)
                .add("type", type)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to update visit", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                updateGlobalRating();
                Log.i("****", "Visit has been updated");
                Log.i("****", "The Http response is: " + response.toString());
            }
        });
        isVisited = false; // default behavior is remove
        if(type.equals("save")) {
            isVisited = true;
        }
    }

    private void unRegisterFavorite(){
        isFavorite = false;
    }

    private void registerVisit() {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.save_visit_url);
        FormBody formBody = new FormBody.Builder()
                .add("installation_id", installationId)
                .add("record_id", recordId)
                .add("playground_name", playgroundName)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to register visit", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                updateGlobalRating();
                Log.i("****", "Visit has been registered");
                Log.i("****", "The Http response is: " + response.toString());
            }
        });
        isVisited = true;
    }

    private void unRegisterVisit() {
        isVisited = false;
    }

    private void recordClick() {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.store_click_url);
        FormBody formBody = new FormBody.Builder()
                .add("installation_id", installationId)
                .add("record_id", recordId)
                .add("playground_name", playgroundName)
                .add("latitude", lat)
                .add("longitude", lon)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to record click", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("****", "Click has been recorded");
                Log.i("****", "The Http response is: " + response.toString());
            }
        });
    }

    private void checkIfFavorite() {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.check_favorite_url);
        Request request = new Request.Builder().url(url + "?playground_name=" + playgroundName + "&installation_id=" + installationId).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to check if it is favorite", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject Jobject = new JSONObject(jsonData);
                    isFavorite = (Boolean) Jobject.getBoolean("check");
                } catch (JSONException e) {
                    Log.i("****", "Favorite check failed", e);
                } catch (IOException e) {
                    Log.i("****", "Favorite check failed", e);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toolbarFavorite.setFavorite(isFavorite);
                        favoriteInit = false;
                    }
                });
                Log.i("**** check if favorite", "The Http response is: " + response.toString());
            }
        });
    }

    private void checkIfVisited() {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.check_visit_url);
        Request request = new Request.Builder().url(url + "?playground_name=" + playgroundName + "&installation_id=" + installationId).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to check if it has been visited", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject Jobject = new JSONObject(jsonData);
                    isVisited = (Boolean) Jobject.getBoolean("check");
                } catch (JSONException e) {
                    Log.i("****", "Visit check failed", e);
                } catch (IOException e) {
                    Log.i("****", "Visit check failed", e);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toolbarVisited.setFavorite(isVisited);
                        visitedInit = false;
                    }
                });
                Log.i("**** check if visited", "The Http response is: " + response.toString());
            }
        });
    }


    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        mark = String.valueOf(Math.round(rating));
        if(!initialisation) {
            postRating();
        }
    }


}

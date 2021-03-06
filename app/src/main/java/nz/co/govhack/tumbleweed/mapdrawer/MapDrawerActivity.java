package nz.co.govhack.tumbleweed.mapdrawer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;


import com.google.maps.android.clustering.ClusterManager;

public class MapDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private GoogleMap mMap;
    private JSONArray allParksJson;
    public JSONArray parksJson;

    private ArrayList<String> favoriteParks;
    String installationId = "";
    MapsFragment fragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // load the map fragment
        fragment = (MapsFragment)getSupportFragmentManager().findFragmentById(R.id.map);

        // load stuff required for filtering --> the installation id and the parks
        installationId = Installation.id(getApplicationContext());
        Installation.getToken(installationId, getApplicationContext());

        String json = Utils.loadJSONFromAsset(this.getApplicationContext().getAssets());
        try {
            allParksJson = new JSONArray(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        parksJson = allParksJson;

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //fragment = (MapsFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mMap = fragment.getMyMap();

        if (mMap == null) {
            return false;
        }

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_map_type_normal) {
            mMap.setMapType(MAP_TYPE_NORMAL);
        } else if (id == R.id.action_map_type_hybrid) {
            mMap.setMapType(MAP_TYPE_HYBRID);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.favorite_playgrounds) {
            try {
                try {
                    getFavoritesPlaygrounds();
                } catch (InvalidKeyException e) {
                    Toast.makeText(this.getApplicationContext(), "Error with the authentication token", Toast.LENGTH_LONG).show();
                } catch (NoSuchAlgorithmException e) {
                    Toast.makeText(this.getApplicationContext(), "Error with the authentication token", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Toast.makeText(this.getApplicationContext(), "Error with the authentication token", Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.all_playgrounds) {
            parksJson = new JSONArray();
            for(int i = 0; i < allParksJson.length(); i++) {
                try {
                    String name = ((JSONObject) allParksJson.get(i)).getString("name");
                    parksJson.put((JSONObject) allParksJson.get(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            fragment.getMyMap().clear();
            fragment.onMapReady(fragment.getMyMap());
        } else if (id == R.id.visited_playgrounds) {
            try {
                try {
                    getVisitedPlaygrounds();
                } catch (InvalidKeyException e) {
                    Toast.makeText(this.getApplicationContext(), "Error with the authentication token", Toast.LENGTH_LONG).show();
                } catch (NoSuchAlgorithmException e) {
                    Toast.makeText(this.getApplicationContext(), "Error with the authentication token", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Toast.makeText(this.getApplicationContext(), "Error with the authentication token", Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.top_playgrounds) {
            try {
                try {
                    getTopPlaygrounds(getResources().getInteger(R.integer.top));
                } catch (InvalidKeyException e) {
                    Toast.makeText(this.getApplicationContext(), "Error with the authentication token", Toast.LENGTH_LONG).show();
                } catch (NoSuchAlgorithmException e) {
                    Toast.makeText(this.getApplicationContext(), "Error with the authentication token", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Toast.makeText(this.getApplicationContext(), "Error with the authentication token", Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.nav_share) {
            shareApp();

        } else if (id == R.id.nav_send) {
            openPlayStore();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void shareApp() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out the Kiwi Playground app! http://bit.ly/kiwi-playgrounds");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
    }

    private void openPlayStore() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=nz.co.govhack.tumbleweed.mapdrawer"));
        startActivity(intent);
    }


    /* Endpoint Calls functions */


    private void getFavoritesPlaygrounds() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.get_favorite_list_url);
        Request request = new Request.Builder().url(url + "?installation_id=" + installationId)
                .addHeader("Id", installationId)
                .addHeader("Token", Installation.readTokenFile(getApplicationContext())).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to get favorites playgrounds", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject Jobject = new JSONObject(jsonData);
                        JSONArray playgrounds = Jobject.getJSONArray("playground_name_list");
                        favoriteParks = new ArrayList<String>();
                        if (playgrounds != null) {
                            for (int i = 0; i < playgrounds.length(); i++) {
                                favoriteParks.add(playgrounds.get(i).toString());
                            }
                        }

                        parksJson = new JSONArray();
                        for (int i = 0; i < allParksJson.length(); i++) {
                            try {
                                String name = ((JSONObject) allParksJson.get(i)).getString("name");
                                if (favoriteParks.contains(name))
                                    parksJson.put((JSONObject) allParksJson.get(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    } catch (JSONException e) {
                        Log.i("****", "Favorite Playgrounds filtering failed", e);
                    } catch (IOException e) {
                        Log.i("****", "Favorite Playgrounds filtering failed", e);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fragment.getMyMap().clear();
                            fragment.onMapReady(fragment.getMyMap());
                        }
                    });

                    Log.i("****", "The Http response is: " + response.toString());
            } else {
                Log.i("****", "unsuccessful http request");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_LONG).show();
                        }
                    });
            }
          }
       });
    }

    private void getVisitedPlaygrounds() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.get_visit_list_url);
        Request request = new Request.Builder().url(url + "?installation_id=" + installationId)
                .addHeader("Id", installationId)
                .addHeader("Token", Installation.readTokenFile(getApplicationContext())).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to get visited playgrounds", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                try {
                    String jsonData = response.body().string();
                    JSONObject Jobject = new JSONObject(jsonData);
                    JSONArray playgrounds = Jobject.getJSONArray("playground_name_list");
                    favoriteParks = new ArrayList<String>();
                    if (playgrounds != null) {
                        for (int i=0; i<  playgrounds.length(); i++){
                            favoriteParks.add(playgrounds.get(i).toString());
                        }
                    }

                    parksJson = new JSONArray();
                    for(int i = 0; i < allParksJson.length(); i++) {
                        try {
                            String name = ((JSONObject) allParksJson.get(i)).getString("name");
                            if(favoriteParks.contains(name)) parksJson.put((JSONObject) allParksJson.get(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (JSONException e) {
                    Log.i("****", "Visited Playgrounds filtering failed", e);
                } catch (IOException e) {
                    Log.i("****", "Visited Playgrounds filtering failed", e);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fragment.getMyMap().clear();
                        fragment.onMapReady(fragment.getMyMap());
                    }
                });

                Log.i("****", "The Http response is: " + response.toString());
            } else {
                  Log.i("****", "unsuccessful http request");
                  runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_LONG).show();
                    }
                    });
            }
          }
        });
    }

    private void getTopPlaygrounds(int top) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        OkHttpClient client = new OkHttpClient();
        String url = getResources().getString(R.string.get_top_url);
        Request request = new Request.Builder().url(url + "?top=" + String.valueOf(top))
                .addHeader("Id", installationId)
                .addHeader("Token", Installation.readTokenFile(getApplicationContext())).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to get top ranked playgrounds", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject Jobject = new JSONObject(jsonData);
                        JSONArray playgrounds = Jobject.getJSONArray("playground_name_list");
                        favoriteParks = new ArrayList<String>();
                        if (playgrounds != null) {
                            for (int i = 0; i < playgrounds.length(); i++) {
                                favoriteParks.add(playgrounds.get(i).toString());
                            }
                        }

                        parksJson = new JSONArray();
                        for (int i = 0; i < allParksJson.length(); i++) {
                            try {
                                String name = ((JSONObject) allParksJson.get(i)).getString("name");
                                if (favoriteParks.contains(name))
                                    parksJson.put((JSONObject) allParksJson.get(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                    } catch (JSONException e) {
                        Log.i("****", "Top ranked Playgrounds filtering failed", e);
                    } catch (IOException e) {
                        Log.i("****", "Top ranked Playgrounds filtering failed", e);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fragment.getMyMap().clear();
                            fragment.onMapReady(fragment.getMyMap());
                        }
                    });
                    Log.i("****", "The Http response is: " + response.toString());
                } else {
                    Log.i("****", "unsuccessful http request");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

}





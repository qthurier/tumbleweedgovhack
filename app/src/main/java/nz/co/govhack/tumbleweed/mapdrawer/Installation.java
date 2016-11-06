package nz.co.govhack.tumbleweed.mapdrawer;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Installation {
    private static String sID = null;
    private static String sToken = null;
    private static final String INSTALLATION = "INSTALLATION";
    private static final String TOKEN = "TOKEN";
    private static Context mContext;

    public synchronized static String id(Context context) {
        if (sID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists())
                    writeInstallationFile(installation);
                sID = readInstallationFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    public synchronized static String token(Context context) {
        String id = id(context);
        if (sToken == null) {
            File tokenFile = new File(context.getFilesDir(), TOKEN);
            mContext = context;
            try {
                if (!tokenFile.exists()) writeTokenFile(id);
                while(!tokenFile.exists()) Thread.sleep(100); //birk
                sToken = readTokenFile(tokenFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sToken;
    }

    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }

    private static void writeTokenFile(String id) throws IOException {
        OkHttpClient client = new OkHttpClient();
        String url = mContext.getResources().getString(R.string.get_token_url);
        Request request = new Request.Builder().url(url + "?installation_id=" + id).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("****", "Failed to get token", e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200) {
                    try {
                        String jsonData = response.body().string();
                        JSONObject Jobject = new JSONObject(jsonData);
                        String token = Jobject.getString("token");
                        File tokenFile = new File(mContext.getFilesDir(), TOKEN);
                        FileOutputStream out = new FileOutputStream(tokenFile);
                        out.write(token.getBytes());
                        out.close();
                    } catch (JSONException e) {
                        Log.i("****", "Get Access token failed", e);
                    } catch (IOException e) {
                        Log.i("****", "Get Access token failed", e);
                    }
                    Log.i("****", "The Http response is: " + response.toString());
                } else {
                    Log.i("****", "unsuccessful http request");
                }
            }
        });
    }

    private static String readTokenFile(File token) throws IOException {
        RandomAccessFile f = new RandomAccessFile(token, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

}
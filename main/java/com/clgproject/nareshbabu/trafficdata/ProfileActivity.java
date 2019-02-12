package com.clgproject.nareshbabu.trafficdata;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.input.InputManager;
import android.location.Geocoder;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;
import com.squareup.picasso.Picasso;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    ImageView profileImage;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    RatingBar para,viol,analy;
    float r1,r2,r3;
    EditText editText;
    Bitmap resized;
    private SharedPreferences sharedPreferences;
    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        analy = findViewById(R.id.ratingBar);
        viol = findViewById(R.id.ratingBar2);
        para = findViewById(R.id.ratingBar3);

        profileImage = findViewById(R.id.imageView);
        sharedpreferences = getApplicationContext().getSharedPreferences("preferences", Activity.MODE_PRIVATE);
        editor = sharedpreferences.edit();
        Bitmap image = StringToBitMap(sharedpreferences.getString("profilepicture", null));
        resized = image;
        r1 = sharedpreferences.getFloat("r1", (float) 2.5);
        r2 = sharedpreferences.getFloat("r2", (float) 2.5);
        r3 = sharedpreferences.getFloat("r3", (float) 2.5);
        editText = findViewById(R.id.editText);
        String s = sharedpreferences.getString("et", "Name");
        if(image!=null) {
            profileImage.setImageBitmap(image);
            analy.setRating(r1);
            viol.setRating(r2);
            para.setRating(r3);
            editText.setText(s);
        }


    }

    //Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        editor.putString("et", editText.getText().toString());
        editor.putFloat("r1", analy.getRating());
        editor.putFloat("r2", viol.getRating());
        editor.putFloat("r3", para.getRating());
        editor.commit();
        Toast.makeText(ProfileActivity.this, viol.getRating()+"", Toast.LENGTH_SHORT).show();

        new PostDataAsyncTask().execute();
        finish();
        return super.onOptionsItemSelected(item);
    }

    public void pickImageFromGallery(View view) {
        Intent intent = new Intent();
        //******call android default gallery
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        //******code for crop image
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 0);
        intent.putExtra("aspectY", 0);
        try {
            intent.putExtra("return-data", true);
            startActivityForResult(
                    Intent.createChooser(intent,"Complete action using"),
                    1);
        } catch (ActivityNotFoundException e) {}
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);


        if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                resized = Bitmap.createScaledBitmap(selectedImage, 200, 200, true);
                String s = BitMapToString(resized);
                editor.putString("profilepicture",s);
                editor.commit();
                Log.v("HI", "onActivityResult: "+s );
                profileImage.setImageBitmap(resized);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }
    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp=Base64.encodeToString(b, Base64.DEFAULT);
        return temp;

    }
    public Bitmap StringToBitMap(String encodedString){
        try{
            byte [] encodeByte= Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap=BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        }catch(Exception e){
            e.getMessage();
            return null;
        }
    }

    public class PostDataAsyncTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();
            // do stuff before posting data
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                String TAG = "update";
                String postReceiverUrl = "http://kalamsdream.000webhostapp.com/profile_receiver.php";
                Log.v(TAG, "postURL: " + postReceiverUrl);

                // HttpClient
                HttpClient httpClient = new DefaultHttpClient();

                // post header
                HttpPost httpPost = new HttpPost(postReceiverUrl);

                // add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                JSONObject jsonParam = new JSONObject();

                jsonParam.put("id", Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID));
                jsonParam.put("name", editText.getText().toString());
                jsonParam.put("r1",  (int)analy.getRating());
                jsonParam.put("r2",(int)viol.getRating());
                jsonParam.put("r3", (int)para.getRating());
                jsonParam.put("image", BitMapToString(resized));

                nameValuePairs.add(new BasicNameValuePair("json", jsonParam.toString()));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // execute HTTP post request
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity resEntity = response.getEntity();

                if (resEntity != null) {

                    String responseStr = EntityUtils.toString(resEntity).trim();
                    Log.v(TAG, "Response: " +  responseStr);

                    // you can add an if statement here and do other actions based on the response
                }
                Log.v(TAG, "Response: "+jsonParam.toString());


            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return  null;
        }

            @Override
        protected void onPostExecute(String lenghtOfFile) {
            // do stuff after posting data
        }
    }

}

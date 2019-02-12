package com.clgproject.nareshbabu.trafficdata;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends AppCompatActivity implements LoaderCallbacks<Void> {

    private static final String TAG = "MainActivity";

    int CUR_STATE;
    RadioGroup vehicle;
    RadioButton Custom;
    Button stopButton;
    EditText custom_vehicle;
    Spinner time_interval;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    FrameLayout allForOne;
    EditText startLoc;
    EditText endLoc;
    TextView heading, note;
    int height;
    int width;
    String modeOfTransport;
    long timeInterval;
    String startLocation;
    String endLocation;
    ImageView stateIcon;
    TextView startLocShower, endLocShower;
    ProgressBar uploadProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "onCreate: ");
        sharedpreferences = getApplicationContext().getSharedPreferences("preferences", Activity.MODE_PRIVATE);
        editor = sharedpreferences.edit();


        setContentView(R.layout.activity_main);
        CUR_STATE = sharedpreferences.getInt("cur_state", 0);
        stateIcon = findViewById(R.id.state_icon);
        if (CUR_STATE < getBaseContext().getResources().getInteger(R.integer.RUNNING))
            CUR_STATE = 0;
        startLocation = sharedpreferences.getString("start_loc", "start");
        endLocation = sharedpreferences.getString("end_loc", "end");
        timeInterval = sharedpreferences.getLong("time_interval", 120);

        allForOne = findViewById(R.id.frameLayout);
        startLoc = findViewById(R.id.start);
        endLoc = findViewById(R.id.end);
        note = findViewById(R.id.note);
        heading = findViewById(R.id.heading);
        vehicle = findViewById(R.id.vehicle);
        Custom = findViewById(R.id.Custom);
        custom_vehicle = findViewById(R.id.custom_vehicle);
        time_interval = (Spinner) findViewById(R.id.time_interval);
        //time_interval.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        startLocShower = findViewById(R.id.start_location_shower);
        endLocShower = findViewById(R.id.end_location_shower);
        stopButton = findViewById(R.id.stop_button);
        uploadProgress = findViewById(R.id.upload_progress);

        //Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.intervals_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        time_interval.setAdapter(adapter);
        time_interval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(MainActivity.this, ""+position, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "onItemSelected: "+position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        time_interval.setSelection(1,false);

        //Height and Width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));
        }

        //Ask permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1
            );
        }

        if (CUR_STATE == 3 || CUR_STATE == 4) {
            if (CUR_STATE == 3) {
                heading.setText("Running");
                stateIcon.setImageResource(R.mipmap.pause);
                stateIcon.animate().translationX(-width / 50).setDuration(500).alpha(1);
            } else
                heading.setText("Paused");
            loadRunningState();
        }
        if (CUR_STATE == getBaseContext().getResources().getInteger(R.integer.STOPPED))
            allStop();

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertStop();
            }
        });
        allForOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CUR_STATE == getBaseContext().getResources().getInteger(R.integer.START)) {
                    //startApp();
                    CUR_STATE++;
                    getLoc();
                } else if (CUR_STATE == getBaseContext().getResources().getInteger(R.integer.LOCATION)) {
                    getLoc();

                } else if (CUR_STATE == getBaseContext().getResources().getInteger(R.integer.MODE)) {
                    getMode();

                } else if (CUR_STATE == getBaseContext().getResources().getInteger(R.integer.RUNNING)) {
                    pausedState();
                } else if (CUR_STATE == getBaseContext().getResources().getInteger(R.integer.PAUSED)) {
                    resumerState();
                } else if (CUR_STATE == getBaseContext().getResources().getInteger(R.integer.STOPPED)) {
                    //UPLOAD
                    if(haveNetworkConnection())
                        upload();
                    else
                        Toast.makeText(MainActivity.this, "No internet", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause: ");
        editor.clear();
        editor = sharedpreferences.edit();
        editor.putInt("cur_state", CUR_STATE);
        editor.apply();
        super.onPause();


    }

    @Override
    protected void onDestroy() {
        editor.clear();
        editor = sharedpreferences.edit();
        editor.putInt("cur_state", CUR_STATE);
        editor.apply();
        super.onDestroy();
    }

    private void startApp() {

        heading.animate().setDuration(500).alpha(0);
        allForOne.animate().setDuration(1000).scaleX(0.5f).scaleY(0.5f).translationY(height / 8).translationX(width / 3);
        startLoc.setVisibility(View.INVISIBLE);
        startLoc.animate().translationX(-width);
        endLoc.setVisibility(View.INVISIBLE);
        endLoc.animate().translationX(width);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                heading.setText("Enter Location");
                heading.animate().alpha(1).setDuration(500);
                startLoc.setVisibility(View.VISIBLE);
                endLoc.setVisibility(View.VISIBLE);
                startLoc.animate().translationX(0).setDuration(500);
                endLoc.animate().translationX(0).setDuration(500);
            }
        }, 501);
        CUR_STATE++;
    }

    private void getLoc() {

//        startLocation = startLoc.getText().toString();
//        endLocation = endLoc.getText().toString();
//        if (startLocation.equals("") || endLocation.equals("")) {
//            Toast.makeText(this, "Enter Location", Toast.LENGTH_SHORT).show();
//        } else {
//            editor.clear();
//            editor = sharedpreferences.edit();
//            editor.putString("start_loc", startLocation);
//            editor.putString("end_loc", endLocation);
//            editor.apply();
            heading.animate().translationYBy(-height / 8).setDuration(500).alpha(0);
//            startLoc.animate().translationX(width).setDuration(500);
//            endLoc.animate().translationX(-width).setDuration(500);
        allForOne.animate().setDuration(1000).scaleX(0.5f).scaleY(0.5f).translationY(height / 6 + height / 10).translationX(width / 3);

        //allForOne.animate().setDuration(500).translationY(height / 6 + height / 10);
            vehicle.setVisibility(View.INVISIBLE);
            Custom.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (custom_vehicle.getVisibility() == View.GONE) {
                        custom_vehicle.setVisibility(View.VISIBLE);
                    } else
                        custom_vehicle.setVisibility(View.GONE);
                }
            });

            time_interval.setVisibility(View.INVISIBLE);
            time_interval.setAlpha(0);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startLoc.setVisibility(View.GONE);
                    endLoc.setVisibility(View.GONE);
                    heading.setText("Enter Modes");
                    heading.animate().alpha(1).setDuration(500);
                    vehicle.setVisibility(View.VISIBLE);
                    time_interval.setVisibility(View.VISIBLE);
                    vehicle.animate().setDuration(500).alpha(1);
                    time_interval.animate().setDuration(500).alpha(1);
                }
            }, 501);
            CUR_STATE++;


    }

    private void getMode() {

        switch (vehicle.getCheckedRadioButtonId()) {
            case R.id.Bike:
                modeOfTransport = "BIKE";
                break;
            case R.id.Car:
                modeOfTransport = "CAR";
                break;
            case R.id.Auto:
                modeOfTransport = "AUTO";
                break;
            case R.id.Bus:
                modeOfTransport = "BUS";
                break;
            default:
                modeOfTransport = custom_vehicle.getText().toString();
        }





        if (modeOfTransport.equals(""))
            Toast.makeText(this, "Enter Choice", Toast.LENGTH_SHORT).show();
        else {
            editor.clear();
            editor = sharedpreferences.edit();
            timeInterval = (time_interval.getSelectedItemPosition()+1)*60;
            Log.e(TAG, "getMode: "+timeInterval);
            editor.putString("vehicle", modeOfTransport);
            editor.putLong("time_interval", timeInterval);
            editor.apply();
            heading.animate().alpha(0).setDuration(500).translationX(0);
            vehicle.animate().setDuration(500).alpha(0);
            if (custom_vehicle.getVisibility() == View.VISIBLE)
                custom_vehicle.animate().setDuration(500).alpha(0);
            time_interval.animate().setDuration(500).alpha(0);
            stateIcon.animate().alpha(0).setDuration(500);
            allForOne.animate().setDuration(1000).translationX(0).translationY(0).scaleY(1).scaleX(1);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    heading.setText("Running");
                    vehicle.setVisibility(View.GONE);
                    custom_vehicle.setVisibility(View.GONE);
                    time_interval.setVisibility(View.GONE);
                    heading.animate().alpha(1).setDuration(500);
                    stateIcon.setImageResource(R.mipmap.pause);
                    stateIcon.animate().translationX(-width / 50).setDuration(500).alpha(1);
                    loadRunningState();
                }
            }, 501);
            CUR_STATE++;
            Intent myService = new Intent(getApplicationContext(), GpsService.class).putExtra("interval", timeInterval);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(myService);
            } else {
                startService(myService);
            }
        }

    }

    private void loadRunningState() {
        heading.setTranslationY(0);
//        startLocShower.setText(startLocation);
//        endLocShower.setText(endLocation);
//        startLocShower.setVisibility(View.VISIBLE);
//        endLocShower.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.VISIBLE);

    }

    private void pausedState() {
        stopService(new Intent(getBaseContext(), GpsService.class));
        heading.animate().alpha(0).setDuration(500);
        stateIcon.animate().alpha(0).translationX(0).setDuration(500);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                heading.setText("Paused");
                heading.animate().alpha(1).setDuration(500);
                stateIcon.setImageResource(R.mipmap.play);
                stateIcon.animate().alpha(1).setDuration(500);
            }
        }, 501);
        CUR_STATE = 4;
    }

    private void resumerState() {
        Intent myService = new Intent(getApplicationContext(), GpsService.class).putExtra("interval", timeInterval);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(myService);
        } else {
            startService(myService);
        }

        heading.animate().alpha(0).setDuration(500);
        stateIcon.animate().alpha(0).translationX(-width / 50).setDuration(500);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                heading.setText("Running");
                heading.animate().alpha(1).setDuration(500);
                stateIcon.setImageResource(R.mipmap.pause);
                stateIcon.animate().alpha(1).setDuration(500);
            }
        }, 501);
        CUR_STATE = 3;
    }

    private void alertStop() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("STOP");
        builder.setMessage("Do you want to end your data collection ?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Now upload with internet", Toast.LENGTH_SHORT).show();
                CUR_STATE = 5;
                stopService(new Intent(getBaseContext(), GpsService.class));
                allStop();

            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Stop avoided", Toast.LENGTH_SHORT).show();

            }
        });

        builder.show();
    }

    private void allStop() {
        if (stopButton.getVisibility() == View.VISIBLE) {
            stopButton.animate().setDuration(500).alpha(0);
            heading.animate().setDuration(500).alpha(0);
            startLocShower.animate().setDuration(500).alpha(0);
            endLocShower.animate().setDuration(500).alpha(0);

            stateIcon.animate().alpha(0).setDuration(400);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    heading.setText("Upload");
                    startLocShower.setVisibility(View.GONE);
                    endLocShower.setVisibility(View.GONE);
                    stopButton.setVisibility(View.GONE);
                    heading.animate().alpha(1).setDuration(500).translationY(0);
                    stateIcon.setImageResource(R.mipmap.upload);
                    stateIcon.animate().translationX(-width / 50).setDuration(500).alpha(1);
                    Handler handler1 = new Handler();
                    handler1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            allForOne.performClick();

                        }
                    }, 502);
                }
            }, 501);
        } else {
            heading.setText("Upload");
            stateIcon.setImageResource(R.mipmap.upload);
            stateIcon.setTranslationX(-width / 50);
            allForOne.performClick();
        }
    }

    private void upload() {

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(1, null, this);

        heading.animate().setDuration(100).alpha(0);
        uploadProgress.setVisibility(View.VISIBLE);
        allForOne.animate().scaleX(0).scaleY(0).setDuration(100);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                heading.setText("Done");
                heading.animate().alpha(1).setDuration(500);
                allForOne.setVisibility(View.GONE);
            }
        }, 101);
    }

    @Override
    public DataLoader onCreateLoader(int id, Bundle args) {
        return new DataLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Void> loader, Void data) {
        stateIcon.setImageResource(R.mipmap.tick);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(sharedpreferences.getInt("responsecode", 0)==200) {
                    heading.setText("Done");
                    CUR_STATE = 0;
                }
                else
                    heading.setText("Done");
                allForOne.setVisibility(View.VISIBLE);
                allForOne.setClickable(false);
                allForOne.animate().scaleY(1).scaleX(1).setDuration(500);
                uploadProgress.setVisibility(View.GONE);
                Intent i = new Intent(getBaseContext(),MainActivity.class);
                startActivity(i);
                finish();
            }
        }, 101);



    }

    @Override
    public void onLoaderReset(Loader<Void> loader) {

    }

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

    }

    //Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.action_save) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("RESTART");
            builder.setMessage("Do you want to clear current data and close ?");
            builder.setCancelable(false);
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CUR_STATE = 0;
                    stopService(new Intent(getBaseContext(), GpsService.class));
                    String filename = "jsondata.txt";
                    File file = new File(getApplicationContext().getFilesDir() + "/" + filename);
                    if (file.exists())
                        file.delete();
                    finish();
                }

            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getApplicationContext(), "Restart terminated", Toast.LENGTH_SHORT).show();

                }
            });

            builder.show();
        }
        else {
            Intent myIntent = new Intent(this, ProfileActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}

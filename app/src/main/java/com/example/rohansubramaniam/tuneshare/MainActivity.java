package com.example.rohansubramaniam.tuneshare;

import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothLeAdvertiser advertiser;
    private AdvertiseData mAdvertiseData;
    private AdvertiseData mAdvertiseScanResponse;
    private AdvertiseSettings mAdvertiseSettings;

    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGattService gattService;
    private BluetoothGattCharacteristic mPrefCharacteristic;

    private static final UUID TUNE_SHARE_SERVICE_UUID = UUID
            .fromString("0000180F-0000-1000-8000-00805f9b34fb");

    private static final UUID MUSIC_UUID = UUID
            .fromString("00000000-0000-1000-8000-00805f9b34fb");


    private BluetoothManager bluetoothManager;



    private static final String TAG = MainActivity.class.getCanonicalName();


    // UI Components
    private TextView message;

    // Spotify components
    private static final String CLIENT_ID = "e8de4117302a4ad19dbe0f0ddd79af1c";
    private static final String REDIRECT_URI = "tuneshare://tuneshare-app";
    private SpotifyAppRemote mSpotifyAppRemote;
    private static final int REQUEST_CODE = 1337;

    private String authToken;
    private JSONArray spotifyData;




    @Override
    protected void onStart() {
        super.onStart();
        checkAuthToken();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();


        message = (TextView) findViewById(R.id.message);


        mAdvertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build();
        mAdvertiseScanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();
        mAdvertiseData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .build();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        advertiser.stopAdvertising(mAdvertiseCallback);
        if (mBluetoothGattServer != null) {
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
        }

    }


    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    // TODO: Save token somewhere, so that it can be used!
                    Log.i(TAG,"Token received!");
                    authToken = response.getAccessToken();
                    SharedPreferences.Editor editor = getSharedPreferences("spotify_auth", MODE_PRIVATE).edit();
                    editor.putString("token", response.getAccessToken());
                    editor.apply();

                    enableAndStartBluetooth();
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    Log.e(TAG, response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    Log.i(TAG, "Canceled login");
                    // Handle other cases
            }
        }
        if (resultCode == 1) {
            Log.i(TAG, "Starting bluetooth");
            startBluetooth();
        }

    }

    public void checkAuthToken() {
        SharedPreferences prefs = getSharedPreferences("spotify_auth", MODE_PRIVATE);
        String tkn = prefs.getString("token", null);
        // check if auth flow was already completed
//        if (tkn != null) {
//            authToken = tkn;
//            enableAndStartBluetooth();
//        } else {
            // perform auth
            AuthenticationRequest.Builder builder =
                    new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

            builder.setScopes(new String[]{"user-read-recently-played", "user-top-read"});
            builder.setShowDialog(true);
            AuthenticationRequest request = builder.build();
            AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

//        }

    }


    public void enableAndStartBluetooth() {
        // Enable bluetooth
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
            startBluetooth();
        }
    }

    private void startBluetooth() {
        // Get spotify data needed
//        VolleyLog.DEBUG = true;
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="https://api.spotify.com/v1/me/top/artists?time_range=medium_term&limit=10&offset=0";

        message.setText("Collecting Spotify Preferences...");

        // Request a string response from the provided URL.
        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "Successfully got response for spotify");

                        try {
                            //  Block of code to try

                            JSONObject obj = new JSONObject(response);
                            JSONArray a = obj.getJSONArray("items");
                            String[] data = new String[a.length()];
                            for (int i = 0; i < a.length(); i++) {
                                data[i] = a.getJSONObject(i).getString("id");
                                Log.i(TAG, data[i]);
                            }
                            message.setText("Preferences Received!");
                            spotifyData = new JSONArray(data);
                            startAdvertising();
                            startServer();
                        }
                        catch(Exception e) {
                            message.setText("An error has occurred, try again later");
                            Log.e(TAG, e.toString());
                            //  Block of code to handle errors
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        message.setText("An error has occurred, are you connected to the network?");
                        Log.e(TAG, error.toString());
                        Log.e(TAG, new String(error.networkResponse.data));
                    }
                }
        ){

            //This is for Headers If You Needed
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                params.put("Accept", "application/json");
                params.put("Authorization", "Bearer " + authToken);
                return params;
            }
//            //Pass Your Parameters here
//            @Override
//            protected Map<String, String> getParams() {
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("User", UserName);
//                params.put("Pass", PassWord);
//                return params;
//            }
        };

        queue.add(req);

        // Start advertising and GATT server
//        startAdvertising();
//        startServer();
    }


    public void startAdvertising() {
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        advertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, mAdvertiseScanResponse, mAdvertiseCallback);
    }

    public void startServer() {
        mBluetoothGattServer = bluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }
        initializeGattService();
        mBluetoothGattServer.addService(gattService);
        Log.i(TAG, "Successfully started server!");
        message.setText("Please wait for Connection from server");


    }

    public void initializeGattService() {

        gattService = new BluetoothGattService(TUNE_SHARE_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //Characteristic that sends data to the Dragonboard
        mPrefCharacteristic =
                new BluetoothGattCharacteristic(MUSIC_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ |
                                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ);

        mPrefCharacteristic.setValue("Get data here");
        gattService.addCharacteristic(mPrefCharacteristic);

    }

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        //reference: https://developer.android.com/reference/android/bluetooth/le/AdvertiseCallback.html
        //Bluetooth LE advertising callbacks, used to deliver advertising operation status.
        public void onStartFailure(int errorCode) {
            //Callback when advertising could not be started.
            super.onStartFailure(errorCode);
            Log.e(TAG, "Not broadcasting: " + errorCode);
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        /*
        Callback triggered in response to startAdvertising(AdvertiseSettings, AdvertiseData,
        AdvertiseCallback) indicating that the advertising has been started successfully.
        */
            super.onStartSuccess(settingsInEffect);
            Log.v(TAG, "Broadcasting");
        }


    };



    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {


        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            //A remote client has requested to read a local characteristic.
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
//            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
//            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
//            if (offset != 0) {
//                //mResponse is the response sent whenever a response is requested from the Dragonboard
//                //When null, the Dragonboard ceases to communicate with the Android App after some seconds
//                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
//                        mResponse.getBytes());
//                return;
//            }
            Log.i(TAG, "MESSAGE");


            if (spotifyData != null){
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                        offset, spotifyData.toString().getBytes());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        message.setText("Data has been sent! You may close the application");

                    }
                });

            } else {
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                        offset, "Not set up!".getBytes());

            }



        }

        @Override
        //This abstract class is used to implement BluetoothGattServer callbacks.
        public void onConnectionStateChange(BluetoothDevice device, final int status, int state) {
            //Callback indicating when a remote device has been connected or disconnected.
            super.onConnectionStateChange(device, status, state);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("INFO", "SUCCESS");


            } else {
                Log.e("ErROR", "fucked");
            }
        }

//        @Override
//        public void

    };

}

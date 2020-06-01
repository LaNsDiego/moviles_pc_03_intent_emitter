package com.example.emisor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.android.gms.location.FusedLocationProviderClient;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GoogleMap.OnCameraIdleListener,GoogleMap.OnMapClickListener, OnMapReadyCallback {
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 5;
    private boolean mLocationPermissionGranted;
    private Location mLastKnownLocation;
    private GoogleMap mMap;
    private Marker marker;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private final LatLng mDefaultLocation = new LatLng(-18.0058389, -70.2254838);
    private static final int CAMERA_CAPTURE_REQUEST = 1;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";


    private static final String TAG = "DIEGO:";
    private EditText edtName , edtLastName , edtPhoneNumber , edtEstudentCode ;

    private Button btnTakePhoto,btnSendToReceptor;
    private ImageView imgCaptured;
    public String strLatitude,strLongitude;
    public EditText edtAddress;
    private Uri uriImage;
    FragmentManager supportFragmentManager;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        supportFragmentManager = getSupportFragmentManager();
        initComponents();
        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getDeviceLocation();



    }

    private void initComponents(){
        imgCaptured = findViewById(R.id.img_captured);
        edtName = findViewById(R.id.edt_name);
        edtLastName = findViewById(R.id.edt_last_name);
        edtPhoneNumber = findViewById(R.id.edt_phone_number);
        edtAddress = findViewById(R.id.edt_address);
        edtEstudentCode = findViewById(R.id.edt_student_code);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnSendToReceptor = findViewById(R.id.btn_send_to_receptor_app);
        edtAddress.setEnabled(false);
        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentCamera =  new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intentCamera,MainActivity.CAMERA_CAPTURE_REQUEST);
            }
        });

        btnSendToReceptor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(
                        !edtName.getText().equals("") &&
                        !edtLastName.getText().equals("") &&
                        !edtPhoneNumber.getText().equals("") &&
                        !edtAddress.getText().equals("") &&
                        !edtEstudentCode.getText().equals("") &&
                        imgCaptured.getDrawable() != null
                ){
                    sendToReceptorApp(uriImage);
                }else{
                    Toast toast = Toast.makeText(getApplicationContext(),"Debe llenar los campos",Toast.LENGTH_SHORT);
                    toast.show();
                }

            }
        });


        SupportMapFragment mapFragment = (SupportMapFragment) supportFragmentManager
                .findFragmentById(R.id.mainMap);
        mapFragment.getMapAsync(this);

    }



    public void callbackSetterAddressAndPoint(String latitude , String longitude , String address){
        strLatitude = latitude;
        strLongitude = longitude;
        edtAddress.setText(address);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == MainActivity.CAMERA_CAPTURE_REQUEST && resultCode == RESULT_OK){
            Bitmap newPhoto = (Bitmap) data.getExtras().get("data");
            imgCaptured.setImageBitmap(newPhoto);
            uriImage = data.getData();
            Log.d(TAG,uriImage.toString());
        }
    }


    private void sendToReceptorApp(Uri uriToImage){
        Intent sharedIntent = new Intent();
        sharedIntent.setAction(Intent.ACTION_SEND);
        sharedIntent.putExtra("name",edtName.getText().toString());
        sharedIntent.putExtra("lastName",edtLastName.getText().toString());
        sharedIntent.putExtra("studentCode",edtEstudentCode.getText().toString());
        sharedIntent.putExtra("phoneNumber",edtPhoneNumber.getText().toString());
        sharedIntent.putExtra("address",edtAddress.getText().toString());
        sharedIntent.putExtra("latitude",strLatitude);
        sharedIntent.putExtra("longitude",strLongitude);
        sharedIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
        sharedIntent.setType("*/*");
        if(sharedIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(sharedIntent,"escoja una app"));
        }else{
            Log.d(TAG,"no se resolvio el envio de datos ");
        }



    }

//    MAPS

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);
        marker = mMap.addMarker(new MarkerOptions().position(mDefaultLocation).title("Ubicación por defecto"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation,14f));

        // Prompt the user for permission.
        getLocationPermission();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
    }

    @Override
    public void onCameraIdle() {
        LatLng centerPoint = mMap.getCameraPosition().target;
        Log.d(TAG,"Lat: " + centerPoint.latitude + "___ Long: " + centerPoint.longitude  );
        if(marker == null){
            marker.remove();
            mMap.addMarker(new MarkerOptions()
                    .position(centerPoint));


        }else{
            marker.setPosition(centerPoint);

        }
        String strAddress = getStringAddress(centerPoint.latitude,centerPoint.longitude);
        callbackSetterAddressAndPoint(
                String.valueOf(centerPoint.latitude),
                String.valueOf(centerPoint.longitude),
                strAddress
        );
    }


    private String getStringAddress(Double latitud, Double longitud){
        String address = "";
        Geocoder geocoder;
        List<Address> addresses ;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(latitud,longitud,1);
            if(addresses.size() > 0){
                Address objAddress = addresses.get(0);
                address = objAddress.getAddressLine(0);
            }else{
                address = "Desconocido";
            }
        }catch (IOException e){
        }

        return address;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }



    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            final String ar[] = new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this,ar,
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getDeviceLocation() {

        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), 15));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, 15));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

}

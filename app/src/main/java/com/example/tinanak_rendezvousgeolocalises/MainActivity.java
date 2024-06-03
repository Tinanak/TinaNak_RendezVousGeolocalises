package com.example.tinanak_rendezvousgeolocalises;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 0;
    private static final int PICK_CONTACT_REQUEST = 1;
    private static final int PICK_LOCATION_REQUEST = 2;
    private static final int SMS_PERMISSION_REQUEST_CODE = 3;
    private EditText editTextPhoneNumber, editTextMessage, editTextLocation;
    private LocationManager locationManager;
    private Location currentLocation;
    private ArrayList<String> listLocations = new ArrayList<>();
    private CustomAdapter customAdapter;

    // coordonnées GPS du RDV
    private LatLng latLng;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextMessage = findViewById(R.id.editTextMessage);
        editTextLocation = findViewById(R.id.editTextLocation);
        Button buttonSelectContacts = findViewById(R.id.buttonSelectContacts);
        Button buttonAddLocation = findViewById(R.id.location);
        Button buttonSendInvite = findViewById(R.id.buttonSendInvite);

        // Button click listeners
        buttonSelectContacts.setOnClickListener(this::selectContacts);
        buttonAddLocation.setOnClickListener(this::addLocation);
        buttonSendInvite.setOnClickListener(this::sendInvite);

        // Initialize locations list and adapter
        customAdapter = new CustomAdapter(this, listLocations.toArray(new String[0]));
        ListView listView = findViewById(R.id.list_location);
        listView.setAdapter(customAdapter);

        // Initialize location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Vérifier si les services de localisation sont activés
        checkLocationEnabled();

        // Demander la permission de localisation
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocation();
        }
    }

    private void selectContacts(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, PICK_CONTACT_REQUEST);
    }

    @SuppressLint("MissingPermission")
    private void addLocation(View view) {

        String uri = "geo:" + currentLocation.getLatitude() + "," + currentLocation.getLongitude();
        Uri geoUri = Uri.parse(uri);

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            listLocations.add(geoUri.toString());
            customAdapter.notifyDataSetChanged();
            startActivityForResult(mapIntent, PICK_LOCATION_REQUEST);
        } else {
            Toast.makeText(this, "Localisation non disponible", Toast.LENGTH_LONG).show();
        }
    }

    private void sendInvite(View view) {
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();

        if (currentLocation != null) {
            String locationMessage = "Location: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude();
            message += "\n" + locationMessage;
        }

        if (phoneNumber.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_LONG).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
        } else {
            sendSMS(phoneNumber, message);
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Toast.makeText(this, "Invitation envoyée avec succès", Toast.LENGTH_LONG).show();
        editTextMessage.setText("");
        editTextPhoneNumber.setText("");
        editTextLocation.setText("");
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    currentLocation = location;
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(@NonNull String provider) {}

                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Selction de contact
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};

            assert contactUri != null;
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(numberIndex);

                // Ajouter le numéro de téléphone sélectionné à la liste
                if (editTextPhoneNumber.getText().toString().isEmpty()) {
                    editTextPhoneNumber.setText(number);
                } else if (editTextPhoneNumber.getText().toString().contains(number)) {
                    Toast.makeText(this, "Le numéro est déjà sélectionné", Toast.LENGTH_LONG).show();
                } else {
                    editTextPhoneNumber.setText(editTextPhoneNumber.getText() + ";" + number);
                }
                cursor.close();
            }
        }

        // Selection une location sur la carte et l'ajouter à la liste, faire comme pour les contacts
        if (requestCode == PICK_LOCATION_REQUEST && resultCode == RESULT_OK) {
            this.latLng = new LatLng(
                    data.getDoubleExtra("latitude", 0.0),
                    data.getDoubleExtra("longitude", 0.0)
            );

            Uri locationUri = Uri.parse("geo:" + latLng.latitude + "," + latLng.longitude);

            assert locationUri != null;
            Cursor cursor = getContentResolver().query(locationUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int locationIndex = cursor.getColumnIndex("location");
                String location = cursor.getString(locationIndex);

                // Ajouter la location sélectionnée à la liste
                listLocations.add(location);
                customAdapter.notifyDataSetChanged();
                cursor.close();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendInvite(null);
            } else {
                Toast.makeText(this, "Permission SMS refusée", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkLocationEnabled() {
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {
            Toast.makeText(this, "Veuillez activer les services de localisation", Toast.LENGTH_LONG).show();
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } else {
            getLocation();
        }
    }

}

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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int CONTACT_PICK_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private static final int SMS_PERMISSION_REQUEST_CODE = 3;
    private EditText editTextPhoneNumber, editTextMessage;
    private LocationManager locationManager;
    private Location currentLocation;
    private ArrayList<String> listLocations = new ArrayList<>();
    private CustomAdapter customAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextMessage = findViewById(R.id.editTextMessage);
        Button buttonSelectContacts = findViewById(R.id.buttonSelectContacts);
        Button buttonSendInvite = findViewById(R.id.buttonSendInvite);

        // Button click listeners
        buttonSelectContacts.setOnClickListener(this::selectContacts);
        buttonSendInvite.setOnClickListener(v -> sendInvite());

        // Initialize locations list and adapter
        customAdapter = new CustomAdapter(this, listLocations.toArray(new String[0]));
        ListView listView = findViewById(R.id.list_location);
        listView.setAdapter(customAdapter);

        // Initialize location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocation();
        }
    }

    private void selectContacts(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, CONTACT_PICK_REQUEST);
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = location;
            }
        });
    }

    private void sendInvite() {
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();

        if (currentLocation != null) {
            String locationMessage = "Location: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude();
            message += "\n" + locationMessage;
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
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONTACT_PICK_REQUEST && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};

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
                sendInvite();
            } else {
                Toast.makeText(this, "Permission d'envoi de SMS refusée", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void addLocationAction(View view) {
        if (currentLocation != null) {
            String locationMessage = "Location: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude();
            listLocations.add(locationMessage);
            customAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Impossible d'obtenir la localisation actuelle", Toast.LENGTH_LONG).show();
        }
    }
}




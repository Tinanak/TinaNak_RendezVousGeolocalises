package com.example.tinanak_rendezvousgeolocalises;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 0;
    private static final int PICK_CONTACT_REQUEST = 1;
    private static final int PICK_LOCATION_REQUEST = 2;
    private static final int SMS_PERMISSION_REQUEST_CODE = 3;
    private EditText editTextPhoneNumber, editTextMessage, editTextLocation;
    private LocationManager locationManager;
    private Geocoder geocoder;
    private final ArrayList<String> listLocations = new ArrayList<>();
    private CustomAdapter customAdapter;
    private GoogleMap mMap;
    LatLng currentLatLng;

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

        // Initialize location manager et geocoder
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        geocoder = new Geocoder(this, Locale.FRANCE);

        // Vérifier si les services de localisation sont activés et demander la permission de localisation
        checkLocationEnabled();
        requestPermission(this::getLocation);

    }

    /**
     * Callback pour la carte prête à être utilisée
     *
     * @param googleMap    La carte Google
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        currentLatLng = new LatLng(getIntent().getDoubleExtra("latitude", 0), getIntent().getDoubleExtra("longitude", 0));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng));
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", latLng.latitude);
            resultIntent.putExtra("longitude", latLng.longitude);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    /**
     * Sélectionner un contact ou plusieurs contacts
     *
     * @param view     La vue actuelle
     */
    private void selectContacts(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT_REQUEST);
    }

    /**
     * Ajouter une location à la liste
     * et ouvrir la carte pour sélectionner une location sur la carte
     *
     * @param view      La vue actuelle
     */
    @SuppressLint("MissingPermission")
    private void addLocation(View view) {
        String locationName = editTextLocation.getText().toString();
        if (locationName.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un nom de localisation", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            assert addressList != null;
            String[] dataToDisplay = new String[addressList.size()];
            for (int i = 0; i < addressList.size(); i++) {
                dataToDisplay[i] = addressList.get(i).getAddressLine(0);
            }
            if (addressList.isEmpty()) {
                Toast.makeText(this, "Localisation introuvable", Toast.LENGTH_LONG).show();
                return;
            }

            ListView listView = findViewById(R.id.list_location); // Assurez-vous que l'ID du ListView est correct
            listView.setAdapter(new CustomAdapter(this, dataToDisplay));
            listView.setOnItemClickListener((parent, view1, position, id) -> {
                Address address = addressList.get(position);
                TextView textView = findViewById(R.id.detailLocation); // Assurez-vous que l'ID du TextView est correct
                textView.setText(address.toString());
            });

            Address address = addressList.get(0);
            double latitude = address.getLatitude();
            double longitude = address.getLongitude();

            // Ouvrir la carte pour sélectionner une location
            String uri = "geo:" + latitude + "," + longitude;
            Uri geoUri = Uri.parse(uri);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);

            // Vérifier si l'application de localisation est disponible
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                listLocations.add(address.getAddressLine(0));
                customAdapter.notifyDataSetChanged();
                startActivityForResult(mapIntent, PICK_LOCATION_REQUEST);
            } else {
                Toast.makeText(this, "Localisation non disponible", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors de la récupération de la localisation", Toast.LENGTH_LONG).show();
        }
    }
    /*private void addLocation(View view) throws IOException {

        // Obtenir la localisation en France
        geocoder = new Geocoder(this, Locale.FRANCE);

        // Ouvrir la carte pour sélectionner une location
        String uri = "geo:" + geocoder;
        Uri geoUri = Uri.parse(uri);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);

        // Vérifier si l'application de localisation est disponible
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            listLocations.add(geoUri.toString());
            customAdapter.notifyDataSetChanged();
            startActivityForResult(mapIntent, PICK_LOCATION_REQUEST);
        } else {
            Toast.makeText(this, "Localisation non disponible", Toast.LENGTH_LONG).show();
        }

        try {
            String location_name = ((EditText) findViewById(R.id.editTextLocation)).getText().toString();
            List<Address> listAddr = geocoder.getFromLocationName(location_name, 10);
            assert listAddr != null;
            String[] dataToDisplay = new String[listAddr.size()];
            for (int i = 0; i < listAddr.size(); i++) {
                dataToDisplay[i] = listAddr.get(i).getAddressLine(0);
            }

            ListView listView = findViewById(R.id.list_location); // Assurez-vous que l'ID du ListView est correct
            listView.setAdapter(new CustomAdapter(this, dataToDisplay));
            listView.setOnItemClickListener((parent, view1, position, id) -> {
                Address address = listAddr.get(position);
                TextView textView = findViewById(R.id.detailLocation); // Assurez-vous que l'ID du TextView est correct
                textView.setText(address.toString());
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors de la récupération de la localisation", Toast.LENGTH_LONG).show();
        }
    }*/

    /**
     * Envoyer une invitation
     *
     * @param view      La vue actuelle
     */
    private void sendInvite(View view) {
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();

        if (phoneNumber.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_LONG).show();
            return;
        }

        if (!listLocations.isEmpty()) {
            message += "\nLocalisation: " + listLocations.get(listLocations.size() - 1);
        } else if (geocoder != null) {
            String locationMessage = "Localisation: " + currentLatLng.latitude + ", " + currentLatLng.longitude;
            message += "\n" + locationMessage;
        } else {
            Toast.makeText(this, "Aucune localisation disponible", Toast.LENGTH_LONG).show();
            return;
        }
        String finalMessage = message;
        requestPermission(() -> sendSMS(phoneNumber, finalMessage));
    }

    /**
     * Envoyer un SMS
     *
     * @param phoneNumber Le numéro de téléphone
     * @param message Le message
     */
    private void sendSMS(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Toast.makeText(this, "Invitation envoyée avec succès", Toast.LENGTH_LONG).show();
        clearFields();
    }

    /**
     * Effacer les champs de texte
     */
    private void clearFields() {
        editTextMessage.setText("");
        editTextPhoneNumber.setText("");
        editTextLocation.setText("");
    }

    /**
     * Obtenir la localisation actuelle
     */
    @SuppressLint("MissingPermission")
    private void getLocation() {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Toast.makeText(MainActivity.this, "Veuillez activer les services de localisation", Toast.LENGTH_LONG).show();
            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    /**
     * Callback pour le résultat de l'activité
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     */
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
                appendPhoneNumber(number);
                cursor.close();
            }
        }

        // Selection une location sur la carte et l'ajouter à la liste
        /*if (requestCode == PICK_LOCATION_REQUEST && resultCode == RESULT_OK) {
            this.currentLatLng = new LatLng(
                    data.getDoubleExtra("latitude", 0.0),
                    data.getDoubleExtra("longitude", 0.0)
            );

            Uri locationUri = Uri.parse("geo:" + currentLatLng.latitude + "," + currentLatLng.longitude);
            String[] projection = {"location"};

            assert locationUri != null;
            Cursor cursor = getContentResolver().query(locationUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int locationIndex = cursor.getColumnIndex("location");
                String location = cursor.getString(locationIndex);

                // Ajouter la location sélectionnée à la liste
                listLocations.add(location);
                customAdapter.notifyDataSetChanged();
                cursor.close();
            }
        }*/
        if (requestCode == PICK_LOCATION_REQUEST && resultCode == RESULT_OK) {
            LatLng selectedLocation = new LatLng(
                    data.getDoubleExtra("latitude", 0.0),
                    data.getDoubleExtra("longitude", 0.0)
            );
            listLocations.add("Location: " + selectedLocation.latitude + ", " + selectedLocation.longitude);
            customAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Ajouter un numéro de téléphone à la liste
     *
     * @param number        Le numéro de téléphone
     */
    private void appendPhoneNumber(String number) {
        if (editTextPhoneNumber.getText().toString().isEmpty()) {
            editTextPhoneNumber.setText(number);
        } else if (!editTextPhoneNumber.getText().toString().contains(number)) {
            editTextPhoneNumber.append(";" + number);
        } else {
            Toast.makeText(this, "Le numéro est déjà sélectionné", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Callback pour la demande de permission
     *
     * @param requestCode The request code passed in requestPermissions(android.app.Activity, String[], int)
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendInvite(null);
            } else {
                Toast.makeText(this, "Permission d'envoi de SMS refusée", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Vérifier si les services de localisation sont activés
     */
    private void checkLocationEnabled() {
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {}
        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {}

        if (!gpsEnabled && !networkEnabled) {
            new AlertDialog.Builder(this)
                    .setMessage("Les services de localisation sont désactivés. Veuillez les activer.")
                    .setPositiveButton("Paramètres", (paramDialogInterface, paramInt) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Annuler", null)
                    .show();
        }
    }

    /**
     * Demander la permission de localisation
     */
    private void requestPermission(Runnable onGranted) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainActivity.LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            onGranted.run();
        }
    }
}

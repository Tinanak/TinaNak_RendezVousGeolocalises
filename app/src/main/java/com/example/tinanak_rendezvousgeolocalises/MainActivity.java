package com.example.tinanak_rendezvousgeolocalises;

import static com.example.tinanak_rendezvousgeolocalises.ListSmsRDV.smsResponses;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.util.Log;
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
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 0;
    private static final int PICK_CONTACT_REQUEST = 1;
    private static final int PICK_LOCATION_REQUEST = 2;
    private static final int SMS_PERMISSION_REQUEST_CODE = 3;
    private static EditText editTextPhoneNumber;
    private static EditText editTextMessage;
    private static EditText editTextLocation;
    private static TextView textViewDetailLocation;
    private LocationManager locationManager;
    private Geocoder geocoder;
    private static final ArrayList<String> listLocations = new ArrayList<>();
    private static CustomAdapter customAdapter;
    private GoogleMap mMap;
    LatLng currentLatLng;
    private static ListView listViewLocations;
    Button date, time;

    @SuppressLint({"SetTextI18n", "CutPasteId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextMessage = findViewById(R.id.editTextMessage);
        editTextLocation = findViewById(R.id.editTextLocation);
        textViewDetailLocation = findViewById(R.id.detailLocation);
        Button buttonSelectContacts = findViewById(R.id.buttonSelectContacts);
        Button buttonAddLocation = findViewById(R.id.location);
        Button buttonSendInvite = findViewById(R.id.buttonSendInvite);

        // Button click listeners
        buttonSelectContacts.setOnClickListener(this::selectContacts);
        buttonAddLocation.setOnClickListener(this::addLocation);
        buttonSendInvite.setOnClickListener(this::sendInvite);

        // Date est time
        date = findViewById(R.id.date_picker);
        time = findViewById(R.id.time_picker);

        // Initialize locations list and adapter
        customAdapter = new CustomAdapter(this, listLocations.toArray(new String[0]));
        listViewLocations = findViewById(R.id.list_location);
        listViewLocations.setAdapter(customAdapter);

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

    /**
     * Action liée à l'ouverture du time picker
     *
     * @param view button du time picker
     */
    public void showTimePickerDialog(View view) {
        DialogFragment newFragment = new PickerActivityFragment.TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    /**
     * Action liée à l'ouverture du date picker
     *
     * @param view button du date picker
     */
    public void showDatePickerDialog(View view) {
        DialogFragment newFragment = new PickerActivityFragment.DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    /**
     * Envoyer une invitation
     *
     * @param view      La vue actuelle
     */

    private void sendInvite(View view) {
        Log.d("sendInvite", "sendInvite called");

        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();

        // Vérifier si les champs sont vides
        if (phoneNumber.isEmpty()) {
            showToast("Veuillez remplir le champ du numéro de téléphone");
            Log.d("sendInvite", "phoneNumber is empty");
            return;
        }

        // Vérifier si les champs sont vides
        if (message.isEmpty()) {
            showToast("Veuillez remplir le champ du message");
            Log.d("sendInvite", "message is empty");
            return;
        }

        // Vérifier si la date et l'heure sont sélectionnées
        if (date.getText().equals("Date") || time.getText().equals("Time")) {
            showToast("Veuillez sélectionner une date et une heure");
            Log.d("sendInvite", "date or time not selected");
            return;
        }

        // Construire le message
        StringBuilder context = new StringBuilder();
        context.append("Le rendez-vous au ");

        // Vérifier si une localisation est sélectionnée et l'ajouter au message
        if (!listLocations.isEmpty()) {
            String address = listLocations.get(listLocations.size() - 1);
            context.append(address);
        } else if (geocoder != null) { // Vérifier si la localisation actuelle est disponible
            String locationMessage = "Localisation: " + currentLatLng.latitude + ", " + currentLatLng.longitude;
            context.append(locationMessage);
        } else {
            showToast("Aucune localisation disponible");
            Log.d("sendInvite", "no location available");
            return;
        }

        // Ajouter la date et l'heure au message
        context.append(" le ").append(date.getText()).append(" à ").append(time.getText());
        message += "\n" + context.toString();

        // Vérifier si l'application a la permission d'envoyer des SMS
        String finalMessage = message;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
        } else {
            sendSMS(this, phoneNumber, finalMessage);
        }
    }

    /**
     * Afficher un message toast
     *
     * @param message Le message à afficher
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Envoyer un SMS à un numéro ou plusieurs numéros
     *
     * @param phoneNumber Le numéro de téléphone
     * @param message Le message
     */
    public static void sendSMS(Context context, String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        String[] numbers = phoneNumber.split(";");
        for (String number : numbers) {
            smsManager.sendTextMessage(number, null, message, null, null);
            ListSmsRDV.addSMSResponse(context, message); // Ajouter le message à la liste des SMS envoyés
        }
        Toast.makeText(context, "Invitation envoyée", Toast.LENGTH_LONG).show();
        clearFields();
    }

    /**
     * Ouvrir la liste des SMS envoyés
     *
     * @param view
     */
    public void openSMSList(View view) {
        Intent intent = new Intent(this, ListSmsRDV.class);
        startActivity(intent);
    }

    /**
     * Effacer les champs de texte
     */
    private static void clearFields() {
        editTextMessage.setText("");
        editTextPhoneNumber.setText("");
        editTextLocation.setText("");

        // Clear locations list
        if (!listLocations.isEmpty()) {
            listLocations.clear();
            customAdapter.notifyDataSetChanged();
        }

        listViewLocations.clearChoices();
        textViewDetailLocation.setText("");
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

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendInvite(new View(this));
            } else {
                Toast.makeText(this, "Permission d'envoi de SMS refusée", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendInvite(new View(this));
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

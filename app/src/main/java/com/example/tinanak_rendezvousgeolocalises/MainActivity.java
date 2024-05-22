package com.example.tinanak_rendezvousgeolocalises;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
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
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int CONTACT_PICK_REQUEST = 1;

    private EditText editTextPhoneNumber, editTextMessage;

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
        buttonSelectContacts.setOnClickListener(v -> selectContacts());
        buttonSendInvite.setOnClickListener(v -> onRequestPermissionsResult(
                0,
                new String[]{Manifest.permission.SEND_SMS},
                new int[]{PackageManager.PERMISSION_GRANTED}));

        // Display list of locations
        ListView listView = findViewById(R.id.list_location);
        Geocoder geocoder = new Geocoder(this, Locale.FRANCE);
        try {
            String location_name = "Decathlon, Bordeaux";
            List<Address> listAddr = geocoder.getFromLocationName(location_name, 10);
            assert listAddr != null;
            String[] dataToDisplay = new String[listAddr.size()];
            for (int i = 0; i < listAddr.size(); i++) {
                dataToDisplay[i] = listAddr.get(i).getAddressLine(0);
            }
            listView.setAdapter(new CustomAdapter(this, dataToDisplay));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Envoyer une invitation à un contact sélectionné
     */
    private void selectContacts() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, CONTACT_PICK_REQUEST);
    }


    /**
     * Envoyer une invitation à un contact sélectionné
     *
     * @param requestCode       Un code d'entier qui identifie la demande de résultat
     * @param resultCode        Un code d'entier qui identifie le résultat de l'activité
     * @param data              Un objet Intent qui contient le résultat de l'activité
     *
     */
    @SuppressLint("Range")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONTACT_PICK_REQUEST && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            assert contactUri != null;
            Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                @SuppressLint("Range") String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor phoneCursor = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contactId},
                            null
                    );
                    // Remplir le champ de numéro de téléphone avec le numéro de téléphone du contact sélectionné,
                    // si la sélection est plus d'un contact, alors on doit ajouter un , entre les numéros, et les afficher dans le champ
                    // les messages seront envoyés à tous les numéros
                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        editTextPhoneNumber.append(phoneNumber);
                        Toast.makeText(this, "Contact sélectionné : " + contactName + ", Numéro : " + phoneNumber, Toast.LENGTH_SHORT).show();
                    }

                    if (phoneCursor != null) {
                        phoneCursor.close();
                    }
                }
            }

            //
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Envoyer un SMS à un numéro de téléphone avec un message
     *
     * @param numero Numéro de téléphone
     *  @param message Message à envoyer
     */
    private void sendSMS(String numero, String message) {
        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Demande de permission pour envoyer des SMS si elle n'est pas accordée
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.SEND_SMS)) {
                    Toast.makeText(this, "Permission de SMS refusée", Toast.LENGTH_LONG).show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 0);
                    Toast.makeText(this, "Veuillez accorder la permission pour envoyer des SMS", Toast.LENGTH_LONG).show();
                }
            } else {
                SmsManager.getDefault().sendTextMessage(numero, null, message, null, null);
                Toast.makeText(this, "Invitation envoyée avec succès", Toast.LENGTH_LONG).show();
                editTextMessage.setText("");
                editTextPhoneNumber.setText("");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors de l'envoi de l'invitation", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Envoyer une invitation à un contact sélectionné
     *
     * @param requestCode       Un code d'entier qui identifie la demande de résultat
     * @param permissions       Les autorisations demandées par l'application
     * @param grantResults      Les résultats des autorisations demandées
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: // Permission pour envoyer des SMS
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission accordée, envoyer le SMS
                    String phoneNumber = editTextPhoneNumber.getText().toString().trim();
                    String message = editTextMessage.getText().toString().trim();
                    if (!phoneNumber.isEmpty() && !message.isEmpty()) {
                        sendSMS(phoneNumber, message);
                    } else {
                        Toast.makeText(this, "Veuillez saisir tous les champs", Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Permission refusée
                    Toast.makeText(this, "Permission de SMS refusée", Toast.LENGTH_LONG).show();
                }
                break;
            case CONTACT_PICK_REQUEST: // Permission pour sélectionner des contacts
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectContacts();
                } else {
                    Toast.makeText(this, "Permission de lecture des contacts refusée", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}



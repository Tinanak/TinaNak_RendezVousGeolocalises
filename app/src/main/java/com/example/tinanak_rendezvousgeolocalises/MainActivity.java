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
    private Button buttonSelectContacts, buttonSendInvite;

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSelectContacts = findViewById(R.id.buttonSelectContacts);
        buttonSendInvite = findViewById(R.id.buttonSendInvite);

        // Button click listeners
        buttonSelectContacts.setOnClickListener(v -> selectContacts());
        buttonSendInvite.setOnClickListener(v -> sendInvite());

        // Display list of locations
        listView = findViewById(R.id.list_location);
        Geocoder geocoder = new Geocoder(this, Locale.FRANCE);
        try {
            String location_name = "Decathlon, Bordeaux";
            List<Address> listAddr = geocoder.getFromLocationName(location_name, 10);
            String[] dataToDisplay = new String[listAddr.size()];
            for (int i = 0; i < listAddr.size(); i++) {
                dataToDisplay[i] = listAddr.get(i).getAddressLine(0);
            }
            listView.setAdapter(new CustomAdapter(this, dataToDisplay));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void selectContacts() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, CONTACT_PICK_REQUEST);
    }

    @SuppressLint("Range")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONTACT_PICK_REQUEST && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            Cursor cursor = getContentResolver().query(contactUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor phoneCursor = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contactId},
                            null
                    );

                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        editTextPhoneNumber.setText(phoneNumber);
                        Toast.makeText(this, "Contact sélectionné : " + contactName + ", Numéro : " + phoneNumber, Toast.LENGTH_SHORT).show();
                    }

                    if (phoneCursor != null) {
                        phoneCursor.close();
                    }
                }
            }

            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void sendInvite() {
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        String message = editTextMessage.getText().toString().trim();
        if (!phoneNumber.isEmpty() && !message.isEmpty()) {
            sendSMS(phoneNumber, message);
        } else {
            Toast.makeText(this, "Veuillez saisir tous les champs", Toast.LENGTH_LONG).show();
        }
    }

    private void sendSMS(String numero, String message) {
        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Demande de permission
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.SEND_SMS)) {
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 0);
                }
            } else {
                SmsManager.getDefault().sendTextMessage(numero, null, message, null, null);
                Toast.makeText(this, "Invitation envoyée avec succès", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lors de l'envoi de l'invitation", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
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
        }
    }
}



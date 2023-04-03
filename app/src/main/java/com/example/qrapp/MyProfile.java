package com.example.qrapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * The MyProfile class is the activity that is used to display the user's profile
 *  and their stats, when called, it will get the user's UID from auth, then
 *  get the user's data from the database, then get the QRCodes they have scanned
 *  and display the stats & information based on that data.
 *  It also has a button to view the highest and lowest QRCodes scanned by the user, which
 *  will open a new activity "QRProfile", passing the QRCode object to it.
 *  it displays the user's username, email, highest and lowest QRCodes scanned, total score,
 *  and total number of QRCodes scanned. It has buttons to view the highest and lowest QRCodes, and
 *  a back button to return to the previous activity.
 */
public class MyProfile extends AppCompatActivity {


    private TextView usernameText;
    private TextView emailText;
    private TextView highestQRCvalue;
    private TextView lowestQRCvalue;
    private TextView totalscoreValue;
    private TextView codesScannedValue;

    private ImageButton backButton;
    private ImageButton viewHighestQRCButton;
    private ImageButton viewLowestQRCButton;

    private Button viewScansButton;

    private String userID;

    private String username;

    private String email;

    private TextView rankingField;

    private ArrayList<QRCode> QRCodeList;

    private FirebaseFirestore db;
    //TODO Stats section

    /**
     * the onCreate method finds all the views and sets the onclick listeners,
     * as well as establishing the database connection, then calls updateUserInfo()
     * and getQRCodes() in order to get the user data and the QRCodes they have scanned
     *
     * @param savedInstanceState the saved instance state of the activity, not used except to call the super method
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_user);

        //get the views
        usernameText = findViewById(R.id.username);
        emailText = findViewById(R.id.email);
        backButton = findViewById(R.id.back);
        highestQRCvalue = findViewById(R.id.highestQRCvalue);
        lowestQRCvalue = findViewById(R.id.lowestQRCvalue);
        totalscoreValue = findViewById(R.id.totalscoreValue);
        codesScannedValue = findViewById(R.id.codesScannedValue);
        viewHighestQRCButton = findViewById(R.id.viewHighestQRCButton);
        viewLowestQRCButton = findViewById(R.id.viewLowestQRCButton);
        viewScansButton = findViewById(R.id.myQRCbutton);
        rankingField = findViewById(R.id.rankingValue);

        db = FirebaseFirestore.getInstance();

        //check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //set the buttons related to highest & lowest QR codes to invisible and disabled until the data is loaded
        viewHighestQRCButton.setEnabled(false);
        viewHighestQRCButton.setVisibility(View.INVISIBLE);
        viewLowestQRCButton.setEnabled(false);
        viewLowestQRCButton.setVisibility(View.INVISIBLE);

        //set the view scans button to open the ViewPlayerScannedQR with currentUser true
        viewScansButton.setOnClickListener(v -> {
            Intent intent = new Intent(MyProfile.this, ViewPlayerScannedQRActivity.class);
            intent.putExtra("isCurrentUser", true);
            intent.putExtra("QRCodeList", QRCodeList);
            startActivity(intent);
        });


        // set the view scans button to disabled until the data is loaded
        viewScansButton.setEnabled(false);


        // get the user data from the database
        updateUserInfo();

        //initialize the QRCodeList, this will be populated in the OnResume step of the lifecycle
        QRCodeList = new ArrayList<>();


        //close activity when back button is pressed
        backButton.setOnClickListener(v -> finish());


    }

    /**
     * onResume is called whenever the user opens or returns to this activity
     * it calls getQRCodes() to update the QRCodeList in case the user updated their qr codes
     */
    @Override
    protected void onResume() {
        super.onResume();
        getQRCodes();
    }


    /**
     * Gets all the QRCodes that the user has scanned,
     * writes them to the QRCodeList
     * then calls updateScores() to update the stats
     */
    private void getQRCodes() {
        //reset everything before loading data
        QRCodeList.clear();
        String loading = getString(R.string.loading);
        highestQRCvalue.setText(loading);
        lowestQRCvalue.setText(loading);
        totalscoreValue.setText(loading);
        codesScannedValue.setText(loading);

        //get new data
        db.collection("QRCodes").whereArrayContains("playersScanned", userID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    Integer points = document.getLong("Points").intValue();
                    String name = document.getString("Name");
                    String icon = document.getString("icon");
                    Object playersScanned = document.get("playersScanned");
                    Object comments = document.get("Comments");
                    GeoPoint geolocation = document.getGeoPoint("Geolocation");
                    String hashed = document.getString("Hash");

                    QRCode queriedQR = new QRCode(comments, points, name, icon, playersScanned, geolocation, hashed);
                    QRCodeList.add(queriedQR);
                }


                updateScores(userID);

                //enable the view scans button
                viewScansButton.setEnabled(true);


            } else {
                Toast.makeText(MyProfile.this, "Error getting user data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        });

    }

    /**
     * UpdateScores finds the highest, lowest and total scores of the user using the QRCodeList,
     * sets the text views to their corresponding values,
     * sets if the view highest and lowest buttons are enabled or not,
     * sets the buttons to open the QRProfile activity with the highest and lowest QRCode respectively
     */
    private void updateScores(String deviceID) {

        if (QRCodeList.size() == 0) {
            //if the user has not scanned any QR codes, set the text views to null representing values
            highestQRCvalue.setText("N/A");
            lowestQRCvalue.setText("N/A");
            totalscoreValue.setText("0");
            codesScannedValue.setText("0");

            return;
        }

        //find the highest, lowest QR code and total scores
        QRCode highestQR = QRCodeList.get(0);
        QRCode lowestQR = QRCodeList.get(0);
        int total = 0;
        for (QRCode qrCode : QRCodeList) {
            int currentScore = Integer.parseInt(qrCode.getPoints());
            int highestScore = Integer.parseInt(highestQR.getPoints());
            int lowestScore = Integer.parseInt(lowestQR.getPoints());
            if (currentScore > highestScore) {
                highestQR = qrCode;
            }
            if (currentScore < lowestScore) {
                lowestQR = qrCode;
            }
            total += currentScore;
        }
        //set the text views
        highestQRCvalue.setText(String.valueOf(highestQR.getPoints()));
        lowestQRCvalue.setText(String.valueOf(lowestQR.getPoints()));
        totalscoreValue.setText(String.valueOf(total));
        codesScannedValue.setText(String.valueOf(QRCodeList.size()));
        calculateRanking(db, deviceID, rankingField);


        // enable the buttons
        viewHighestQRCButton.setEnabled(true);
        viewHighestQRCButton.setVisibility(View.VISIBLE);
        viewLowestQRCButton.setEnabled(true);
        viewLowestQRCButton.setVisibility(View.VISIBLE);

        //set the buttons to open the QRProfile activity
        final QRCode finalLowestQR = lowestQR;
        final QRCode finalHighestQR = highestQR;
        viewHighestQRCButton.setOnClickListener(v -> {
            Intent intent = new Intent(MyProfile.this, QRProfile.class);
            intent.putExtra("qr_code", finalHighestQR);
            startActivity(intent);
        });
        viewLowestQRCButton.setOnClickListener(v -> {
            Intent intent = new Intent(MyProfile.this, QRProfile.class);
            intent.putExtra("qr_code", finalLowestQR);
            startActivity(intent);
        });
    }

    /**
     * UpdateUserInfo Gets the username and email of the user from the database,
     * and sets the text views to their corresponding values
     */
    private void updateUserInfo() {
        db.collection("Users").document(userID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    //set the attributes
                    username = document.getString("username");
                    email = document.getString("email");

                    //set the text views
                    usernameText.setText(username);
                    emailText.setText(email);
                } else {
                    Toast.makeText(MyProfile.this, "Error getting user data", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                Toast.makeText(MyProfile.this, "Error getting user data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        });
    }

    /**
     * calculates the player's ranking for highest unique QR code
     *
     * @param db   passed instance of firebase database
     * @param deID the deviceId to calculate the ranking of
     * @param rnk  the textView to change once rank has been calculated
     */
    public void calculateRanking(FirebaseFirestore db, String deID, TextView rnk) {
        // As a player, I want an estimate of my ranking for the highest scoring unique QR code
        // 1. go through all players, get their highest QR code that only they scanned, store it in a dict
        // query all player's deviceID, add them to a dict with highest = 0
        Hashtable<String, Integer> deviceIdDict = new Hashtable<String, Integer>();
        db.collection("Users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                for (DocumentSnapshot iterativeDocument : documents) {
                    String deviceId = iterativeDocument.getId();
                    deviceIdDict.put(deviceId, 0);
                }
                //2. Go through all playersScanned fields, find highest QR code that only they scanned
                //Set<String> setofKeys = deviceIdDict.keySet();
                //for (String key : setofKeys) {
                db.collection("QRCodes").get().addOnCompleteListener(newTask -> {
                    if (newTask.isSuccessful()) {
                        try {
                            DocumentSnapshot test = newTask.getResult().getDocuments().get(0);
                        } catch (IndexOutOfBoundsException e) {
                            Log.d("myTag", "Nothing scanned");
                        }
                        // loop through QR codes where playersScanned contains DeviceID
                        // check if ONLY they scanned it
                        List<DocumentSnapshot> newTaskDocuments = newTask.getResult().getDocuments();
                        for (DocumentSnapshot newDocument : newTaskDocuments) {
                            ArrayList<String> pS = new ArrayList<>();
                            pS = (ArrayList<String>) newDocument.get("playersScanned");
                            int size = 0;
                            try {
                                size = pS.size();
                            } catch (Exception e) {
                                Log.d("myTag", "Nothing Scanned");
                            }
                            String stringSize = Integer.toString(size);
                            if (stringSize.equals("1")) { // now we know that the only player who scanned it is the 'key'
                                String playerID = pS.get(0);
                                long points = (long) newDocument.get("Points");
                                int highestUnique = deviceIdDict.get(playerID);
                                if (points > highestUnique) {
                                    highestUnique = (int) points;
                                    deviceIdDict.put(playerID, highestUnique);
                                }
                            }
                        }
                        // we have a dict of highest uniques now (confirmed working), just need to sort it so (highest = index 0)
                        // yeah I know this is ugly I used a bad datatype
                        ArrayList<Integer> highestArrayList = new ArrayList<>();
                        Collection<Integer> highestSet = deviceIdDict.values();
                        highestArrayList.addAll(highestSet);
                        Collections.sort(highestArrayList, Collections.reverseOrder());
                        int playerScore = deviceIdDict.get(deID);
                        String pS = Integer.toString(playerScore);
                        int index = 1;
                        for (int score : highestArrayList) {
                            String s_score = Integer.toString(score);
                            if (s_score.equals(pS)) {
                                Log.d("myTag", "SCORES" + s_score + pS);
                                String stringRank = Integer.toString(index);
                                rnk.setText(stringRank);
                                break;
                            } else {
                                index += 1;
                            }
                        }
                    }
                });
                //}
            }
        }); // first query


    }
}

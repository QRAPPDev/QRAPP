package com.example.qrapp;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is a class that extends the Fragment class. This "RankFragment" class contains and
 * maintains the data that will be displayed on the leaderboard(s)
 */
public class RankFragment extends Fragment {


    Spinner RANK_SPINNER; //select 1 of 4 leaderboards
    ArrayAdapter<CharSequence> RankSpinnerAdapter;

    //Database
    FirebaseAuth Auth;
    FirebaseFirestore DB;
    CollectionReference UserCR, QRCodeCR; //reference to the players and the qrcodes
    String userID;

    //dataholders, temporary storage of some data
    //List<String> players;
    String my_region;
    int null_users = 0;

    //tops
    int X = 10; // will do top 10
    ArrayList<RankPair> Sum_Or_Count; //holds data for all players for sum and count ranking
    ArrayList<RankTriple> Score_Or_Local; //holds data for all players for score and local ranking

    ArrayList<RankPair> topPairs; //these will hold the true top 10
    ArrayList<RankTriple> topTriples;

    //passing data
    String RankBundleKey = "RB"; //use the same key to get the data passed


    /**
     * The onCreate method sets most of the attributes required to maintain ranking data
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Auth = FirebaseAuth.getInstance();
        DB = FirebaseFirestore.getInstance();
        UserCR = DB.collection("Users");
        QRCodeCR = DB.collection("QRCodes");
        userID = Auth.getCurrentUser().getUid();
        setRegion();


        //players = new ArrayList<>();



    }

    /**
     * This method sets the region of the current user to use for local rankings
     */
    private void setRegion() {
        UserCR.document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                my_region = task.getResult().getString("location");
            }
        });
    }

    /**The onCreateView method sets the functionality for critical view parameters
     *  for the RankFragment such as the display and interface of the Spinner
     *  and its adapter. It returns the view
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return View
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);



        View view = inflater.inflate(R.layout.fragment_rank, container, false);
//        for (String playrID:players) {
//            Log.d("RANK",playrID + " is a player");
//        }


        RANK_SPINNER = (Spinner) view.findViewById(R.id.spinnerRank);

        //SET SPINNER
        // Create an ArrayAdapter using the string array and a default spinner layout
        RankSpinnerAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.rank_criteria, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        RankSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        RANK_SPINNER.setAdapter(RankSpinnerAdapter);


        RANK_SPINNER.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                //will use kth largest instead of sorting to get ranks
                switch (position){
                    case 0: //Score
                        Log.d("RANK", "GLOBAL RANK(SCORE)");

                        //order top 20 by Highest QRcodes in whole app
                        //1. get the highest QRCodes of all players
                        Score_Or_Local = new ArrayList<>();
                        topTriples = new ArrayList<>();


                        UserCR.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                //String userID;
                                if(task.isSuccessful()){
                                    int P = task.getResult().size(); //used to make querying quicker

                                    Log.d("RANK2",P + "docs");
                                    for (QueryDocumentSnapshot userDoc: task.getResult()) {//GO OVER USERS
                                        Score_Or_Local.clear(); //remove whatever is there
                                        userID = userDoc.getId();

                                        QRCodeCR.whereArrayContains("playersScanned", userID).get()

                                                // STARTS TO BE DIFFERENT
                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                        int QRcPoints;
                                                        int highest = 0;
                                                        String highestFace = "----";
                                                        if (task.isSuccessful()) {
                                                            for (QueryDocumentSnapshot qrcDoc : task.getResult()) {//GO OVER USER'S CODES, GET HIGHEST
                                                                QRcPoints = qrcDoc.getLong("Points").intValue();
                                                                if (highest <= QRcPoints) {
                                                                    highest = QRcPoints;
                                                                    highestFace = (String) qrcDoc.get("icon");
                                                                }
                                                            }
                                                        } else {
                                                            Log.d("RANK2", "Failed to get QRCodes");
                                                        }


                                                        //ADD USER HIGHEST CODE TO LIST, also track the null users so can know when to get top 10
                                                        if(userDoc.getString("username")!=null) {
                                                            Score_Or_Local.add(new RankTriple(userDoc.getString("username"), highestFace, highest));
                                                        }else{
                                                            null_users++;
                                                        }
                                                        //Log.d("RANK", userDoc.getString("username") + " " + highest);
                                                        int N_Players = Score_Or_Local.size();
                                                        Log.d("RANK2",String.valueOf(N_Players));


                                                        //ONLY GET TOP 10 WHEN ALL PLayers's highest is gotten
                                                        if(N_Players+null_users==P){

                                                            //2)
                                                            //top 10
                                                            for(int i=1; i<=X && i<=N_Players; i++){
                                                                topTriples.add(kthLargestTriple(Score_Or_Local,i));
                                                                Log.d("RANK2", topTriples.get(i-1).PlayerID + " " + topTriples.get(i-1).QRcPoints);
                                                            }

                                                            //3)
                                                            //Bundle em up
                                                            Bundle RankBundle = new Bundle();
                                                            RankBundle.putSerializable(RankBundleKey, topTriples);

                                                            //4)
                                                            //make new RankScoreFragment with data
                                                            Fragment selected = new RankScoreFragment();
                                                            selected.setArguments(RankBundle);

                                                            //5)
                                                            //show it
                                                            getActivity().getSupportFragmentManager()
                                                                    .beginTransaction()
                                                                    .replace(R.id.rankframe, selected).commit();//SHOW FRAGMENT
                                                        }


                                                    }
                                                });


                                    }

                                }else{
                                    Log.d("RANK","Failed to get Users");
                                }

                            }

                            //int N_Players = Score_Or_Local.size();

                        });
                        //mapped each player to their highest qrc




                        null_users=0; //reset nulls
                        break;
                    case 1: //Sum
                        Log.d("RANK", "GLOBAL RANK(SUM)");

                        //order top 10 by Sum of QRCodes
                        //1. get the highest QRCodes of all players
                        Sum_Or_Count = new ArrayList<>();
                        topPairs = new ArrayList<>();


                        UserCR.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                               @Override
                               public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                   //String userID;
                                   if (task.isSuccessful()) {
                                       int P = task.getResult().size(); //used to make querying quicker

                                       Log.d("RANK3", P + "docs");
                                       for (QueryDocumentSnapshot userDoc : task.getResult()) {//GO OVER USERS
                                           Sum_Or_Count.clear(); //remove whatever is there
                                           userID = userDoc.getId();

                                           QRCodeCR.whereArrayContains("playersScanned", userID).get()

                                                   .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                                                       @Override
                                                       public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                           int QRCSum = 0;
                                                           if (task.isSuccessful()) {
                                                               for (QueryDocumentSnapshot qrcDoc : task.getResult()) {//GO OVER USER'S CODES, GET SUM OF SCANS
                                                                   QRCSum += qrcDoc.getLong("Points").intValue();
                                                               }
                                                           } else {
                                                               Log.d("RANK3", "Failed to get QRCodes");
                                                           }

                                                           //ADD USER SCORE SUM TO LIST, also track the null users so can know when to get top 10
                                                           if (userDoc.getString("username") != null) {
                                                               Sum_Or_Count.add(new RankPair(userDoc.getString("username"), QRCSum));
                                                           } else {
                                                               null_users++;
                                                           }

                                                           //Log.d("RANK", userDoc.getString("username") + " " + highest);
                                                           int N_Players = Sum_Or_Count.size();
                                                           Log.d("RANK3", String.valueOf(N_Players));


                                                           //ONLY GET TOP 10 WHEN ALL PLayers's highest is gotten
                                                           if (N_Players + null_users == P) {

                                                               //2)
                                                               //top 10
                                                               for (int i = 1; i <= X && i <= N_Players; i++){
                                                                     topPairs.add(kthLargestPair(Sum_Or_Count, i));
                                                                   Log.d("RANK3", topPairs.get(i - 1).PlayerID + " " + topPairs.get(i - 1).Number);
                                                               }

                                                               //3)
                                                               //Bundle em up
                                                               Bundle RankBundle = new Bundle();
                                                               RankBundle.putSerializable(RankBundleKey, topPairs);

                                                               //4)
                                                               //make new RankSumFragment with data
                                                               Fragment selected = new RankSumFragment();
                                                               selected.setArguments(RankBundle);

                                                               //5)
                                                               //show it
                                                               getActivity().getSupportFragmentManager()
                                                                       .beginTransaction()
                                                                       .replace(R.id.rankframe, selected).commit();//SHOW FRAGMENT
                                                           }


                                                       }
                                                   });
                                       }
                                   } else {
                                       Log.d("RANK", "Failed to get Users");
                                   }
                               }
                           });


                        null_users=0; //reset nulls
                        break;
                    case 2: //Count
                        Log.d("RANK", "GLOBAL RANK(COUNT)");

                        //order top 10 by Count of QRCodes
                        //1. get the highest QRCodes of all players
                        Sum_Or_Count = new ArrayList<>();
                        topPairs = new ArrayList<>();


                        UserCR.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                //String userID;
                                if (task.isSuccessful()) {
                                    int P = task.getResult().size(); //used to make querying quicker

                                    Log.d("RANK4", P + "docs");
                                    for (QueryDocumentSnapshot userDoc : task.getResult()) {//GO OVER USERS
                                        Sum_Or_Count.clear(); //remove whatever is there
                                        userID = userDoc.getId();

                                        QRCodeCR.whereArrayContains("playersScanned", userID).get()

                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                                                    @Override
                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                        int QRCCount = 0;
                                                        if (task.isSuccessful()) {
                                                            for (QueryDocumentSnapshot qrcDoc : task.getResult()) {//GO OVER USER'S CODES, GET COUNT OF SCANS
                                                                if (qrcDoc.getLong("Points").intValue() >= 0) {
                                                                    QRCCount++;
                                                                }
                                                            }
                                                        } else {
                                                            Log.d("RANK4", "Failed to get QRCodes");
                                                        }

                                                        //ADD USER CODE COUNT TO LIST, also track the null users so can know when to get top 10
                                                        if (userDoc.getString("username") != null) {
                                                            Sum_Or_Count.add(new RankPair(userDoc.getString("username"), QRCCount));
                                                        } else {
                                                            null_users++;
                                                        }

                                                        //Log.d("RANK", userDoc.getString("username") + " " + highest);
                                                        int N_Players = Sum_Or_Count.size();
                                                        Log.d("RANK4", String.valueOf(N_Players));


                                                        //ONLY GET TOP 10 WHEN ALL PLayers's highest is gotten
                                                        if (N_Players + null_users == P) {

                                                            //2)
                                                            //top 10
                                                            for (int i = 1; i <= X && i <= N_Players; i++){
                                                                topPairs.add(kthLargestPair(Sum_Or_Count, i));
                                                                Log.d("RANK4", topPairs.get(i - 1).PlayerID + " " + topPairs.get(i - 1).Number);
                                                            }

                                                            //3)
                                                            //Bundle em up
                                                            Bundle RankBundle = new Bundle();
                                                            RankBundle.putSerializable(RankBundleKey, topPairs);

                                                            //4)
                                                            //make new RankCountFragment with data
                                                            Fragment selected = new RankCountFragment();
                                                            selected.setArguments(RankBundle);

                                                            //5)
                                                            //show it
                                                            getActivity().getSupportFragmentManager()
                                                                    .beginTransaction()
                                                                    .replace(R.id.rankframe, selected).commit();//SHOW FRAGMENT
                                                        }


                                                    }
                                                });
                                    }
                                } else {
                                    Log.d("RANK", "Failed to get Users");
                                }
                            }
                        });


                        null_users=0; //reset nulls
                    case 3: //Local
                        Log.d("RANK", "LOCAL RANK(SCORE)");

                        //order top 10 by Highest QRCodes locally(ANA of Postal code)
                        //1. get the highest QRCodes of all players
                        Score_Or_Local = new ArrayList<>();
                        topTriples = new ArrayList<>();


                        //get all players with the same region as me
                        UserCR.whereEqualTo("location",my_region).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                String userID;
                                if(task.isSuccessful()){
                                    int P = task.getResult().size(); //used to make querying quicker

                                    Log.d("RANK5",P + "docs");
                                    for (QueryDocumentSnapshot userDoc: task.getResult()) {//GO OVER USERS
                                        Score_Or_Local.clear();
                                        userID = userDoc.getId();

                                        QRCodeCR.whereArrayContains("playersScanned", userID).get()

                                                // STARTS TO BE DIFFERENT
                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                        int QRcPoints;
                                                        int highest = 0;
                                                        String highestFace = "----";
                                                        if (task.isSuccessful()) {
                                                            for (QueryDocumentSnapshot qrcDoc : task.getResult()) {//GO OVER USER'S CODES, GET THEIR HIGHEST
                                                                QRcPoints = qrcDoc.getLong("Points").intValue();
                                                                if (highest <= QRcPoints) {
                                                                    highest = QRcPoints;
                                                                    highestFace = (String) qrcDoc.get("icon");
                                                                }
                                                            }
                                                        } else {
                                                            Log.d("RANK5", "Failed to get QRCodes");
                                                        }


                                                        //IF THEY ARE NOT A NULL USER, ADD THEM TO THE LIST, OTHERWISE TRACK NULLs to know when we can get top 10
                                                        if(userDoc.getString("username")!=null) {
                                                            Score_Or_Local.add(new RankTriple(userDoc.getString("username"), highestFace, highest));
                                                        }else{
                                                            null_users++;
                                                        }

                                                        //Log.d("RANK", userDoc.getString("username") + " " + highest);
                                                        int N_Players = Score_Or_Local.size();
                                                        Log.d("RANK5",String.valueOf(N_Players));



                                                        //GET TOP 10 ONLY WHEN GONE OVER ALL PLAYERS
                                                        if(N_Players+null_users==P){

                                                            //2) top X=10
                                                            for(int i=1; i<=X && i<=N_Players; i++){
                                                                topTriples.add(kthLargestTriple(Score_Or_Local,i));
                                                                Log.d("RANK5", topTriples.get(i-1).PlayerID + " " + topTriples.get(i-1).QRcPoints);

                                                            }

                                                            //3)
                                                            //Bundle em up
                                                            Bundle RankBundle = new Bundle();
                                                            RankBundle.putSerializable(RankBundleKey, topTriples);

                                                            //4)
                                                            //make new RankLocalFragment with data
                                                            Fragment selected = new RankLocalFragment();
                                                            selected.setArguments(RankBundle);

                                                            //5)
                                                            //show it
                                                            getActivity().getSupportFragmentManager()
                                                                    .beginTransaction()
                                                                    .replace(R.id.rankframe, selected).commit();//SHOW FRAGMENT
                                                        }


                                                    }
                                                });


                                    }

                                }else{
                                    Log.d("RANK5","Failed to get Users");
                                }
                            }

                        });


                        null_users=0;//reset nulls
                        break;
                }



            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return view;
    }







    // to find the kth largest elem in an Arralist of Player hash string to QRCode pairs
    //eg kthLargest(player_pairs, 1) returns the Player hash string and QRCode pair with
    // the highest QRCode score in player_pairs

    /**
     * This function returns the kth largest RankTriple in an arraylist of ranktriples
     * @param arr
     * @param k
     * @return RankTriple
     */
    public RankTriple kthLargestTriple(ArrayList<RankTriple> arr, int k) {
        //O(n)linear time + works for unsorted ordered containers
        int left = 0;
        int right = arr.size() - 1;

        while (left <= right) {
            int pivotIndex = partitionTriple(arr, left, right);

            if (pivotIndex == k - 1) {
                return arr.get(pivotIndex);
            } else if (pivotIndex < k - 1) {
                left = pivotIndex + 1;
            } else {
                right = pivotIndex - 1;
            }
        }
        return null; // kth largest not found, default to null
    }



    //helper funct for kthLargest. Partitioning based on Quicksort
    /**
     * This function is a helper function that partitions
     * a given array to get the top X players for RankTriples
     * @param arr
     * @param left
     * @param right
     * @return int
     */
    private static int partitionTriple(ArrayList<RankTriple> arr, int left, int right) {
        RankTriple pivot = arr.get(right);
        int i = left - 1;

        for (int j = left; j < right; j++) {
            if (arr.get(j).QRcPoints >= pivot.QRcPoints) {
                i++;
                Collections.swap(arr, i, j);
            }
        }

        Collections.swap(arr, i + 1, right);

        return i + 1;
    }


    /**
     * This function returns the kth largest RankPair in an arraylist of rankpairs
     * @param arr
     * @param k
     * @return RankPair
     */
    public RankPair kthLargestPair(ArrayList<RankPair> arr, int k) {
        //O(n)linear time + works for unsorted ordered containers
        int left = 0;
        int right = arr.size() - 1;

        while (left <= right) {
            int pivotIndex = pairPartition(arr, left, right);

            if (pivotIndex == k - 1) {
                return arr.get(pivotIndex);
            } else if (pivotIndex < k - 1) {
                left = pivotIndex + 1;
            } else {
                right = pivotIndex - 1;
            }
        }
        return null; // kth largest not found, default to null
    }


    //helper funct for kthLargest. Partitioning based on Quicksort
    /**
     * This function is a helper function that partitions
     * a given array to get the top X players for RankPairs
     * @param arr
     * @param left
     * @param right
     * @return int
     */
    private static int pairPartition(ArrayList<RankPair> arr, int left, int right) {
        RankPair pivot = arr.get(right);
        int i = left - 1;

        for (int j = left; j < right; j++) {
            if (arr.get(j).Number >= pivot.Number) {
                i++;
                Collections.swap(arr, i, j);
            }
        }

        Collections.swap(arr, i + 1, right);

        return i + 1;
    }
}

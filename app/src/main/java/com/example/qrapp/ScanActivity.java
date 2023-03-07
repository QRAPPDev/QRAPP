package com.example.qrapp;

import static java.lang.Math.pow;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;

//Scanner X - cam
//ML Kit
public class ScanActivity extends AppCompatActivity {

    //QRc Personality
    TextView QRcView;
    //to get hash
    private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);
    HashMap<Character, Integer> converter;

    //to convert to name
    HashMap<Character, String> Title, Descriptor, A, B, C, D, E;

    //to convert to face
    char nline = '\n';
    String right_roof = " /",
            centre_roof = "| ",
            left_roof = " \\";
    HashMap<Character, Character> RoofnFloor, Eyes, Nose, Mouth;
    HashMap<Character, String> Walls, Hat, FaceHair;
    //QRc Personality

    //BUTTONS
    ImageButton BACK_ARROW;
    FloatingActionButton PHOTO;
    //BUTTONS

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        //QRc Personality
        /*Overview We want each QRc to have a unique name and face . The hash function create a unique hex for each QRc content
        We pass the QRc content into a hashfunction to generate a unique hash
        Unique hash used to generate unique name and face
        -> Convert hex to oct
        ->use dictionaries to map particular values to strings that form the name and face respectively
        ->7 choices for the first 7 values of the oct, 8 options at each choice for both name and face

        The score of the QRc is also calculated
         */

        //QRcView = findViewById(R.id.textview_score); // show it in the Listview

        String content = "QRAPP";
        String hash_function = "SHA-256";

        //convert values for chars to their respective numeric: '0'==0
        int max = 20; //for 0 since 0 is actually 20 in this scoring sys
        converter = new HashMap<>();
        converter.put('O', max);
        for (int c = '1'; c <= '9'; c++)
            converter.put((char) c, c - '0');
        for (int c = 'a'; c <= 'f'; c++)
            converter.put((char) c, c - 'a' + 10);

        //NAMING
        //>2,000,000: (8^7) unique names
        //~1% chance: (1/2^k, k=7) that 2 distinct QRc have the same name
        Title = new HashMap<>();
        setTitles(Title);

        Descriptor = new HashMap<>();
        setDescriptors(Descriptor);

        A = new HashMap<>();
        setFirstName(A);

        B = new HashMap<>();
        setSecondName(B);

        C = new HashMap<>();
        setThirdName(C);

        D = new HashMap<>();
        setFourthName(D);

        E = new HashMap<>();
        setLastName(E);


        //DRAWING
        //>2,000,000: (8^7) unique images
        //~1% chance: (1/2^k, k=7) that 2 distinct QRc have the same image
        RoofnFloor = new HashMap<>();
        setRnF(RoofnFloor);

        Hat = new HashMap<>();
        setHat(Hat);

        Walls = new HashMap<>();
        setWalls(Walls);

        Eyes = new HashMap<>();
        setEyes(Eyes);

        Nose = new HashMap<>();
        setNose(Nose);

        Mouth = new HashMap<>();
        setMouth(Mouth);

        FaceHair = new HashMap<>();
        setFaceHair(FaceHair);


        //GETTING THE HASH OF A QRc, doesnt always work so need the try accept block.
        // will potentially crash open for randomly problematic codes
        try {
            MessageDigest MD = MessageDigest.getInstance(hash_function);
            byte[] QRC_hash = MD.digest(content.getBytes(StandardCharsets.UTF_8));

            String hash_out = bytesToHex(QRC_hash);

            int octal = 8;
            String hash_8= change_of_base(hash_out,octal);

            String QRcName = nameQRc(hash_8);
            String QRcImage = imageQRc(hash_8);


            //LOG INFO for debug/testing
            /*
            Log.d("HEX TO OCT RESULT", hash_8);
            Log.d("GOOD TRANSLATION", hash_out);
            Log.d("IMAGE", String.format(Locale.CANADA, "%c%s",
                    nline, QRcImage));
            Log.d("MY NAME IS  ... ", QRcName);
            for (byte b : QRC_hash) {
                //Indiv bytes in log
                Log.d("TAG", String.format("%02x", b) + " " + String.format("%o", b));
            }
            Log.d("TESTING ECLASS EXAMPLE", String.valueOf(score("bb999ee55")));

            */
            //



        } catch (NoSuchAlgorithmException e) {//throws, app crashes and shows cause in log
            e.printStackTrace();
        }
        //QRc Personality



        BACK_ARROW = findViewById(R.id.button_back_scan);
        PHOTO = findViewById(R.id.button_take_photo);

        //go back to main activity
        BACK_ARROW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //take a photo(not scan)
        PHOTO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //take photo if allowed
            }
        });


    }







    //QRc Personality
    //IMAGE of a QRc "calculation"
    private String imageQRc(String hash_8) {

        //first line
        String RRoof = hash_8.charAt(0) - '0' == 0 ? "  " : right_roof;
        Character RCeilnFloor = RoofnFloor.get(hash_8.charAt(0));
        String RWall = Walls.get(hash_8.charAt(1));
        String first = String.format(Locale.CANADA, "%s%c%s%c",
                RRoof,
                RCeilnFloor,
                RWall,
                RCeilnFloor);


        //second/middle/face line
        String CRoof = hash_8.charAt(0) - '0' == 0 ? "  " : centre_roof;
        //char CCeilnFloor = RCeilnFloor;
        String face = faceQRc(hash_8);
        String second = String.format(Locale.CANADA, "%s%c%s%c",
                CRoof,
                RCeilnFloor,
                face,
                RCeilnFloor);

        //third/last line
        String LRoof = hash_8.charAt(0) - '0' == 0 ? "  " : left_roof;
        //char LCeilnFloor = RCeilnFloor;
        //String LWall = RWall;
        String third = String.format(Locale.CANADA, "%s%c%s%c",
                LRoof,
                RCeilnFloor,
                RWall,
                RCeilnFloor
        );

        return String.format(Locale.CANADA, "%s%c%s%c%s%c",
                first,
                nline,
                second,
                nline,
                third,
                nline);
    }

    //FACE of a QRc "calculation"
    private String faceQRc(String hash_8) {
        //chars of index 2 to 6 handle face. 0 and 1 for Frame/House
        return String.format(Locale.CANADA, "%c%s%c%c%c%s%c",
                ' ',
                Hat.get(hash_8.charAt(2)),
                Eyes.get(hash_8.charAt(3)),
                Nose.get(hash_8.charAt(4)),
                Mouth.get(hash_8.charAt(5)),
                FaceHair.get(hash_8.charAt(6)),
                ' ');
    }


    //All possible FaceHair in IMAGING dict
    private void setFaceHair(HashMap<Character, String> FaceHair) {
        FaceHair.put('0', "  ");
        FaceHair.put('1', "- ");
        FaceHair.put('2', "= ");
        FaceHair.put('3', "~ ");
        FaceHair.put('4', "~~");
        FaceHair.put('5', "-~");
        FaceHair.put('6', "=-");
        FaceHair.put('7', ". ");
    }

    //All possible Mouths in IMAGING dict
    private void setMouth(HashMap<Character, Character> Mouth) {
        Mouth.put('0', 'O');
        Mouth.put('1', '1');
        Mouth.put('2', 'D');
        Mouth.put('3', '3');
        Mouth.put('4', ')');
        Mouth.put('5', '\\');
        Mouth.put('6', '#');
        Mouth.put('7', '(');
    }

    //All possible Noses in IMAGING dict
    private void setNose(HashMap<Character, Character> Nose) {
        Nose.put('0', '*');
        Nose.put('1', '<');
        Nose.put('2', '>');
        Nose.put('3', '\'');
        Nose.put('4', '4');
        Nose.put('5', '\"');
        Nose.put('6', '^');
        Nose.put('7', 'c');
    }

    //All possible Eye sets in IMAGING dict
    private void setEyes(HashMap<Character, Character> Eyes) {
        Eyes.put('0', '0');
        Eyes.put('1', '=');
        Eyes.put('2', 'B');
        Eyes.put('3', '8');
        Eyes.put('4', 'X');
        Eyes.put('5', ':');
        Eyes.put('6', ';');
        Eyes.put('7', 'K');
    }

    //All possible Walls in IMAGING dict
    private void setWalls(HashMap<Character, String> Walls) {
        Walls.put('0', "         ");
        Walls.put('1', "---------");
        Walls.put('2', "=========");
        Walls.put('3', "-o-o-o-o-");
        Walls.put('4', "x~o~x~o~x");
        Walls.put('5', "x-o-x-o-x");
        Walls.put('6', "~o~o~o~o~");
        Walls.put('7', "<>~<>~<>~");
    }

    //All possible Hats in IMAGING dict
    private void setHat(HashMap<Character, String> Hat) {
        Hat.put('0', "  ");
        Hat.put('1', " q");
        Hat.put('2', "(|");
        Hat.put('3', "{|");
        Hat.put('4', "[|");
        Hat.put('5', "C|");
        Hat.put('6', " d");
        Hat.put('7', "<|");
    }

    //All possible Ceilings/Floors in IMAGING dict
    private void setRnF(HashMap<Character, Character> RoofnFloor) {
        RoofnFloor.put('0', ' ');
        RoofnFloor.put('1', '+');
        RoofnFloor.put('2', 'o');
        RoofnFloor.put('3', '|');
        RoofnFloor.put('4', '%');
        RoofnFloor.put('5', '@');
        RoofnFloor.put('6', '&');
        RoofnFloor.put('7', 'H');
    }

    //NAME OF A QRc "calculation"
    private String nameQRc(String base8) {

        return String.format(Locale.CANADA, "%s %s %s%s%s%s%s",
                Title.get(base8.charAt(0)),
                Descriptor.get(base8.charAt(1)),
                A.get(base8.charAt(2)),
                B.get(base8.charAt(3)),
                C.get(base8.charAt(4)),
                D.get(base8.charAt(5)),
                E.get(base8.charAt(6)));
    }

    //All possible Lastnames in NAMING dict
    private void setLastName(HashMap<Character, String> E) {
        E.put('0', "Crab");
        E.put('1', "Shark");
        E.put('2', "Shrimp");
        E.put('3', "Squid");
        E.put('4', "Clam");
        E.put('5', "Trout");
        E.put('6', "Cod");
        E.put('7', "Bass");
    }

    //All possible Fourth in NAMING dict
    private void setFourthName(HashMap<Character, String> D) {
        D.put('0', "Sonic");
        D.put('1', "Spectral");
        D.put('2', "Spastic");
        D.put('3', "Salient");
        D.put('4', "Sacred");
        D.put('5', "Scented");
        D.put('6', "Seamless");
        D.put('7', "Seasoned");
    }

    //All possible Thirdnames in NAMING dict
    private void setThirdName(HashMap<Character, String> C) {
        C.put('0', "Mega");
        C.put('1', "Ultra");
        C.put('2', "Super");
        C.put('3', "Contra");
        C.put('4', "Gamma");
        C.put('5', "Monsta");
        C.put('6', "Monga");
        C.put('7', "Monka");
    }

    //All possible Secondnames in NAMING dict
    private void setSecondName(HashMap<Character, String> B) {
        B.put('0', "Mo");
        B.put('1', "Lo");
        B.put('2', "No");
        B.put('3', "Po");
        B.put('4', "Ro");
        B.put('5', "Yo");
        B.put('6', "Go");
        B.put('7', "Jo");
    }

    //All possible Firstnames in NAMING dict
    private void setFirstName(HashMap<Character, String> A) {
        A.put('0', "Fro");
        A.put('1', "Glo");
        A.put('2', "Sno");
        A.put('3', "Dro");
        A.put('4', "Slo");
        A.put('5', "Zro");
        A.put('6', "Pro");
        A.put('7', "Cro");
    }

    //All possible descriptors in NAMING dict
    private void setDescriptors(HashMap<Character, String> Descriptor) {
        Descriptor.put('0', "cold");
        Descriptor.put('1', "hot");
        Descriptor.put('2', "warm");
        Descriptor.put('3', "cool");
        Descriptor.put('4', "smart");
        Descriptor.put('5', "black");
        Descriptor.put('6', "white");
        Descriptor.put('7', "strong");
    }

    //All possible titles in NAMING dict
    private void setTitles(HashMap<Character, String> Title) {
        Title.put('0', "King");
        Title.put('1', "Queen");
        Title.put('2', "Sir");
        Title.put('3', "Madam");
        Title.put('4', "Don");
        Title.put('5', "Cpt");
        Title.put('6', "Emperor");
        Title.put('7', "Empress");
    }


    //CALCULATE THE QRc SCORE
    public long score(String hex) {

        int count = hex.length(); //since hex shoud be 64
        int current = hex.charAt(0);
        int next;


        long value = 0;
        int combo = 0;
        for (int i = 1; i < count; i++) {
            next = hex.charAt(i);
            //Log.d("scoreTAG2", (char) next + " = " + converter.get((char)next));

            if (next == current) {
                combo++;
            } else {
                int y = (int) pow(converter.get((char) current), combo);
//                Log.d("scoreTAG3",(char)current + " " + y);
                value += y;
                combo = 0;
            }
            current = next;
        }
        value += pow(converter.get((char) current), combo);

        return value;
    }

    //convert bytes to hex String
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[(bytes.length << 1)];
        //*2 == <<1
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[(j << 1)] = HEX_ARRAY[v >>> 4];
            hexChars[(j << 1) + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }


    //convert hex string to string of base "to_base"
    public String change_of_base(String hex, int to_base){
        BigInteger BI = new BigInteger(hex, 16);
        return BI.toString(to_base);
    }


    //QRc Personality
}

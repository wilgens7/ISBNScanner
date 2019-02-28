package com.seprojects.wilgens7.isbnscanner;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView isbnNumberTextView;
    private Button snapButton;
    private Button findButton;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView imageView;
    private Bitmap bitmapImage;
    private String retrievedISBNNumber;
    private String TAG;
    FirebaseDatabase database;
    DatabaseReference myRef;
    FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //PRE EXISTING CODE
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //=====================================================================
        //=================MY CODE================

        snapButton = findViewById(R.id.snapButton);//click to take photo
        findButton = findViewById(R.id.findButton);// click after taking photo
        imageView = findViewById(R.id.imageView);// to show the photo taken
        isbnNumberTextView = findViewById(R.id.isbnNumberTextView);
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("book1");
        db = FirebaseFirestore.getInstance();
        readFromRealTimeDatabase();// reads from Real time data base
        //readFromFirestore(); // reads from fire store

        //Code to pull up camera and scan picture
        snapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dispatchTakePictureIntent();
            }
        });
        //code to retrieve isbn number
        findButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //converts bitmap image
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmapImage);
                //gets image recognizer
                FirebaseVisionTextRecognizer detector = FirebaseVision
                        .getInstance()
                        .getOnDeviceTextRecognizer();
                //processes image
                final Task<FirebaseVisionText> result =
                        detector.processImage(image)
                                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                    @Override
                                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                        // Task completed successfully
                                        updateISBN(firebaseVisionText);
                                    }
                                })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Task failed with an exception
                                                // ...
                                            }
                                        });



            }
        });


    }

    //method to start intent and take picture
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    //saves photo as bitmap in extras under "data"
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
             bitmapImage = (Bitmap) extras.get("data");
            imageView.setImageBitmap(bitmapImage);
        }
    }

    //updates ISBN string from the text recognizer
    private void updateISBN(FirebaseVisionText text){
        List<FirebaseVisionText.TextBlock> blocks = text.getTextBlocks();
        if(blocks.size() == 0){
            Toast.makeText(MainActivity.this,"No text found",Toast.LENGTH_LONG).show();
            return;
        }

        for(FirebaseVisionText.TextBlock block : text.getTextBlocks()){
            retrievedISBNNumber = block.getText();
            isbnNumberTextView.setText(retrievedISBNNumber);
        }
    }

    //============================IGNORE CODE BETWEEN THESE LINES===================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //================================================================================

    //experimenting 
    public void readFromRealTimeDatabase(){

        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d("FIREBASEDATA", "Value is: " + value);
                isbnNumberTextView.setText(value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("FIREBASE DATA", "Failed to read value.", error.toException());
            }
        });

        
    }

    //
    private void readFromFirestore(){
        db.collection("book")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                isbnNumberTextView.setText(String.valueOf(document.getData()));
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }
}


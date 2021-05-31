package com.blue.mytravellbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.admin.SystemUpdatePolicy;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity2 extends AppCompatActivity {

    Bitmap selectedImage;
    ImageView imageView;
    EditText cityNameText, regionText, descriptionText;
    Button save;
    SQLiteDatabase database;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        imageView = findViewById(R.id.imageView);
        cityNameText = findViewById(R.id.cityNameText);
        regionText = findViewById(R.id.regionText);
        descriptionText = findViewById(R.id.descriptionText);
        save = findViewById(R.id.save);

        database = this.openOrCreateDatabase("Travels",MODE_PRIVATE,null);


        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if (info.matches("new")){
            cityNameText.setText("");
            regionText.setText("");
            descriptionText.setText("");
            save.setVisibility(View.VISIBLE);

            Bitmap selectImage = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.selectimage);
            imageView.setImageBitmap((selectImage));

        }else{
          int travelId = intent.getIntExtra("travelId",1);
          save.setVisibility(View.INVISIBLE);

          try {
              Cursor cursor = database.rawQuery("SELECT*FROM travels WHERE id=?",new String[]{String.valueOf(travelId)});

              int cityNameIx = cursor.getColumnIndex("cityName");
              int regionNameIx = cursor.getColumnIndex("regionName");
              int descriptionIx = cursor.getColumnIndex("description");
              int imageIx = cursor.getColumnIndex("image");

              while (cursor.moveToNext()){

                  cityNameText.setText(cursor.getString(cityNameIx));
                  regionText.setText(cursor.getString(regionNameIx));
                  descriptionText.setText(cursor.getString(descriptionIx));

                  byte[] bytes = cursor.getBlob(imageIx);
                  Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                  imageView.setImageBitmap(bitmap);

              }
              cursor.close();


          }catch (Exception e){

          }



        }

    }

    public void selectImage(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},1);
        } else {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery,2);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery,2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {

            Uri imageData = data.getData();


            try {

                if (Build.VERSION.SDK_INT >= 28) {

                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectedImage = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectedImage);

                } else {
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageData);
                    imageView.setImageBitmap(selectedImage);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    public void delete(View view){
        String cityName = cityNameText.getText().toString();

        try {
            database = this.openOrCreateDatabase("Travels", MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS travels(id INTEGER PRIMARY KEY, cityName VARCHAR,regionName VARCHAR,description VARCHAR,image BLOB)");

            String sqlString ="DELETE FROM Travels WHERE cityName=?" ;
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,cityName);
            sqLiteStatement.execute();

        }catch (Exception e){

        }

        Intent intent = new Intent(MainActivity2.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);



    }
    public void update(View view){
        String cityName = cityNameText.getText().toString();
        String regionName = regionText.getText().toString();
        String description = descriptionText.getText().toString();
        try {
            database = this.openOrCreateDatabase("Travels", MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS travels(id INTEGER PRIMARY KEY, cityName VARCHAR,regionName VARCHAR,description VARCHAR,image BLOB)");

            String sqlString ="UPDATE Travels SET cityName =?, regionName=?, description=?  WHERE cityName=?" ;
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,cityName);
            sqLiteStatement.bindString(2,regionName);
            sqLiteStatement.bindString(3,description);
            sqLiteStatement.bindString(4,cityName);
            sqLiteStatement.execute();

        }catch (Exception e){

        }

        Intent intent = new Intent(MainActivity2.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }



    public void save(View view){

       String cityName = cityNameText.getText().toString();
       String regionName = regionText.getText().toString();
       String description = descriptionText.getText().toString();

       Bitmap smallImage = makeSmallerImage(selectedImage,300);

       ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); //görseli sqlite kaydetmek için dönüştürdük
       smallImage.compress(Bitmap.CompressFormat.PNG,50, outputStream);
       byte[] byteArray = outputStream.toByteArray();

       try {
           database = this.openOrCreateDatabase("Travels", MODE_PRIVATE,null);
           database.execSQL("CREATE TABLE IF NOT EXISTS travels(id INTEGER PRIMARY KEY, cityName VARCHAR,regionName VARCHAR,description VARCHAR,image BLOB)");

           String sqlString ="INSERT INTO travels (cityName,regionName,description,image) VALUES (?,?,?,?)";
           SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);//üstte verilen yani girilen değerleri dönüştürerek sqlite a kaydeder (stringi sqlite komutuna dönüştürür)
           sqLiteStatement.bindString(1,cityName);
           sqLiteStatement.bindString(2,regionName);
           sqLiteStatement.bindString(3,description);
           sqLiteStatement.bindBlob(4,byteArray);
           sqLiteStatement.execute();

       }catch (Exception e){

       }

       Intent intent = new Intent(MainActivity2.this,MainActivity.class);
       intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
       startActivity(intent);
       //finish();
    }
    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){ // sqlite için görsel küçültme işlemi
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1){
            width=maximumSize;
            height = (int) (width/bitmapRatio);

        }else{
            height =maximumSize;
            width = (int) (height*bitmapRatio);
        }

       return  Bitmap.createScaledBitmap(image,width,height,true);
    }
}
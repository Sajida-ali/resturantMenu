package com.resturant.menu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private EditText name, price, desc;
    private Button add;
    private ImageView meal;
    private CheckBox isFood, hot, cold, isDrink, vegan, flesh;

    private FirebaseFirestore db;
    private StorageReference storageRef;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        name = findViewById(R.id.PrudctName);
        price = findViewById(R.id.product_price);
        desc = findViewById(R.id.description);
        add = findViewById(R.id.save);
        isFood = findViewById(R.id.isFood);
        hot = findViewById(R.id.hot);
        cold = findViewById(R.id.cold);
        isDrink = findViewById(R.id.isDrink);
        vegan = findViewById(R.id.vegan);
        flesh = findViewById(R.id.flesh);
        meal = findViewById(R.id.meal);

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        meal.setOnClickListener(v -> {
            if (checkPermission(Manifest.permission.CAMERA)) {
                dispatchTakePictureIntent();
            } else {
                requestPermission(Manifest.permission.CAMERA);
            }
        });

        add.setOnClickListener(v -> {
            String productName = name.getText().toString();
            String productPrice = price.getText().toString();
            String productDescription = desc.getText().toString();
            boolean isFoodChecked = isFood.isChecked();
            boolean isDrinkChecked = isDrink.isChecked();
            boolean isVeganChecked = vegan.isChecked();
            boolean isColdChecked = cold.isChecked();

            if (productName.isEmpty() || productPrice.isEmpty() || productDescription.isEmpty()) {
                Toast.makeText(HomeActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Create a new product object with the details
                Map<String, Object> product = new HashMap<>();
                product.put("name", productName);
                product.put("price", Double.parseDouble(productPrice));
                product.put("description", productDescription);
                product.put("isFood", isFoodChecked);
                product.put("isDrink", isDrinkChecked);

                if (isFoodChecked) {
                    product.put("isVegan", isVeganChecked);
                } else if (isDrinkChecked) {
                    product.put("isCold", isColdChecked);
                }

                // Upload the photo and save the product details
                if (photoUri != null) {
                    uploadPhotoAndSaveProduct(photoUri, product);
                } else {
                    saveProductDetails(product);
                }
            }
        });
    }

    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String permission) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_IMAGE_PICK);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void dispatchPickImageIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    meal.setImageBitmap(imageBitmap);
                    photoUri = getImageUri(imageBitmap);
                }
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                if (data.getClipData() != null) {
                    // Multiple images selected
                    ClipData clipData = data.getClipData();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        Uri selectedImageUri = clipData.getItemAt(i).getUri();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            // Handle the bitmap or upload it to Firebase Storage
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (data.getData() != null) {
                    // Single image selected
                    Uri selectedImageUri = data.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        // Handle the bitmap or upload it to Firebase Storage
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "MealImage", null);
        return Uri.parse(path);
    }

    private void uploadPhotoAndSaveProduct(Uri photoUri, Map<String, Object> product) {
        String imageName = System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child("product_images/" + imageName);

        UploadTask uploadTask = imageRef.putFile(photoUri);
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                // Continue with the task to get the download URL
                return imageRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    if (downloadUri != null) {
                        product.put("photoUrl", downloadUri.toString());
                    }
                    saveProductDetails(product);
                } else {
                    // Handle error
                    Toast.makeText(HomeActivity.this, "Failed to upload photo", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error uploading photo: " + task.getException());
                }
            }
        });
    }

    private void saveProductDetails(Map<String, Object> product) {
        db.collection("products")
                .add(product)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(HomeActivity.this, "Product added successfully", Toast.LENGTH_SHORT).show();
                        resetForm();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(HomeActivity.this, "Failed to add product", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error adding product: " + e.getMessage());
                    }
                });
    }

    private void resetForm() {
        name.setText("");
        price.setText("");
        desc.setText("");
        isFood.setChecked(false);
        hot.setChecked(false);
        cold.setChecked(false);
        isDrink.setChecked(false);
        vegan.setChecked(false);
        flesh.setChecked(false);
        meal.setImageResource(R.drawable.ic_launcher_background);
        photoUri = null;
    }
}

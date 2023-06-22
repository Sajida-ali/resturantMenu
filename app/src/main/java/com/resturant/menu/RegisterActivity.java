package com.resturant.menu;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.resturant.menu.Model.Users;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText inputUsername, inputEmail, inputPass;
    private Button registerBtn, loginBtn;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        inputUsername = findViewById(R.id.username);
        inputEmail = findViewById(R.id.email);
        inputPass = findViewById(R.id.pass);
        registerBtn = findViewById(R.id.register);
        loginBtn = findViewById(R.id.login);

        registerBtn.setOnClickListener(this);
        loginBtn.setOnClickListener(this);

        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void onClick(View v) {
        if (v == loginBtn) {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        } else if (v == registerBtn) {
            isValidData();
        }
    }

    private void isValidData() {
        String username = inputUsername.getText().toString();
        String email = inputEmail.getText().toString();
        String password = inputPass.getText().toString();

        if (TextUtils.isEmpty(username) || username.length() < 5) {
            Toast.makeText(this, "Username must have 5 characters or more", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password) || !isValidPassword(password)) {
            Toast.makeText(getApplicationContext(), "Password must contain at least 8 characters, including letters, digits, and special symbols", Toast.LENGTH_SHORT).show();
            return;
        }

        checkIfUserExists(email);
    }

    private void checkIfUserExists(String email) {
        CollectionReference usersRef = db.collection("Users");
        usersRef.whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            Toast.makeText(RegisterActivity.this, "Please try another email", Toast.LENGTH_SHORT).show();
                        } else {
                            createAccount();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error checking user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createAccount() {
        String username = inputUsername.getText().toString();
        String email = inputEmail.getText().toString();
        String password = inputPass.getText().toString();

        Users user = new Users(username, email, password);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            String uid = currentUser.getUid();

                            CollectionReference usersRef = db.collection("Users");
                            usersRef.document(uid).set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Error creating account: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(RegisterActivity.this, "Failed to get current user.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error creating account: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidPassword(String password) {
        int letterCount = 0;
        int digitCount = 0;
        int specialCharCount = 0;

        if (password.length() < 8) {
            return false;
        }

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                letterCount++;
            } else if (Character.isDigit(c)) {
                digitCount++;
            } else {
                specialCharCount++;
            }
        }

        return letterCount >= 1 && digitCount >= 1 && specialCharCount >= 1;
    }
}

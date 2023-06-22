package com.resturant.menu;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;



public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.loginM);

        loginButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent i=new Intent(MainActivity.this,LoginActivity.class);
        startActivity(i);
    }
}
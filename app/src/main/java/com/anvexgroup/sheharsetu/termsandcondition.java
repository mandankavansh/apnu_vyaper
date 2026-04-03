package com.anvexgroup.sheharsetu;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class termsandcondition extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://anvexgroup.com/Terms_and_condition.php")));
        finish();


    }
}
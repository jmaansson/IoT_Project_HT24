package com.example.iot_project_test;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private boolean locked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ImageView shield = findViewById(R.id.shield);
        Button lock_btn = findViewById(R.id.lock_button);
        lock_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locked) {
                    lock_btn.setText(getString(R.string.unlock_label));
                    // lock_btn.setIconResource(R.drawable.lock_open_24dp);
                    lock_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock_open_24dp, 0, 0, 0);
                    lock_btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#006600")));
                    // shield.setBackground(R.drawable.shield_locked_144dp);
                    locked = false;
                    shield.setImageResource(R.drawable.shield_locked_144dp);
                } else {
                    lock_btn.setText(getString(R.string.lock_label));
                    lock_btn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock_24dp, 0, 0, 0);
                    lock_btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8A1919")));
                    shield.setImageResource(R.drawable.shield_144dp);
                    locked = true;
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
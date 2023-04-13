package edu.harvard.cs50.kiwi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private EditText editTextUser;
    private Button buttonSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Display logo in toolbar
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.kiwi_logo);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        editTextUser = findViewById(R.id.text_from_user);
        buttonSubmit = findViewById(R.id.button_submit);

        // Enable/disable button on text input/removal
        editTextUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String userText = editTextUser.getText().toString();

                // Button disabled unless user input provided
                buttonSubmit.setEnabled(false);
                if (!userText.trim().isEmpty()) {
                    buttonSubmit.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    public void submitText(View view) {
        String userText = editTextUser.getText().toString();

        // Pass text on to wikiActivity
        Intent wikiIntent = new Intent(getBaseContext(), WikiActivity.class);
        wikiIntent.putExtra("userText", userText);

        view.getContext().startActivity(wikiIntent);
    };
}

package com.bawazir.webrefresh;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SetupActivity extends AppCompatActivity {

    private EditText urlInput;
    private EditText refreshInput;
    private TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        urlInput = findViewById(R.id.urlInput);
        refreshInput = findViewById(R.id.refreshInput);
        error = findViewById(R.id.error);
        Button save = findViewById(R.id.save);

        String existing = Prefs.getUrl(this);
        if (!TextUtils.isEmpty(existing)) {
            urlInput.setText(existing);
            urlInput.setSelection(existing.length());
        }
        int r = Prefs.getRefreshSeconds(this);
        if (r > 0 && r != 60) refreshInput.setText(String.valueOf(r));

        save.setOnClickListener(v -> onSave());
    }

    private void onSave() {
        String url = urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            error.setText(R.string.error_empty);
            error.setVisibility(View.VISIBLE);
            return;
        }
        if (!Prefs.isValidUrl(url)) {
            error.setText(R.string.error_url);
            error.setVisibility(View.VISIBLE);
            return;
        }
        error.setVisibility(View.GONE);

        Prefs.setUrl(this, url);
        String rStr = refreshInput.getText().toString().trim();
        if (!rStr.isEmpty()) {
            try {
                int r = Integer.parseInt(rStr);
                if (r >= 5) Prefs.setRefreshSeconds(this, r);
            } catch (NumberFormatException ignored) { }
        }

        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_URL, url);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
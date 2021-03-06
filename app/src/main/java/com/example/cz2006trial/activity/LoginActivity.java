package com.example.cz2006trial.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cz2006trial.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText mEmail, mPassword;
    private Button mLoginButton;
    private TextView mRegisterButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEmail = findViewById(R.id.email2);
        mPassword = findViewById(R.id.password2);
        mLoginButton = findViewById(R.id.loginButton);
        mRegisterButton = findViewById(R.id.fromLoginToRegister);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // trim email and password to get rid of unnecessary spaces
                String email = mEmail.getText().toString().trim();
                String password = mPassword.getText().toString().trim();

                // if email field is empty
                if (TextUtils.isEmpty(email)) {
                    mEmail.setError("Email is Required");
                    return;
                }
                // if password field is empty
                if (TextUtils.isEmpty(password)) {
                    mPassword.setError("Password is Required");
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);

                // listen to firebase and wait till firebase has authenticate the user
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // upon unsuccessful login, display error message
                        if (!task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Error " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                        // upon successful login, direct user to Maps page
                        else {
                            startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                            finish();
                        }
                    }
                });
            }
        });

        // direct user to sign up page when clicked
        mRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
                finish();
            }
        });
    }
}

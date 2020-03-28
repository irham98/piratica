package com.example.cz2006trial.fragment;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cz2006trial.DatabaseManager;
import com.example.cz2006trial.model.Goal;
import com.example.cz2006trial.controller.GoogleMapController;
import com.example.cz2006trial.R;
import com.example.cz2006trial.controller.UserLocationController;
import com.example.cz2006trial.model.UserLocationSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MapsTrackFragment extends Fragment {

    private static final String TAG = "MapsTrackFragment";
    private Button startButton;
    private Button endButton;
    private Chronometer mChronometer;
    private TextView distanceTravelledView;
    private TextView goalProgressView;
    private long timeElapsed = 0;
    private UserLocationSession userLocationSession = new UserLocationSession();

    private GoogleMapController controller = GoogleMapController.getController();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_track, container, false);
        startButton = view.findViewById(R.id.buttonStart);
        endButton = view.findViewById(R.id.buttonEnd);
        mChronometer = view.findViewById(R.id.chronometer);
        distanceTravelledView = view.findViewById(R.id.distanceTravelled);
        goalProgressView = view.findViewById(R.id.goalProgress);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (startButton.getText().equals("Start")) {
                    endButton.setText("End");
                    mChronometer.setBase(SystemClock.elapsedRealtime() - timeElapsed);
                    startButton.setText("Pause");
                    endButton.setVisibility(View.VISIBLE);
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    userLocationSession = new UserLocationSession(Calendar.getInstance().getTime());
                    displaySession(userLocationSession);
                    controller.beginTracking(userLocationSession);
                } else if (startButton.getText().equals("Resume")) {
                    mChronometer.setBase(SystemClock.elapsedRealtime() - timeElapsed);
                    startButton.setText("Pause");
                    mChronometer.start();
                    controller.resumeTracking();
                } else {
                    startButton.setText("Resume");
                    mChronometer.stop();
                    timeElapsed = SystemClock.elapsedRealtime() - mChronometer.getBase();
                    controller.endTracking();
                }
            }
        });

        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (endButton.getText().equals("End")) {
                    endButton.setText("Save");
                    startButton.setText("Start");
                    mChronometer.stop();
                    timeElapsed = SystemClock.elapsedRealtime() - mChronometer.getBase();
                    controller.endTracking();
                } else {
                    UserLocationController.setTimeTaken(userLocationSession, timeElapsed);
                    DatabaseManager.updateUserLocationSession(userLocationSession);
                    endButton.setText("End");
                    endButton.setVisibility(View.INVISIBLE);
                }
            }
        });

        return view;
    }

    public void displaySession(final UserLocationSession userLocationSession) {
        controller.setDisplayTrackingDistanceListener(new GoogleMapController.DisplayTrackingDistanceListener() {
            @Override
            public void onChange() {
                distanceTravelledView.setText("Distance travelled: " + Math.round(userLocationSession.getDistance() * 10) / 10.0 + " km");
                distanceTravelledView.setVisibility(View.VISIBLE);
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                final String dateString = dateFormat.format(userLocationSession.getTimestamp());
                DatabaseManager.getGoalData(new DatabaseManager.GoalDatabaseCallback() {
                    @Override
                    public void onCallback(ArrayList<String> stringArgs, double[] doubleArgs, String[] errorMsg, ArrayList<Goal> goals) {
                        if (errorMsg[0] != null)
                            Toast.makeText(getContext(), errorMsg[0], Toast.LENGTH_LONG).show();
                        else if (errorMsg[1] != null) {
                            goalProgressView.setText(R.string.noTargetSet);
                        } else {
                            if (doubleArgs[1] != -1 && doubleArgs[1] != 0) {
                                System.out.println("Target" + doubleArgs[0]);
                                goalProgressView.setText("Target: " + (Math.round(doubleArgs[0] * 10) / 10.0) + " / " + doubleArgs[1] + " km");
                            } else {
                                goalProgressView.setText(R.string.noTargetSet);

                            }
                        }
                    }
                }, dateString);
            }
        });
    }

    /*public void displaySessionFromDatabase(final Date date) {
        if (date != null) {
            String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference databaseUserSession = FirebaseDatabase.getInstance().getReference()
                    .child(UID).child("userLocationSessions").child(date.toString());
            databaseUserSession.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    UserLocationSession userSession = dataSnapshot.getValue(UserLocationSession.class);
                    if (userSession != null) {
                        UserLocationController.calculateNSetTimeTaken(userLocationSession, SystemClock.elapsedRealtime() - mChronometer.getBase());
                        distanceTravelledView.setText("Distance travelled: " + Math.round(userSession.getDistance() * 10) / 10.0 + " km");
                        distanceTravelledView.setVisibility(View.VISIBLE);
                        displayGoalTarget(date);

                    } else {
                        distanceTravelledView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    System.out.println("The read failed: " + databaseError.getCode());
                }
            });
        }
    }*/

    /*public void displayGoalTarget(Date date) {
        String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        final String dateString = dateFormat.format(date);
        DatabaseReference databaseGoals = FirebaseDatabase.getInstance().getReference().child(UID).child("goals").child(dateString);
        databaseGoals.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Goal goal = dataSnapshot.getValue(Goal.class);
                if (goal != null) {
                    if (goal.getTarget() != -1) {
                        goalProgressView.setText("Target: " + (Math.round(goal.getDistance() * 10) / 10.0) + " / " + goal.getTarget() + " km");
                    } else {
                        goalProgressView.setText(R.string.noTargetSet);
                    }
                } else {
                    goalProgressView.setText(R.string.noTargetSet);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }*/
}

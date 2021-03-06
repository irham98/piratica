package com.example.cz2006trial.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cz2006trial.controller.GoogleMapController;
import com.example.cz2006trial.R;
import com.example.cz2006trial.controller.UserRouteController;
import com.example.cz2006trial.database.DatabaseManager;
import com.example.cz2006trial.model.UserRoute;

/**
 * This fragment is used to create route based on a starting location and an ending location
 * chosen from access point markers on the map
 */
public class MapsCreateFragment extends Fragment {

    private static final String TAG = "MapsCreateFragment";

    private Button createButton;
    private Button saveButton;
    private Button setStartButton;
    private Button setEndButton;
    private TextView startPoint;
    private TextView endPoint;
    private UserRoute userRoute = new UserRoute();

    private GoogleMapController controller = GoogleMapController.getController();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_create, container, false);

        createButton = view.findViewById(R.id.buttonCreate);
        saveButton = view.findViewById(R.id.buttonSave);
        startPoint = view.findViewById(R.id.startPoint);
        endPoint = view.findViewById(R.id.endPoint);
        setStartButton = view.findViewById(R.id.buttonSetStart);
        setEndButton = view.findViewById(R.id.buttonSetEnd);

        displayStartEndText();

        setStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // when button is 'SET', allow users to select a starting location
                if (setStartButton.getText().equals("SET")) {
                    setStartButton.setText("DONE");
                    createButton.setVisibility(View.GONE);
                    setEndButton.setVisibility(View.GONE);
                    endPoint.setVisibility(View.GONE);
                    controller.setCreatePoint(true, userRoute);
                }
                // when button is 'DONE', prevent users from selecting a starting location
                // display starting location chosen
                else {
                    setStartButton.setText("SET");
                    createButton.setVisibility(View.VISIBLE);
                    setEndButton.setVisibility(View.VISIBLE);
                    endPoint.setVisibility(View.VISIBLE);
                    controller.stopSettingPoints();
                    if (UserRouteController.getStartPointName(userRoute) != null)
                        Toast.makeText(getActivity(), "Starting Point set", Toast.LENGTH_SHORT).show();
                    displayStartEndText();
                }
            }
        });

        setEndButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // when button is 'SET', allow users to select a ending location
                if (setEndButton.getText().equals("SET")) {
                    setEndButton.setText("DONE");
                    createButton.setVisibility(View.GONE);
                    startPoint.setVisibility(View.GONE);
                    setStartButton.setVisibility(View.GONE);
                    controller.setCreatePoint(false, userRoute);
                }
                // when button is 'DONE', prevent users from selecting a ending location
                // display ending location chosen
                else {
                    setEndButton.setText("SET");
                    setStartButton.setVisibility(View.VISIBLE);
                    createButton.setVisibility(View.VISIBLE);
                    startPoint.setVisibility(View.VISIBLE);
                    controller.stopSettingPoints();
                    if (UserRouteController.getEndPointName(userRoute) != null)
                        Toast.makeText(getActivity(), "Ending Point set", Toast.LENGTH_SHORT).show();
                    displayStartEndText();
                }
            }
        });


        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // when button is 'Create', create route using GoogleMaps API via GoogleMapController
                if (createButton.getText().equals("Create")) {
                    controller.create(userRoute);
                    String message = controller.getMessage();
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                    if (message.equals("Route created")) {
                        setStartButton.setVisibility(View.GONE);
                        setEndButton.setVisibility(View.GONE);
                        createButton.setText("Create New");
                        saveButton.setVisibility(View.VISIBLE);
                    }
                }
                // when button is 'Create New', clear route on map using GoogleMapController
                else {
                    controller.clearRoute();
                    userRoute = new UserRoute();
                    createButton.setText("Create");
                    displayStartEndText();
                    setStartButton.setVisibility(View.VISIBLE);
                    setEndButton.setVisibility(View.VISIBLE);
                    saveButton.setVisibility(View.GONE);
                }
            }
        });

        // when button is clicked, update created route to firebase database via Database Manager
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveButton.setVisibility(View.GONE);
                DatabaseManager.updateUserRouteDatabase(userRoute);
                Toast.makeText(getActivity(), "Route saved successfully", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // display starting and ending location if any
    public void displayStartEndText() {
        userRoute = controller.getUserRoute();
        String startPointText = UserRouteController.getStartPointName(userRoute);
        String endPointText = UserRouteController.getEndPointName(userRoute);
        if (startPointText != null)
            startPoint.setText("Start Point: " + startPointText);
        else
            startPoint.setText("Start Point: Select an access point marker");
        if (endPointText != null)
            endPoint.setText("End Point: " + endPointText);
        else
            endPoint.setText("End Point: Select an access point marker");
    }

    // to make sure that the route created is still on the map when user comes back from different fragment
    @Override
    public void onStart() {
        super.onStart();
        if (!controller.getRoute().isEmpty()) {
            createButton.setText("Create New");
            saveButton.setVisibility(View.VISIBLE);
            setStartButton.setVisibility(View.GONE);
            setEndButton.setVisibility(View.GONE);
        }
    }
}

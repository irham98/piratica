package com.example.cz2006trial.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cz2006trial.database.DatabaseManager;
import com.example.cz2006trial.adapter.PointRecyclerViewAdapter;
import com.example.cz2006trial.controller.GoogleMapController;
import com.example.cz2006trial.R;
import com.example.cz2006trial.controller.UserLocationController;
import com.example.cz2006trial.model.Point;
import com.example.cz2006trial.model.UserLocationSession;
import com.example.cz2006trial.controller.UserRouteController;
import com.example.cz2006trial.model.UserRoute;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineString;
import com.google.maps.android.data.geojson.GeoJsonPoint;
import com.google.maps.android.data.kml.KmlLayer;

import org.json.JSONException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This fragment is used to display google map along with its functionalities.
 * Functionalities include search, GPS, sliders to display parks including park connectors, create and track route
 */
public class MapFragment extends Fragment implements OnMapReadyCallback{

    private ImageView arrowImg;

    private GoogleMap mMap;

    private boolean startTrack = false;
    private ArrayList<Polyline> startTrackLine = new ArrayList<>();

    private UserLocationSession userLocationSession = new UserLocationSession();

    private boolean createRoute = false;
    private boolean setStartPoint = false;
    private boolean setEndPoint = false;
    private UserRoute userRoute = new UserRoute();
    private Marker startPoint;
    private Marker endPoint;
    private ArrayList<Polyline> routeLine = new ArrayList<>();

    private KmlLayer parklayer;
    private GeoJsonLayer pcnLayer;
    private KmlLayer accesslayer;
    private GeoJsonLayer accessLayer;

    private BottomSheetBehavior bottomSheetBehavior;


    private ArrayList<LatLng> locations = new ArrayList<>();
    private ArrayList<Marker> accessPoint = new ArrayList<>();

    private GoogleMapController controller = GoogleMapController.getController();


    private final long MINTIME = 1000 * 2;
    private final float MINDIST = 0;
    private final int ZOOM = 13;
    private final int ZOOM_OUT = 10;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private LatLng userLocation;
    private LatLng lastLocation;


    private LocationManager locationManager;
    private LocationListener locationListener;

    private Marker userMarker;
    private GeoJsonLayer parksLayer;
    private ArrayList<Marker> parkPoint = new ArrayList<>();;
    private ArrayList<Polyline> pcnLines = new ArrayList<>();

    private SearchView searchBar;
    private ArrayList<Point> pointList;
    private PointRecyclerViewAdapter adapter;

    private Point searchResult;
    private Switch switchPark;
    private Switch switchPcn;
    private RecyclerView listPoints;
    private TextView buffer;

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater,
                              ViewGroup container, Bundle savedInstanceState)  {

        Log.i("Mapfragment", "create");

        View root = inflater.inflate(R.layout.fragment_map, container, false);


        searchBar = root.findViewById(R.id.search_point);
        listPoints = root.findViewById(R.id.list_point);
        pointList = new ArrayList<>();

        buffer = root.findViewById(R.id.buffer);

        adapter = new PointRecyclerViewAdapter(R.layout.points_search_item, pointList, new PointRecyclerViewAdapter.PointsAdapterListener() {
            @Override
            public void onPointSelected(Point point) {
                searchResult = point;
                togglePoint(point, true);
                searchBar.setIconified(true);
                listPoints.setVisibility(View.GONE);
                buffer.setVisibility(View.VISIBLE);
            }
        });
        listPoints.setLayoutManager(new LinearLayoutManager(getContext()));
        listPoints.setAdapter(adapter);

        searchBar.setVisibility(View.GONE);
        listPoints.setVisibility(View.GONE);

        searchBar.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPoints.setVisibility(View.VISIBLE);
                if (searchResult != null)
                    togglePoint(searchResult, false);
                buffer.setVisibility(View.GONE);
            }
        });


        searchBar.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                listPoints.setVisibility(View.GONE);
                buffer.setVisibility(View.VISIBLE);
                return false;
            }

        });

        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //adapter.getFilter().filter(s);
                //searchBar.setIconified(true);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                adapter.getFilter().filter(s);
                return true;
            }
        });

        switchPark = root.findViewById(R.id.switch_park);
        switchPcn = root.findViewById(R.id.switch_pcn);
        switchPark.setVisibility(View.GONE);
        switchPcn.setVisibility(View.GONE);

        switchPark.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) showPark();
                else removePark();
            }
        });

        switchPcn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) showPcn();
                else removePcn();
            }
        });

        arrowImg = root.findViewById(R.id.arrow_bottom_sheet);
        arrowImg.setImageResource(R.drawable.ic_arrow_up);
        final View bottomSheet = root.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        //clicking on arrow will expand/collapse bottom sheet
        arrowImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        // to set image of arrow to allow create and track route functionality to minimize or maximize
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch(newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        arrowImg.setImageResource(R.drawable.ic_arrow_up);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        arrowImg.setImageResource(R.drawable.ic_arrow_down);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        BottomNavigationView navigationView = root.findViewById(R.id.bottom_nav);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_create, R.id.nav_track)
                .build();

        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.bottom_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(navigationView, navController);

        return root;
    }

    private void togglePoint(Point point, boolean show) {
        if (mMap != null) {
            if (point.getType().equals("access")) {
                for (Marker marker: accessPoint) {
                    if (marker.getTitle().equals(point.getName())) {
                        marker.setVisible(show);
                        break;
                    }
                }

            } else {
                for (Marker marker: parkPoint) {
                    if (marker.getTitle().equals(point.getName())) {
                        marker.setVisible(show);
                        break;
                    }
                }
            }
            if (show)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point.getLocation(), mMap.getCameraPosition().zoom));
        }
    }


    // get the map fragment
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void showPark() {
        if (mMap != null && parkPoint != null) {
            for (Marker marker: parkPoint) {
                marker.setVisible(true);
            }
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if(!marker.getTitle().equals("Your Location")) {
                        String title = marker.getTitle();
                        controller.getInfo(title);
                        controller.setPlaceListener(new GoogleMapController.PlaceListener() {
                            @Override
                            public void onChange() {
                                HashMap <String, Object> info = controller.getParkInfo();
                                for (HashMap.Entry<String, Object> entry : info.entrySet()) {
                                    String key = entry.getKey();
                                    Object value = entry.getValue();
                                    Log.i(key, value.toString());
                                }
                                DialogFragment parksFragment = ParksFragment.newInstance(info);
                                parksFragment.show(getChildFragmentManager(), "Dialog shown");
                            }
                        });
                    }
                    return false;
                }
            });
        }
    }

    private void removePark() {
        if (mMap != null && parkPoint != null) {
            for (Marker marker: parkPoint) {
                marker.setVisible(false);
            }
        }
        mMap.setOnMarkerClickListener(null);
    }

    private void showPcn() {
        if (mMap != null && pcnLines != null) {
            for (Polyline line: pcnLines) {
                line.setVisible(true);
            }
        }
    }

    private void removePcn() {
        if (mMap != null && pcnLines != null) {
            for (Polyline line : pcnLines) {
                line.setVisible(false);
            }
        }
    }

    public void clearTrack() {
        controller.setClearTrackListener(new GoogleMapController.ClearTrackListener() {
            @Override
            public void onChange() {
                System.out.println("Inside");
                for (int i = 0; i < startTrackLine.size(); i++)
                    startTrackLine.get(i).remove();
                startTrackLine.clear();
                locations.clear();

            }
        });
    }

    public void setStartingPoint() {
        controller.setStartListener(new GoogleMapController.StartListener() {
            @Override
            public void onChange() {
                setStartPoint = controller.isSetStartPoint();
                userRoute = controller.getUserRoute();

                if (setStartPoint) {
                    for (Marker marker : accessPoint) {
                        marker.setVisible(true);
                    }
                    if (searchResult != null)
                        togglePoint(searchResult, false);
                    if (startPoint != null)
                        startPoint.remove();


                    mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            if (!marker.getTitle().equals("Your Location"))
                                if (endPoint == null || !marker.getTitle().equals("Your Ending Location")) {
                                    if (startPoint != null)
                                        startPoint.remove();
                                    startPoint = mMap.addMarker(new MarkerOptions().position(marker.getPosition())
                                            .title(marker.getTitle())
                                            .snippet("Your Starting Point")
                                            .zIndex(2f)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startPoint.getPosition(), mMap.getCameraPosition().zoom));
                                    UserRouteController.setStartMarkerInfo(userRoute, startPoint);

                                }
                            return false;
                        }


                    });
                } else {
                    for (Marker marker : accessPoint) {
                        marker.setVisible(false);
                    }
                }
            }
        });
    }

    public void setEndingPoint() {
        controller.setEndListener(new GoogleMapController.EndListener() {
            @Override
            public void onChange() {
                setEndPoint = controller.isSetEndPoint();
                userRoute = controller.getUserRoute();
                if (setEndPoint) {
                    if (searchResult != null)
                        togglePoint(searchResult, false);
                    if (endPoint != null)
                        endPoint.remove();
                    for (Marker marker : accessPoint) {
                        marker.setVisible(true);
                    }
                    mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            if (!marker.getTitle().equals("Your Location"))
                                if (startPoint == null || !marker.getTitle().equals(startPoint.getTitle())) {
                                    if (endPoint != null)
                                        endPoint.remove();
                                    endPoint = mMap.addMarker(new MarkerOptions().position(marker.getPosition())
                                            .title(marker.getTitle())
                                            .snippet("Your Ending Location")
                                            .zIndex(1f)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(endPoint.getPosition(), mMap.getCameraPosition().zoom));
                                    UserRouteController.setEndMarkerInfo(userRoute, endPoint);
                                }
                            return false;
                        }
                    });
                } else {
                    for (Marker marker : accessPoint) {
                        marker.setVisible(false);
                    }
                }

            }
        });
    }


    public void createRoute() {
        controller.setCreateListener(new GoogleMapController.CreateListener() {
            @Override
            public void onChange() {
                createRoute = controller.isCreateRoute();
                if (createRoute) {
                    if (startPoint == null || endPoint == null)
                        controller.setMessage("Missing starting point or ending point");
                    else {
                        userRoute = controller.getUserRoute();
                        createRoute = true;
                        controller.getDirections(startPoint.getPosition(), endPoint.getPosition());
                        routeDone();
                        controller.setMessage("Route created");

                    }
                } else {
                    startPoint.remove();
                    endPoint.remove();
                    for (int i = 0; i < routeLine.size(); i++)
                        routeLine.get(i).remove();
                    startPoint = null;
                    endPoint = null;
                    routeLine.clear();
                }
            }

        });

    }

    public void routeDone() {
        controller.setRouteListener(new GoogleMapController.RouteListener() {
            @Override
            public void onChange() {
                ArrayList<LatLng> route = controller.getRoute();
                Log.i("route", route.toString());
                routeLine.add(mMap.addPolyline(new PolylineOptions().addAll(route).width(10.0f).color(Color.BLACK)));
                if (startPoint != null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startPoint.getPosition(), mMap.getCameraPosition().zoom));

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        searchBar.setVisibility(View.VISIBLE);
        switchPark.setVisibility(View.VISIBLE);
        switchPcn.setVisibility(View.VISIBLE);

        setStartingPoint();
        setEndingPoint();
        createRoute();
        clearTrack();

        userRoute = controller.getUserRoute();

        if (userRoute.getStartPointName() != null) {
            startPoint = mMap.addMarker(new MarkerOptions().position(userRoute.startPointRetrieve())
                    .title(userRoute.getStartPointName())
                    .snippet("Your Starting Point")
                    .zIndex(2f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        }
        if (userRoute.getEndPointName() != null) {
            endPoint = mMap.addMarker(new MarkerOptions().position(userRoute.endPointRetrieve())
                    .title(userRoute.getEndPointName())
                    .snippet("Your Ending Point")
                    .zIndex(2f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        }
        if (!controller.getRoute().isEmpty()) {
            Toast.makeText(getActivity(), "Inside", Toast.LENGTH_SHORT).show();
            controller.create(userRoute);
        }


        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {

            //to prevent camera to move back to user location every second
            boolean isFirstTime = true;

            //method to move location according to user's position
            @Override
            public void onLocationChanged(Location location) {
                //update current position of user
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                if (isFirstTime) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, ZOOM));
                    isFirstTime = false;
                }

                if (userLocation != null)
                    lastLocation = userLocation;


                //check if tracking is enabled
                startTrack = controller.isStartTrack();
                if (startTrack) {
                    if (locations.isEmpty())
                        locations = controller.getLocations();
                    userLocationSession = controller.getUserLocationSession();
                    locations.add(lastLocation);
                    controller.setLocations(locations);
                    startTrackLine.add(mMap.addPolyline(new PolylineOptions().addAll(locations).width(10.0f).color(Color.RED)));
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
                    TimeZone tz = TimeZone.getTimeZone("Asia/Singapore");
                    sdf.setTimeZone(tz);
                    java.util.Date curDate = new java.util.Date();
                    String date = sdf.format(curDate);
                    UserLocationController.addUserLocation(userLocationSession, lastLocation, DatabaseManager.convertStringToDate(date));
                }


                if (userMarker != null)
                    userMarker.remove();
                userMarker = mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));


            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        try {
            pcnLayer = new GeoJsonLayer(mMap, R.raw.parkconnectorloopg, getContext());
            accessLayer = new GeoJsonLayer(mMap, R.raw.accesspointsg, getContext());
            parksLayer = new GeoJsonLayer(mMap, R.raw.parksg, getContext());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            parklayer = new KmlLayer(mMap, R.raw.parkconnectorloop, getContext());
            accesslayer = new KmlLayer(mMap, R.raw.accesspoints, getContext());


        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //get the access points from geojson
        extractGeodata(accessLayer);
        //get the points of parks from geojson
        extractGeodata(parksLayer);


        //get the park connector network from geojson
        extractGeodata(pcnLayer);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINTIME, MINDIST, locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnownLocation != null) {
                userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, ZOOM));
                sortLocations(pointList, userLocation);
                adapter.notifyDataSetChanged();
            } else {
                LatLng sgLoc = new LatLng(1.3521, 103.8198);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sgLoc, ZOOM_OUT));
            }
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

    }

    private void extractGeodata(GeoJsonLayer layer) {
        if (layer != null) {
            if (layer.equals(pcnLayer)) {
                for (GeoJsonFeature feature : pcnLayer.getFeatures()) {
                    if("LineString".equalsIgnoreCase(feature.getGeometry().getGeometryType())) {
                        GeoJsonLineString line = (GeoJsonLineString) feature.getGeometry();
                        ArrayList<LatLng> latLngList = (ArrayList)line.getCoordinates();
                        Resources res = getActivity().getResources();
                        switch(feature.getProperty("PCN_LOOP")) {
                            case "Central Urban Loop":
                                pcnLines.add(mMap.addPolyline(new PolylineOptions().addAll(latLngList).visible(false)
                                        .color(res.getColor(R.color.color1, getActivity().getTheme()))));
                                break;
                            case "Eastern Coastal Loop":
                                pcnLines.add(mMap.addPolyline(new PolylineOptions().addAll(latLngList).visible(false)
                                        .color(res.getColor(R.color.color2, getActivity().getTheme()))));
                                break;
                            case "Northern Explorer Loop":
                                pcnLines.add(mMap.addPolyline(new PolylineOptions().addAll(latLngList).visible(false)
                                        .color(res.getColor(R.color.color3, getActivity().getTheme()))));
                                break;
                            case "North Eastern Riverine Loop":
                                pcnLines.add(mMap.addPolyline(new PolylineOptions().addAll(latLngList).visible(false)
                                        .color(res.getColor(R.color.color4, getActivity().getTheme()))));
                                break;
                            case "Southern Ridges Loop":
                                pcnLines.add(mMap.addPolyline(new PolylineOptions().addAll(latLngList).visible(false)
                                        .color(res.getColor(R.color.color5, getActivity().getTheme()))));
                                break;
                            case "Western Adventure Loop":
                                pcnLines.add(mMap.addPolyline(new PolylineOptions().addAll(latLngList).visible(false)
                                        .color(res.getColor(R.color.color6, getActivity().getTheme()))));
                                break;

                        }
                    }
                }
            } else {
                for (GeoJsonFeature feature : layer.getFeatures()) {
                    if ("Point".equalsIgnoreCase(feature.getGeometry().getGeometryType())) {
                        GeoJsonPoint p = (GeoJsonPoint) feature.getGeometry();
                        LatLng latLng = p.getCoordinates();

                        if (layer.equals(accessLayer)) {
                            Point point = new Point(feature.getProperty("Name"), latLng, "access", "access");
                            pointList.add(point);
                            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(feature.getProperty("Name"))
                                    .visible(false));
                            accessPoint.add(marker);
                        } else {
                            Point point = new Point(feature.getProperty("Name"), latLng, feature.getProperty("description"), "park");
                            pointList.add(point);
                            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(feature.getProperty("Name"))
                                    .visible(false));
                            parkPoint.add(marker);
                        }
                    }
                }
            }

        }
    }

    //method to check whether permission for location access has been granted
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    //how often location is updated
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINTIME, MINDIST, locationListener);
                }
            }
        }
    }

    public static void sortLocations(ArrayList<Point> locations, final LatLng user) {
        Comparator comp = new Comparator<Point>() {
            @Override
            public int compare(Point o, Point o2) {
                float[] result1 = new float[3];
                android.location.Location.distanceBetween(user.latitude, user.longitude,
                        o.getLocation().latitude, o.getLocation().longitude, result1);
                Float distance1 = result1[0];

                float[] result2 = new float[3];
                android.location.Location.distanceBetween(user.latitude, user.longitude,
                        o2.getLocation().latitude, o2.getLocation().longitude, result2);
                Float distance2 = result2[0];

                return distance1.compareTo(distance2);
            }
        };


        Collections.sort(locations, comp);
    }

}

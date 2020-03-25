package com.example.cz2006trial;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.viewpager.widget.ViewPager;

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
import com.google.android.material.navigation.NavigationView;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPoint;
import com.google.maps.android.data.kml.KmlLayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback{

    private TextView peekText;
    private ImageView arrowImg;

    GoogleMap mMap;

    private boolean startTrack = false;

    private UserLocationSessionEntity userLocationSession = new UserLocationSessionEntity();

    private boolean createRoute = false;
    private boolean setStartPoint = false;
    private boolean setEndPoint = false;
    private UserRouteEntity userRoute = new UserRouteEntity();
    private Marker startPoint;
    private Marker endPoint;
    private ArrayList<Polyline> routeLine = new ArrayList<>();

    KmlLayer parklayer;
    GeoJsonLayer parklayerjson;
    KmlLayer accesslayer;
    GeoJsonLayer accesslayerjson;

    private BottomSheetBehavior bottomSheetBehavior;


    ArrayList<LatLng> locations = new ArrayList<>();
    ArrayList<Marker> accessPoint = new ArrayList<>();

    private GoogleMapController controller = GoogleMapController.getController();

    private SectionsStatePagerAdapter mSectionsStatePagerAdapter; // Swa
    private ViewPager mViewPager; // Swa

    private final long MINTIME = 1000 * 2;
    private final float MINDIST = 0;
    private final int ZOOM = 12;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private LatLng userLocation;
    private LatLng lastLocation;

    private View root;


    LocationManager locationManager;
    LocationListener locationListener;

    Marker userMarker;

    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;

    //method to check whether permission for location access has been granted
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    //how often location is updated
                    //startTrackerService();
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINTIME, MINDIST, locationListener);
                }
            }
        }
    }

    @Override
    public View onCreateView (@NonNull LayoutInflater inflater,
                              ViewGroup container, Bundle savedInstanceState)  {

        root = inflater.inflate(R.layout.fragment_map, container, false);

        peekText = root.findViewById(R.id.peek_text);

        arrowImg = root.findViewById(R.id.arrow_bottom_sheet);
        View bottomSheet = root.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

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
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_create, R.id.nav_track)
                .build();

        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.bottom_fragment);
        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(navigationView, navController);

        //change text of bottom sheet
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                Log.i("navcontroller", String.valueOf(destination.getId()));
                if (peekText.getText().equals("Track")) peekText.setText("Create");
                else peekText.setText("Track");
            }
        });
        //navigationView.setOnNavigationItemSelectedListener(this);

        return root;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    public void setStartingPoint() {
        controller.setStartListener(new GoogleMapController.StartListener() {
            @Override
            public void onChange() {
                setStartPoint = controller.isSetStartPoint();
                userRoute = controller.getUserRouteEntity();
                if (setStartPoint) {
                    for (Marker marker : accessPoint) {
                        marker.setVisible(true);
                    }
                }
                else {
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
                userRoute = controller.getUserRouteEntity();
                if (setEndPoint) {
                    for (Marker marker : accessPoint) {
                        marker.setVisible(true);
                    }
                }
                else {
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
                        userRoute = controller.getUserRouteEntity();
                        createRoute = true;
                        controller.getDirections(startPoint.getPosition(), endPoint.getPosition());
                        routeDone();
                        controller.setMessage("Route created");

                    }
                }
                else {
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
                routeLine.add(mMap.addPolyline(new PolylineOptions().addAll(route).width(10.0f).color(Color.GREEN)));

            }
        });
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        setStartingPoint();
        setEndingPoint();
        createRoute();

        userRoute = controller.getUserRouteEntity();
        if (userRoute.getStartPointName() != null) {
            startPoint = mMap.addMarker(new MarkerOptions().position(userRoute.getStartPoint())
                    .title(userRoute.getStartPointName())
                    .snippet("Your Starting Point")
                    .zIndex(2f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
        if (userRoute.getEndPointName() != null) {
            endPoint = mMap.addMarker(new MarkerOptions().position(userRoute.getEndPoint())
                    .title(userRoute.getEndPointName())
                    .snippet("Your Ending Point")
                    .zIndex(2f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
        if (!controller.getRoute().isEmpty()) {
            Toast.makeText(getActivity(), "Inside", Toast.LENGTH_SHORT).show();
            controller.create(userRoute);
            //ArrayList<LatLng> route = controller.getRoute();
            //routeLine.add(mMap.addPolyline(new PolylineOptions().addAll(route).width(10.0f).color(Color.GREEN)));
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                //Fragment fragment = mSectionsStatePagerAdapter.getItem(2);
                if (setStartPoint) {
                    if (!marker.getTitle().equals("Your Location"))
                        if (endPoint == null || !marker.getTitle().equals("Your Ending Location")) {
                            if (startPoint != null)
                                startPoint.remove();
                            startPoint = mMap.addMarker(new MarkerOptions().position(marker.getPosition())
                                    .title(marker.getTitle())
                                    .snippet("Your Starting Point")
                                    .zIndex(2f)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            UserRouteController.setStartMarkerInfo(userRoute, startPoint);
                            //((MapsCreateFragment) getActivity().getSupportFragmentManager().findFragmentById(fragment.getId())).displayStartEndText();
                        }
                } else if (setEndPoint) {
                    if (!marker.getTitle().equals("Your Location"))
                        if (startPoint == null || !marker.getTitle().equals(startPoint.getTitle())) {
                            if (endPoint != null)
                                endPoint.remove();
                            endPoint = mMap.addMarker(new MarkerOptions().position(marker.getPosition())
                                    .title(marker.getTitle())
                                    .snippet("Your Ending Location")
                                    .zIndex(1f)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            UserRouteController.setEndMarkerInfo(userRoute, endPoint);
                            //((MapsCreateFragment) getActivity().getSupportFragmentManager().findFragmentById(fragment.getId())).displayStartEndText();
                        }
                }
                return false;
            }
        });

        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {

            //to prevent camera to move back to user location every second
            boolean isFirstTime = true;

            //method to move location according to user's position
            @Override
            public void onLocationChanged(Location location) {

                if (isFirstTime) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, ZOOM));
                    isFirstTime = false;
                }

                if (userLocation != null)
                    lastLocation = userLocation;

                //update current position of user
                userLocation = new LatLng(location.getLatitude(), location.getLongitude());

/*                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }*/
                //Location prevLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                //lastLocation = new LatLng(prevLocation.getLatitude(), prevLocation.getLongitude());
                startTrack = controller.isStartTrack();
                if (startTrack) {
                    userLocationSession = controller.getUserLocationSession();
                    locations.add(lastLocation);
                    UserLocationEntity userLocation = new UserLocationEntity();
                    UserLocationController.addUserLocation(userLocationSession, lastLocation, Calendar.getInstance().getTime());
                    UserLocationController.updateUserLocation(userLocationSession);

                }
                    /*Log.i("LAST LOCATION", lastLocation.toString());
                    for (LatLng loc: locations) {
                        Log.i("LOCATION", loc.toString());
                    }*/

                if (userMarker != null) {
                    userMarker.remove();
                    userMarker = mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                    if (startTrack) {
                        mMap.addPolyline(new PolylineOptions().addAll(locations).width(10.0f).color(Color.RED));
                        //mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
                    }
                } else {
                    userMarker = mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                }


                //mMap.addPolyline(new PolylineOptions().add(new LatLng(lastLocation.latitude, lastLocation.longitude),
                //        new LatLng(userLocation.latitude, userLocation.longitude)).width(Float.valueOf("10.0")).color(Color.BLACK));

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
            parklayerjson = new GeoJsonLayer(mMap, R.raw.parkconnectorloopg, getContext());
            accesslayerjson = new GeoJsonLayer(mMap, R.raw.accesspointsg, getContext());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            parklayer = new KmlLayer(mMap, R.raw.parkconnectorloop, getContext());
            accesslayer = new KmlLayer(mMap, R.raw.accesspoints, getContext());
            //parklayer.addLayerToMap();


        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (GeoJsonFeature feature : accesslayerjson.getFeatures()) {
            if ("Point".equalsIgnoreCase(feature.getGeometry().getGeometryType())) {
                GeoJsonPoint point = (GeoJsonPoint) feature.getGeometry();

                LatLng latLng = point.getCoordinates();

                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(feature.getProperty("Name")).visible(false));
                accessPoint.add(marker);
            }
        }

/*        ArrayList<ArrayList<LatLng>> pathLines = new ArrayList();
        for (GeoJsonFeature feature : parklayerjson.getFeatures()) {
            if("LineString".equalsIgnoreCase(feature.getGeometry().getGeometryType())) {
                GeoJsonLineString line = (GeoJsonLineString) feature.getGeometry();
                ArrayList<LatLng> latLngList = (ArrayList)line.getCoordinates();
                pathLines.add(latLngList);
            }
        }

        for (ArrayList <LatLng> latLngList: pathLines) {
            mMap.addPolyline(new PolylineOptions().addAll(latLngList));
        }*/


        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINTIME, MINDIST, locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            //mMap.clear();
            userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

    }

    /*@Override
    public void onStart() {
        super.onStart();
        userRoute = controller.getUserRouteEntity();
        if (userRoute.getStartPointName() != null) {
            startPoint = mMap.addMarker(new MarkerOptions().position(userRoute.getStartPoint())
                    .title(userRoute.getStartPointName())
                    .snippet("Your Starting Point")
                    .zIndex(2f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
        if (userRoute.getEndPointName() != null) {
            endPoint = mMap.addMarker(new MarkerOptions().position(userRoute.getEndPoint())
                    .title(userRoute.getEndPointName())
                    .snippet("Your Ending Point")
                    .zIndex(2f)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        }
        if (!controller.getRoute().isEmpty())
                routeLine.add(mMap.addPolyline(new PolylineOptions().addAll(controller.getRoute()).width(10.0f).color(Color.GREEN)));
    }*/

/*    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.nav_create) {
            peekText.setText(R.string.menu_create);

            return true;
        }
        else if (item.getItemId() == R.id.nav_track) {
            peekText.setText(R.string.menu_track);
            return true;
        }
        return true;
    }*/

}

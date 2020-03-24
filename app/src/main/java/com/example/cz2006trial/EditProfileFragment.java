package com.example.cz2006trial;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;
import androidx.navigation.Navigation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.DOWNLOAD_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

/**
 * This fragment is used to update user profile information to Firebase.
 */
public class EditProfileFragment extends Fragment {

    private static final int SELECT_FILE = 2;
    private static final int REQUEST_CAMERA = 1;

    ImageView profilePhoto;
    TextView usernameTextView;
    TextView emailTextView;
    EditText DOBTextView;
    EditText heightTextView;
    EditText weightTextView;
    EditText BMITextView;
    Button updateProfileButton;
    private boolean photoChange = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_edit_profile, container, false);

        profilePhoto = view.findViewById(R.id.profile);
        usernameTextView = view.findViewById(R.id.username);
        emailTextView = view.findViewById(R.id.email);
        DOBTextView = view.findViewById(R.id.DOB);
        heightTextView = view.findViewById(R.id.height);
        weightTextView = view.findViewById(R.id.weight);
        BMITextView = view.findViewById(R.id.BMI);
        updateProfileButton = view.findViewById(R.id.updateProfileButton);

        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    Toast.makeText(getContext(), "User Profile Updated", Toast.LENGTH_LONG).show();
                    getActivity().onBackPressed();
                }
            }
        };

        getActivity().registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        final Calendar myCalendar = Calendar.getInstance();

        final DatePickerDialog.OnDateSetListener dob = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                String myFormat = "dd/MM/yyyy"; //In which you need put here
                SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
                DOBTextView.setText(sdf.format(myCalendar.getTime()));
            }

        };

        DOBTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new DatePickerDialog(getContext(), dob, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }

        });


        heightTextView.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3, 1)});
        weightTextView.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3, 1)});
        BMITextView.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3, 1)});


        profilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission())
                    editPhoto();
                else
                    requestPermission();
            }
        });

        displayProfileFromDatabase();

        updateProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile();
                if (photoChange && hasPhoto(profilePhoto)) {
                    ImageDatabaseManager.imageDatabase(new ImageDatabaseManager.ImageCallback() {
                        @Override
                        public void onCallback(String[] message) {
                            DownloadFileManager.downloadFile(getContext(), "profilePhoto", ".jpg", Environment.DIRECTORY_DOWNLOADS, message[0]);
                            Toast.makeText(getContext(), message[0], Toast.LENGTH_SHORT).show();
                        }
                    }, "update", profilePhoto);
                } else if (photoChange && !hasPhoto(profilePhoto)) {
                    ImageDatabaseManager.imageDatabase(new ImageDatabaseManager.ImageCallback() {
                        @Override
                        public void onCallback(String[] message) {
                            Toast.makeText(getContext(), message[0], Toast.LENGTH_SHORT).show();
                        }
                    }, "delete", profilePhoto);
                    Toast.makeText(getContext(), "User Profile Updated", Toast.LENGTH_LONG).show();
                    getActivity().onBackPressed();
                } else {
                    Toast.makeText(getContext(), "User Profile Updated", Toast.LENGTH_LONG).show();
                    getActivity().onBackPressed();
                }

            }
        });
    }

/*public class EditProfileActivity extends AppCompatActivity {

    private static final int SELECT_FILE = 2;
    private static final int REQUEST_CAMERA = 1;

    ImageView profilePhoto;
    TextView usernameTextView;
    TextView emailTextView;
    EditText DOBTextView;
    EditText heightTextView;
    EditText weightTextView;
    EditText BMITextView;
    Button updateProfileButton;
    private boolean photoChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        profilePhoto = findViewById(R.id.profile);
        usernameTextView = findViewById(R.id.username);
        emailTextView = findViewById(R.id.email);
        DOBTextView = findViewById(R.id.DOB);
        heightTextView = findViewById(R.id.height);
        weightTextView = findViewById(R.id.weight);
        BMITextView = findViewById(R.id.BMI);
        updateProfileButton = findViewById(R.id.updateProfileButton);

        final Calendar myCalendar = Calendar.getInstance();

        final DatePickerDialog.OnDateSetListener dob = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                String myFormat = "dd/MM/yyyy"; //In which you need put here
                SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
                DOBTextView.setText(sdf.format(myCalendar.getTime()));
            }

        };

        DOBTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new DatePickerDialog(EditProfileActivity.this, dob, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }

        });



        heightTextView.setFilters(new InputFilter[] {new DecimalDigitsInputFilter(3,1)});
        weightTextView.setFilters(new InputFilter[] {new DecimalDigitsInputFilter(3,1)});
        BMITextView.setFilters(new InputFilter[] {new DecimalDigitsInputFilter(3,1)});


        profilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission())
                    editPhoto();
                else
                    requestPermission();
            }
        });

        displayProfileFromDatabase();

        updateProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfile();
                if (photoChange && hasPhoto(profilePhoto)) {
                    ImageDatabaseManager.imageDatabase(new ImageDatabaseManager.ImageCallback() {
                        @Override
                        public void onCallback(String[] message) {
                            DownloadFileManager.downloadFile(EditProfileActivity.this, "profilePhoto", ".jpg", Environment.DIRECTORY_DOWNLOADS, message[0]);
                            Toast.makeText(getApplicationContext(), message[0], Toast.LENGTH_SHORT).show();
                        }
                    },"update", profilePhoto);
                }
                else if (photoChange && !hasPhoto(profilePhoto)) {
                    ImageDatabaseManager.imageDatabase(new ImageDatabaseManager.ImageCallback() {
                        @Override
                        public void onCallback(String[] message) {
                            Toast.makeText(getApplicationContext(), message[0], Toast.LENGTH_SHORT).show();
                        }
                    },"delete", profilePhoto);
                }
                Toast.makeText(getApplicationContext(), "User Profile Updated", Toast.LENGTH_LONG).show();
                finish();
            }
        });

    }

 */

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {

            return true;

        } else {

            return false;

        }
    }

    private void requestPermission() {

        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            Toast.makeText(getContext(), "Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();

        } else {

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(getContext(), "Permission Granted, Now you can edit profile photo.", Toast.LENGTH_LONG).show();
                } else {

                    Toast.makeText(getContext(), "Permission Denied, You cannot edit profile photo.", Toast.LENGTH_LONG).show();

                }
                break;
        }
    }

    public void updateProfile() {
        String username = usernameTextView.getText().toString();
        String email = emailTextView.getText().toString();
        String DOB = null;
        double height = 0;
        double weight = 0;
        double BMI = 0;

        if (!DOBTextView.getEditableText().toString().equals(""))
            DOB = DOBTextView.getEditableText().toString();

        if (!heightTextView.getEditableText().toString().equals(""))
            height = Double.parseDouble(heightTextView.getEditableText().toString());

        if (!weightTextView.getEditableText().toString().equals(""))
            weight = Double.parseDouble(weightTextView.getEditableText().toString());

        if (!BMITextView.getEditableText().toString().equals(""))
            BMI = Double.parseDouble(BMITextView.getEditableText().toString());

        UserProfileController.setEditedUserProfileOnDatabase(username, email, DOB, height, weight, BMI);
    }

    public void displayProfileFromDatabase() {
        String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseUserProfile = FirebaseDatabase.getInstance().getReference(UID).child("userProfile");
        databaseUserProfile.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final UserProfileEntity userProfile = dataSnapshot.getValue(UserProfileEntity.class);
                if (userProfile != null) {
                    usernameTextView.setText(userProfile.getUsername());
                    emailTextView.setText(userProfile.getEmail());

                    if (userProfile.getDOB() != null)
                        DOBTextView.setText(userProfile.getDOB());
                    DOBTextView.setHint("Please input your date of birth");

                    if (userProfile.getHeight() != 0)
                        heightTextView.setText("" + userProfile.getHeight());
                    heightTextView.setHint("Please input your height in cm");

                    if (userProfile.getWeight() != 0)
                        weightTextView.setText("" + userProfile.getWeight());
                    weightTextView.setHint("Please input your weight in kg");

                    if (userProfile.getBMI() != 0)
                        BMITextView.setText("" + userProfile.getBMI());
                    BMITextView.setHint("Please input your BMI");
                    ImageDatabaseManager.imageDatabase(new ImageDatabaseManager.ImageCallback() {
                        @Override
                        public void onCallback(String[] message) {
                            Toast.makeText(getContext(), message[0], Toast.LENGTH_SHORT).show();
                        }
                    }, "retrieve", profilePhoto);

                } else {
                    Toast.makeText(getContext(),
                            "Something went wrong. PLease re-login and try again", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    /**
     * This function checks if the image resource currently attached to an ImageView is the same as
     * that set in the XML.
     *
     * @param profilePhoto The ImageView whose contents are to be checked
     * @return returns true if the image resource inside the ImageView was replaced with another;
     * else returns false
     */
    private boolean hasPhoto(ImageView profilePhoto) {
        boolean result = true;
        Drawable.ConstantState constantState;
        constantState = getResources()
                .getDrawable(R.mipmap.ic_profile_pic, getContext().getTheme())
                .getConstantState();

        if (profilePhoto.getDrawable().getConstantState() == constantState) {
            result = false;
        }

        return result;
    }


    public void editPhoto() {

        //The list of edit photo options. Have to use hardcoded string since this is a
        //CharSequence[] and R.string values can't be used since they are int indexes
        final CharSequence[] items = {"Take Photo", "Choose from Library", "Remove Photo"};

        //creating the alert dialog to show when the edit photo button is clicked
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Warning: Any changes will delete current photo permanently when updating");
        builder.setItems(
                //if the profile photo is set, show the option to remove the photo
                //else show only the first two options.
                (hasPhoto(this.profilePhoto) ? items : Arrays.copyOfRange(items, 0, 2)),
                new DialogInterface.OnClickListener() {

                    /**
                     * OnClick handler for each of the menu items
                     * @param dialog the menu from which the user selected an item
                     * @param item the menu item that the user clicked on
                     */
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (items[item].equals("Take Photo")) {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            startActivityForResult(intent, REQUEST_CAMERA);
                        } else if (items[item].equals("Choose from Library")) {
                            Intent intent = new Intent(
                                    Intent.ACTION_PICK,
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            intent.setType("image/*");
                            startActivityForResult(
                                    Intent.createChooser(intent, "Select File"),
                                    SELECT_FILE);
                        } else if (items[item].equals("Remove Photo")) {
                            profilePhoto.setImageResource(R.mipmap.ic_profile_pic);
                            photoChange = true;
                            dialog.dismiss(); //dismiss the dialog when an option is selected
                        }
                    }
                });
        builder.show(); //finally, show this dialog upon button click
    }

    /**
     * The function that is called when the user returns to EditProfile after having selected a
     * file or taken a photo from the camera app.
     *
     * @param requestCode To determine if the user has returned from the camera app or selected a file
     * @param resultCode  success or error
     * @param data        data received from the activity
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /**if a bitmap was not fetched successfully, there's no point in attempting to
         * parse the data.
         */
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                //if the user took a photo using the camera, save that file to external storage and
                //set that image to the profilePhoto ImageView
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                if (thumbnail != null) {
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
                    /*File destination = new File(Environment.getExternalStorageDirectory(),
                            System.currentTimeMillis() + ".jpg");
                    FileOutputStream fo;
                    try {
                        destination.createNewFile();
                        fo = new FileOutputStream(destination);
                        fo.write(bytes.toByteArray());
                        fo.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/
                    profilePhoto.setImageBitmap(thumbnail);
                    photoChange = true;
                } else {
                    Toast.makeText(getContext(), "Image capture error", Toast.LENGTH_SHORT).show();
                }

            } else if (requestCode == SELECT_FILE) {
                /**
                 * if user chose to upload a file from external storage, set that image to the
                 * profile photo ImageView.
                 */
                Uri selectedImageUri = data.getData();
                String[] projection = {MediaStore.MediaColumns.DATA};
                CursorLoader cursorLoader = new CursorLoader(getContext(), selectedImageUri, projection, null, null, null);
                Cursor cursor = cursorLoader.loadInBackground();
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                cursor.moveToFirst();
                String selectedImagePath = cursor.getString(column_index);
                Bitmap bm;
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(selectedImagePath, options);
                final int REQUIRED_SIZE = 200;
                int scale = 1;
                while (options.outWidth / scale / 2 >= REQUIRED_SIZE
                        && options.outHeight / scale / 2 >= REQUIRED_SIZE)
                    scale *= 2;
                options.inSampleSize = scale;
                options.inJustDecodeBounds = false;
                bm = BitmapFactory.decodeFile(selectedImagePath, options);
                profilePhoto.setImageBitmap(bm);
                photoChange = true;
            }
        }
    }
}
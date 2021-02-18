package com.suganth.firebaseuploadapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class MainActivity<handler> extends AppCompatActivity {

    public static final int PICK_IMAGE_REQUEST = 1;

    private Button mButtonChoose;
    private Button mButtonUpload;
    private EditText mImageName;
    private TextView mShowUploads;
    private ImageView mImageView;
    private ProgressBar mProgressBar;

    private Uri mImageUri;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private StorageTask mUploadTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init()
    {
        mButtonChoose = findViewById(R.id.chooseFileBtn);
        mButtonUpload = findViewById(R.id.uploadBtn);
        mImageName = findViewById(R.id.editFileName);
        mShowUploads = findViewById(R.id.showUploads);
        mImageView = findViewById(R.id.uploadImageView);
        mProgressBar = findViewById(R.id.progressBar);

        //we are givinge reference to a folder where a image could store in firebase
        storageReference = FirebaseStorage.getInstance().getReference("uploads");
        databaseReference = FirebaseDatabase.getInstance().getReference("uploads");

        mButtonChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImageChooser();
            }
        });

        mShowUploads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImagesActivity();
            }
        });

        mButtonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mUploadTask!=null && mUploadTask.isInProgress())
                {
                    Toast.makeText(MainActivity.this,"file uploading is in progress",Toast.LENGTH_LONG).show();
                }else {
                    uploadFiles();
                }
            }
        });
        }

        private void openImageChooser()
        {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            //to determine the image upload, using request code
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null)
        {
            mImageUri = data.getData();
            //after pick a image , get a image from data(local storage) and set in image view
            Picasso.with(this).load(mImageUri).into(mImageView);
        }
    }

    //used to get extension of a file type, which we uploaded.
    private String getFileTypeExtension (Uri uri)
    {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadFiles()
    {
        if(mImageUri != null)
        {
            //it refers to an image which is inside upload folder in firbase , and give the name of the image as ex: 2021021003523101.jpg
            StorageReference fileReference = storageReference.child(System.currentTimeMillis()+"."+getFileTypeExtension(mImageUri));
            mUploadTask = fileReference.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                          Handler handler = new Handler(Looper.getMainLooper());
                          handler.postDelayed(new Runnable() {
                                @Override
                                public void run()
                                {
                                    mProgressBar.setProgress(0);
                                }
                            },1000);
                            Toast.makeText(MainActivity.this, "Successfully Uploaded", Toast.LENGTH_LONG).show();
                            //it uploads to create a new entry in a database
                            Upload upload = new Upload(mImageName.getText().toString().trim(),taskSnapshot.getStorage().getDownloadUrl().toString());
                            //it create a id foe an new entry, using that id , we set value to an upload for an entry.
                            String uploadId = databaseReference.push().getKey();
                            databaseReference.child(uploadId).setValue(upload);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    mProgressBar.setProgress((int)progress);
                }
            });
        }
        else
        {
            Toast.makeText(this,"No file Selected", Toast.LENGTH_LONG);
        }
    }

    private void openImagesActivity ()
    {
        Intent intent = new Intent(this, ImagesActivity.class);
        startActivity(intent);
    }
}


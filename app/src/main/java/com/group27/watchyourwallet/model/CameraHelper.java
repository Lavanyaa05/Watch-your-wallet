package com.group27.watchyourwallet.model;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.FileProvider;

import java.io.File;

public class CameraHelper {

    private Context context;
    private Uri photoUri;

    public CameraHelper(Context context) {
        this.context = context;
    }

    public void launchCamera(ActivityResultLauncher<Intent> cameraLauncher) {
        File photoFile = new File(context.getExternalFilesDir(null), "receipt.jpg");
        photoUri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", photoFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        cameraLauncher.launch(intent);
    }

    public Uri getPhotoUri() {
        return photoUri;
    }
}
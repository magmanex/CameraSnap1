package app.akexorcist.camerasnap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;

public class Main extends Activity implements SurfaceHolder.Callback
		, Camera.ShutterCallback, Camera.PictureCallback {
    Camera mCamera;
    SurfaceView mPreview;
	boolean saveState = false;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN 
        		| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.main);
		
        mPreview = (SurfaceView)findViewById(R.id.preview);
        mPreview.getHolder().addCallback(this);
        mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mPreview.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				if(!saveState) {
					saveState = true;
					mCamera.takePicture(Main.this, null, null, Main.this);
				}
			}        	
        });
	}
    
	public void onResume() {
    	Log.d("System","onResume");
        super.onResume();
        mCamera = Camera.open();
    }
	
	public void onPause() {
    	Log.d("System","onPause");
        super.onPause();
        mCamera.release();
    }

	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		Log.d("CameraSystem","surfaceChanged");
		Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> previewSize = params.getSupportedPreviewSizes();
        List<Camera.Size> pictureSize = params.getSupportedPictureSizes();
        params.setPictureSize(pictureSize.get(0).width, pictureSize.get(0).height);
        params.setPreviewSize(previewSize.get(0).width, previewSize.get(0).height);
        params.setJpegQuality(100);
        mCamera.setParameters(params);
        
        try {
            mCamera.setPreviewDisplay(mPreview.getHolder());
            mCamera.startPreview();
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}

	public void surfaceCreated(SurfaceHolder arg0) { }

	public void surfaceDestroyed(SurfaceHolder arg0) { }

	public void onPictureTaken(byte[] arg0, Camera arg1) { 
    	int imageNum = 0;
        Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imagesFolder = new File(Environment.getExternalStorageDirectory()
        		, "DCIM/CameraSnap");
        imagesFolder.mkdirs();
        String fileName = "IMG_" + String.valueOf(imageNum) + ".jpg";
        File output = new File(imagesFolder, fileName);
        
        while (output.exists()){
            imageNum++;
            fileName = "IMG_" + String.valueOf(imageNum) + ".jpg";
            output = new File(imagesFolder, fileName);
        }

        Uri uri = Uri.fromFile(output);
        imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        
        ContentValues image = new ContentValues();
        String dateTaken = DateFormat.getDateTimeInstance()
        		.format(Calendar.getInstance().getTime());
        image.put(Images.Media.TITLE, output.toString());
        image.put(Images.Media.DISPLAY_NAME, output.toString());
        image.put(Images.Media.DATE_ADDED, dateTaken);
        image.put(Images.Media.DATE_TAKEN, dateTaken);
        image.put(Images.Media.DATE_MODIFIED, dateTaken);
        image.put(Images.Media.MIME_TYPE, "image/jpg");
        image.put(Images.Media.ORIENTATION, 0);
        String path =  output.getParentFile().toString().toLowerCase();
        String name =  output.getParentFile().getName().toLowerCase();
        image.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
        image.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
        image.put(Images.Media.SIZE, output.length());
        image.put(Images.Media.DATA, output.getAbsolutePath());
        
        OutputStream os;
        
        try {
        	os = getContentResolver().openOutputStream(uri);
        	os.write(arg0);
        	os.flush();
        	os.close();
            Toast.makeText(Main.this, fileName, Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
        } catch (IOException e) { }

        Log.d("Camera","Restart Preview");	
        mCamera.stopPreview();
        mCamera.startPreview();
        saveState = false;
	}

	public void onShutter() { }
}

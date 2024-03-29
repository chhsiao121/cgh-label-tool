package com.example.lable_2;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "com.example.lable2.MESSAGE";
    private static final String REQUIRED = "Required";
    private static final int PICKFILE_RESULT_CODE = 1;
    private StorageReference mStorageRef;
    private String PATH;
    private Boolean f_load = Boolean.FALSE;
    private Button buttonUpload;
    public ArrayList<String> testType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        testType = new ArrayList<>();
        testType.add("stopping");
        testType.add("backing");
        testType.add("affricate");
        testType.add("fricative");
        int READ_EXTERNAL_STORAGE = 100;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);


        mStorageRef = FirebaseStorage.getInstance().getReference();
        Button buttonOpen = findViewById(R.id.buttonOpen);
        buttonUpload = findViewById(R.id.buttonUploadn);
        Button buttonLabel = findViewById(R.id.buttonLabel);
        Button buttonExit = findViewById(R.id.buttonExit);
        buttonOpen.setOnClickListener(v -> openFileChooser());
        buttonLabel.setOnClickListener(this::startLabel);
        buttonUpload.setOnClickListener(v -> {
            if (networkIsConnect()) uploadFile();

            else Toast.makeText(getApplicationContext(), "請開啟網路，再重新上傳數據", Toast.LENGTH_SHORT).show();
        });
        buttonExit.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });


    }

    private boolean networkIsConnect(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null){
            return networkInfo.isConnected();
        }else {
            return false;
        }
    }

    public void uploadFile() {
        EditText editUsername = findViewById(R.id.userName);
        String userName = editUsername.getText().toString();
        if (TextUtils.isEmpty(userName)) {
            editUsername.setError(REQUIRED);
        } else {
            userName = userName.replaceAll("\\s", "");
            if(f_load) {
                String json_name = PATH.split("/")[PATH.split("/").length - 1] + ".json";
                File upload_file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), json_name);
                if (upload_file.exists()) {
                    buttonUpload.setEnabled(false);
                    String uploadName = userName + "_" + json_name;
                    Uri jsonUri = Uri.fromFile(upload_file);
                    Log.e("save_name", uploadName);
                    uploadjson(jsonUri, uploadName, json_name);
                } else {
                    Toast.makeText(getApplicationContext(), "請先標記音檔並儲存", Toast.LENGTH_LONG).show();
                }
            }
            else{
                Button buttonOpen = findViewById(R.id.buttonOpen);
                buttonOpen.setError(REQUIRED);
                Toast.makeText(getApplicationContext(), "請先選取音檔資料夾", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void uploadjson(Uri jsonUri, String uploadName, String json_name){
        int uplodaIdx = uploadName.split("_").length - 1;
        String uploadTestType = uploadName.split("_")[uplodaIdx - 1];
        StorageReference Ref;
        if(testType.contains(uploadTestType)){
            Ref = mStorageRef.child("test/"+uploadName);
        }
        else{
            Ref = mStorageRef.child(uploadName);
        }


        UploadTask uploadTask = Ref.putFile(jsonUri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                buttonUpload.setEnabled(true);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(json_name + "上傳失敗，請重新點選上傳按鈕進行上傳");
                builder.setTitle("上傳結果");
                builder.setPositiveButton("收到", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), json_name + "如果持續上傳失敗請聯絡開發者:m11002129@mail.ntust.edu.tw", Toast.LENGTH_LONG).show();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                // ...
                buttonUpload.setEnabled(true);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(uploadName + "已上傳成功");
                builder.setTitle("上傳結果");
                builder.setPositiveButton("收到", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), uploadName + "上傳成功", Toast.LENGTH_LONG).show();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }


    public void startLabel(View view) {
        if (f_load) {

            Intent intent = new Intent(this, LabelActivity.class);
            intent.putExtra(EXTRA_MESSAGE, PATH);
            startActivity(intent);
        } else {
            Button buttonOpen = findViewById(R.id.buttonOpen);
            buttonOpen.setError(REQUIRED);
            Toast.makeText(getApplicationContext(), "請先選取音檔資料夾", Toast.LENGTH_LONG).show();
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, PICKFILE_RESULT_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICKFILE_RESULT_CODE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            PATH = data.getData().getPath();
//            Log.e("test", "PATH"+PATH);
            String[] bits = PATH.split("/");
            PATH = PATH.substring(0, PATH.length() - bits[bits.length - 1].length());
//            Log.e("test", "PATH"+PATH);
            PATH = "/" + PATH.split(":")[1];
//            Log.e("test", "PATH"+PATH);
            setText("已選取音檔路徑: " + PATH);
            f_load = Boolean.TRUE;
        }
    }

    public void setText(String text) {
        TextView textView = findViewById(R.id.textPath);
        textView.setText(text);
    }


}
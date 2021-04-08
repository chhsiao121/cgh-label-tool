package com.example.lable_2;

import android.content.Intent;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "com.example.lable2.MESSAGE";
    private static final String REQUIRED = "Required";
    private static final int PICKFILE_RESULT_CODE = 1;
    private StorageReference mStorageRef;
    private String PATH;
    private Boolean f_load = Boolean.FALSE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        Button buttonOpen = (Button) findViewById(R.id.buttonOpen);
        Button buttonUpload = (Button) findViewById(R.id.buttonUploadn);
        Button buttonLabel = (Button) findViewById(R.id.buttonLabel);
        buttonOpen.setOnClickListener(v -> openFileChooser());
        buttonLabel.setOnClickListener(this::startLabel);
        buttonUpload.setOnClickListener(v -> uploadFile());
        TextView versionName = findViewById(R.id.textAPPVersion);
        versionName.setText("version : " + BuildConfig.VERSION_NAME);
    }

    public void uploadFile() {
        EditText userName = (EditText) findViewById(R.id.userName);
        final String userNmae = userName.getText().toString();
        if (TextUtils.isEmpty(userNmae)) {
            userName.setError(REQUIRED);
        } else {
            File path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (path != null) {
                File[] files = path.listFiles();
                for (File f : files) {
//                    Log.e("TAG", f.toString());
                    Uri jsonUri = Uri.fromFile(f);
                    String fileName = jsonUri.toString().split("/")[jsonUri.toString().split("/").length - 1];
//                    Log.e("name", fileName);
                    final String uploadName=System.currentTimeMillis() + "." + userName.getText() + "." + fileName;
                    Log.e("save_name", uploadName);
                    StorageReference Ref = mStorageRef.child(uploadName);
                    Ref.putFile(jsonUri)
                            .addOnSuccessListener(taskSnapshot -> {
//                                Log.e("TAG", "Upload succesFully");
                                Toast.makeText(getApplicationContext(), uploadName +"上傳成功", Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(exception -> Toast.makeText(getApplicationContext(), "保存失敗Q", Toast.LENGTH_LONG).show());
                }
            }
        }
    }


    public void startLabel(View view) {
        if (f_load) {
            Intent intent = new Intent(this, LabelActivity.class);
            intent.putExtra(EXTRA_MESSAGE, PATH);
            startActivity(intent);
        } else {
            Button buttonOpen = (Button) findViewById(R.id.buttonOpen);
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
            String[] bits = PATH.split("/");
            PATH = PATH.substring(0, PATH.length() - bits[bits.length - 1].length());
            PATH = "/" + PATH.split(":")[1];
            setText("已選取音檔路徑: " + PATH);
            f_load = Boolean.TRUE;
        }
    }

    public void setText(String text) {
        TextView textView = (TextView) findViewById(R.id.textPath);
        textView.setText(text);
    }

}
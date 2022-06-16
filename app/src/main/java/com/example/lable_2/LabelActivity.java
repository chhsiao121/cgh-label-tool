package com.example.lable_2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.io.ByteStreams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LabelActivity extends AppCompatActivity {

    public String target_name;
    public JSONObject jsonData = new JSONObject();

    public String folder_path;
    public File[] files;
    public int select_file = 0;
    public int select_file_max = 0;
    public boolean f_first_play = true;
    public MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label);
        mediaPlayer = new MediaPlayer();
        FloatingActionButton buttonSave = findViewById(R.id.buttonSave);
        FloatingActionButton buttonPlay = findViewById(R.id.buttonPlay);
        FloatingActionButton buttonNext = findViewById(R.id.buttonNext);
        FloatingActionButton buttonPrevious = findViewById(R.id.buttonPrevious);
        FloatingActionButton buttonReset = findViewById(R.id.buttonReset);
        EditText editIndex = findViewById(R.id.editIndex);
        ChipGroup selectGroup = findViewById(R.id.selectGroup);

        Intent intent = getIntent();
        folder_path = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);


        loadFileList();
        playAudio();
        update_text_selectGroup();


        buttonPlay.setOnClickListener(v -> playAudio());

        buttonReset.setOnClickListener(v -> selectGroup.clearCheck()); //清除所有ChipGroup的有選取的選項

        buttonNext.setOnClickListener(v -> {
            saveSelect2json();
            if (!TextUtils.isEmpty(editIndex.getText())) {
                select_file = Integer.parseInt(editIndex.getText().toString()) - 1;
                if (select_file > select_file_max)
                    select_file = select_file_max;
                else if (select_file < 1)
                    select_file = 0;
            } else {
                if (select_file < select_file_max)
                    select_file = select_file + 1;
            }

            update_text_selectGroup();
            playAudio();
        });

        buttonPrevious.setOnClickListener(v -> {
            saveSelect2json();
            if (!TextUtils.isEmpty(editIndex.getText())) {
                select_file = Integer.parseInt(editIndex.getText().toString()) - 1;
                if (select_file > select_file_max)
                    select_file = select_file_max;
                else if (select_file < 1)
                    select_file = 0;
            } else {
                if (select_file > 0)
                    select_file = select_file - 1;
            }

            update_text_selectGroup();
            playAudio();
        });

        buttonSave.setOnClickListener(v -> {
            saveSelect2json();
            saveJson2Phone(target_name, jsonData);
        });

    }

    protected void onStop() {
        super.onStop();
        if(mediaPlayer!=null){
            mediaPlayer.release();
            mediaPlayer = null;
        }

    }

    public void playAudio() {
        if (f_first_play)
            if(this.mediaPlayer!=null){
                this.mediaPlayer.release();
            }
        else
            f_first_play = false;
        this.mediaPlayer = new MediaPlayer();
        Uri myUri = Uri.fromFile(files[select_file]); // initialize Uri here
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        try {
            mediaPlayer.setDataSource(getApplicationContext(), myUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }






    public void update_text_selectGroup() { //靠select_file設定label介面上的音檔名稱、字卡名稱、第幾個音檔，並更新ChipGroup裡的label
        ChipGroup selectGroup = findViewById(R.id.selectGroup);
        TextView textFileName = findViewById(R.id.textFileName);
        TextView textFileNumber = findViewById(R.id.textFileNumber);
        TextView textClassName = findViewById(R.id.textClassName);
        EditText editIndex = findViewById(R.id.editIndex);
        editIndex.setText(null);
        selectGroup.clearCheck();
        String[] selectText;
        try {
            Object data = jsonData.get(files[select_file].getName());//jsonData.get("16_36_1_k83_1n.wav")會拿到"16_36_1_k83_1n.wav"所對應的label
            selectText = data.toString().split(",");
            for (int i = 0; i < selectGroup.getChildCount(); i++) {
                Chip chip = (Chip) selectGroup.getChildAt(i);
                for (String s : selectText) { //看selectGroup裡的chip是否與selectText裡的label相同，相同會將chip選取
                    if (chip.getText().equals(s)) {
                        chip.setChecked(true);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        textFileName.setText(files[select_file].getName());
        String nn = (select_file + 1) + " / " + (select_file_max + 1);
        textFileNumber.setText(nn);
        //這裡如果tmp所得到的字卡編號，不在res/values/strings.xml所宣告的字卡中，等等getStringResourceByName所拿到的resid會有問題
//        String tmp = "wordcard" + files[select_file].getName().split("_")[1] + "_" + files[select_file].getName().split("_")[2];
        String tmp = files[select_file].getName();
        tmp = tmp.substring(0,tmp.lastIndexOf("."));
        textClassName.setText(getStringResourceByName(tmp));
    }


    public void saveSelect2json() { //將此頁面上所對應的音檔與所選取的類別寫進jsonData，如果沒有錯誤類別則為""(jsonData尚未儲存成.json檔)

        ChipGroup selectGroup = findViewById(R.id.selectGroup);
        StringBuilder selectClass = new StringBuilder();
        List<Integer> ids = selectGroup.getCheckedChipIds();
        for (Integer id : ids) {
            Chip chip = selectGroup.findViewById(id);
            selectClass.append(chip.getText()).append(",");
        }
        try {
            jsonData.put(files[select_file].getName(), selectClass);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private String getStringResourceByName(String aString) {
        String packageName = getPackageName();
        int resId = getResources().getIdentifier(aString, "string", packageName);
        return getString(resId);
    }

    public void loadFileList() { //1.建立json檔(如果json檔不存在) 2.建立files陣列，裡面存了所有音檔的絕對路徑 3.呼叫update_text_selectGroup更新介面(並讀取其內容存在jsonData)
        int READ_EXTERNAL_STORAGE = 100;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);

        String path = Environment.getExternalStorageDirectory().toString() + folder_path;
        target_name = path.split("/")[path.split("/").length - 1] + ".json";
        if (!hasExternalStoragePrivateJson(target_name)) {
            createExternalStoragePrivateJson(target_name);
        } else {
            try {
                jsonData = new JSONObject(readJsonFromPhone(target_name));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        File directory = new File(path);
        files = directory.listFiles(new ImageFileFilter());
        Arrays.sort(files);
        List<File> files_list = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            /*It's assumed that all file in the path are in supported type*/
            String filePath = file.getPath();
            String fileName = file.getName();
            String fileTyle=fileName.substring(fileName.lastIndexOf("."));
            if (fileTyle.equals(".wav")) {
                files_list.add(file);
            }
        }
        files = files_list.toArray(new File[0]); //把ArrayList轉成File[](files_list.get(1) = files[1])

        if ((directory.canRead()) && (files != null) && (files.length != 0)) {
            select_file_max = files.length - 1;
            update_text_selectGroup();
        }else{
            Toast.makeText(this, "親，選擇資料夾裡面沒有.wav音檔喔，APP會壞掉", Toast.LENGTH_SHORT).show();
        }



    }

    void createExternalStoragePrivateJson(String filename) {
        File path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        AssetManager assetManager = getAssets();
        File file = new File(path, filename);

        try {
            InputStream is = assetManager.open("new.json");
            OutputStream os = new FileOutputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();

        } catch (IOException ignored) {
        }
    }

    boolean hasExternalStoragePrivateJson(String filename) {
        // Create a path where we will place our picture in the user's
        // public pictures directory and check if the file exists.  If
        // external storage is not currently mounted this will think the
        // picture doesn't exist.
        File path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (path != null) {
            File file = new File(path, filename);
            return file.exists();
        }
        return false;
    }

    public void saveJson2Phone(String filename, @NonNull JSONObject JsonObject) {
        String userString = JsonObject.toString();
        File path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(path, filename);
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(userString);
            bufferedWriter.close();
            Toast.makeText(getApplicationContext(), "保存成功", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readJsonFromPhone(String filename) {
        File path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(path, filename);
        String line = "";
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            line = new String(ByteStreams.toByteArray(fileInputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    public class ImageFileFilter implements FileFilter {
        private final String[] okFileExtensions = new String[] { "wav"};

        public boolean accept(File file) {
            for (String extension : okFileExtensions) {
                if (file.getName().toLowerCase().endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
    }

}

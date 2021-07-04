package com.hadiarajesh.assistant;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Rajesh on 12-04-2018.
 */

public class MainActivity extends AppCompatActivity implements RecognitionListener, TextToSpeech.OnInitListener{

    private static final String WOLFRAM_ALPHA_APP_ID = "YOUR_APP_ID";

    Toggle_Service ts=new Toggle_Service();

    Context context=this;
    SQLiteDatabase database;
    DatabaseHelper databaseHelper = new DatabaseHelper(context);

    ArrayList<String> appNameList=new ArrayList<>();
    ArrayList<String> packageNameList=new ArrayList<>();
    PackageInfo packageInfo;

    TextToSpeech tts ;
    ToggleButton toggleButton;
    TextView textView, textView1;
    ImageView img_loading;
    CardView cardView;
    SpeechRecognizer speechRecognizer=null;
    boolean permissionCheck;
    Intent recognizerIntent;
    String turn_on=".*turn o[nf].*";
    String navigate=".*(find|navigate to).*";
    String selfie=".*(capture|take).*(picture|photo).*";
    String call=".*call.*";
    String alarm=".*set alarm.*";
    String sms=".*(sms|message).*";
    String question="(wh|how).*";
    String music=".*play.*(music|song).*";
    String openapp=".*(open|launch|deploy).*";
    String response;
    Pattern pattern_turn_on=Pattern.compile(turn_on,Pattern.CASE_INSENSITIVE);
    Pattern pattern_navigate=Pattern.compile(navigate,Pattern.CASE_INSENSITIVE);
    Pattern pattern_selfie=Pattern.compile(selfie,Pattern.CASE_INSENSITIVE);
    Pattern pattern_call=Pattern.compile(call,Pattern.CASE_INSENSITIVE);
    Pattern pattern_sms=Pattern.compile(sms,Pattern.CASE_INSENSITIVE);
    Pattern pattern_alarm=Pattern.compile(alarm,Pattern.CASE_INSENSITIVE);
    Pattern pattern_question=Pattern.compile(question,Pattern.CASE_INSENSITIVE);
    Pattern pattern_openapp=Pattern.compile(openapp,Pattern.CASE_INSENSITIVE);
    Pattern pattern_music=Pattern.compile(music,Pattern.CASE_INSENSITIVE);
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences=getSharedPreferences("MyPreference",0);

        toggleButton=findViewById(R.id.toggleButton);
        textView=findViewById(R.id.textView);
        textView1=findViewById(R.id.textView1);
        cardView=findViewById(R.id.cardView);

        cardView.setVisibility(View.GONE);
        textView1.setVisibility(View.GONE);
        img_loading=findViewById(R.id.img_loading);

        if(sharedPreferences.getBoolean("first_time",true)) {
            permissionCheck=checkAndRequestPermissions();
            if(!permissionCheck) {
                checkAndRequestPermissions();
            }

            Handler handler=new Handler();
            handler.postDelayed(this::addContactDatabase,5000);

            sharedPreferences.edit().putBoolean("first_time",false).apply();
        }

        speechRecognizer=SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        recognizerIntent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,2);

        tts = new TextToSpeech(this,this);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    textView.setVisibility(View.INVISIBLE);
                    Glide.with(getApplicationContext()).load(R.drawable.loading).into(img_loading);
                    speechRecognizer.startListening(recognizerIntent);
                }
                else {
                    img_loading.setVisibility(View.INVISIBLE);
                    speechRecognizer.stopListening();
                    textView.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private  boolean checkAndRequestPermissions() {
        int camera = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        int storage = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int contacts = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        int call = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);
        int sms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);

        List<String> listPermissionsNeeded = new ArrayList<>();

        if (contacts != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }
        if (call != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CALL_PHONE);
        }
        if (camera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.CAMERA);
        }
        if (storage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (audio != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        if (sms != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (!listPermissionsNeeded.isEmpty())
        {
            ActivityCompat.requestPermissions(this,listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]),REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onInit(int i) {
        if(i!=TextToSpeech.SUCCESS) {
            Toast.makeText(this, "TTS Initialization Failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        textView.setText("");
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        }

    @Override
    public void onBeginningOfSpeech() {
        img_loading.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRmsChanged(float v) {
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
    }

    @Override
    public void onEndOfSpeech() {
        img_loading.setVisibility(View.INVISIBLE);
        toggleButton.setChecked(false);
    }

    @Override
    public void onError(int i) {
        img_loading.setVisibility(View.INVISIBLE);
        String error=getError(i);
        textView.setText(error);
        toggleButton.setChecked(false);
    }

    @Override
    public void onResults(Bundle bundle) {
        ArrayList<String> result=bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text=result.get(0);
        textView.setVisibility(View.VISIBLE);
        textView.setText(text);

        if(pattern_turn_on.matcher(text).matches()){
            response=ts.toggleService(getApplicationContext(), text);
            speakResult(response);
        }
        else if (pattern_navigate.matcher(text).matches()) {
            Uri mapUri = Uri.parse("geo:0,0?q="+text);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        }
        else if (pattern_call.matcher(text).matches()) {
            String contactname = text.substring(text.indexOf("call")+4, text.length()).trim();
            String number= getContactNumber(contactname);
            try {
                speakResult("Calling " +contactname);
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:"+number));
                startActivity(intent);
            } catch (SecurityException e) {
                Toast.makeText(context, "No call permission", Toast.LENGTH_SHORT).show();
            }
        }
        else if (pattern_sms.matcher(text).matches()) {
            try {
                    String contactName = text.substring(text.indexOf("to") + 2, text.indexOf("that")).trim();
                    String number= getContactNumber(contactName);
                    String msgContent = text.substring(text.indexOf("that")+4).trim();

                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(number,null,msgContent,null,null);
                    speakResult("message sent to "+ contactName);
                } catch(Exception e) {
                    Toast.makeText(context, "Error sending message", Toast.LENGTH_SHORT).show();
            }
        }
        else if (pattern_question.matcher(text).matches()) {
            getAnswer(text);
        }
        else if (pattern_openapp.matcher(text).matches()) {
            getInstalledApp();
            String appName = "";
            if(text.contains("open")) {
                appName = text.substring(text.indexOf("open")+4).trim();
            }
            else if(text.contains("launch")) {
                appName = text.substring(text.indexOf("launch")+6).trim();
            }
            else if(text.contains("deploy")) {
                appName = text.substring(text.indexOf("deploy")+6).trim();
            }
            startApp(appName.toLowerCase());

        }
        else if (pattern_music.matcher(text).matches()) {
            Intent intent = new Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER);
            startActivity(intent);
            speakResult("Opening music");
        }
        else if (pattern_alarm.matcher(text).matches()) {
            String alarmString=startAlarm(text);
            speakResult(alarmString);
        }
        else if (pattern_selfie.matcher(text).matches()) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent,111);
        }
        else
            speakResult("Sorry, I did not understand that");
        }

        @Override
        protected void onActivityResult(int request, int result, Intent data) {
            if(request==111) {
                Bitmap bitmap = (Bitmap)data.getExtras().get("data");

                String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                File myDir = new File(root + "/photos");
                myDir.mkdirs();

                Calendar calendar=Calendar.getInstance();
                String time = calendar.get(Calendar.MINUTE) + String.valueOf(calendar.get(Calendar.SECOND));
                
                String fname = "Photo"+time+".jpg";
                File file = new File(myDir,fname);
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG,100,out);
                    out.flush();
                    out.close();
                    Toast.makeText(context, "Photo stored in "+ root+"/photos/", Toast.LENGTH_SHORT).show();
                    
                } catch(Exception e) {
                    Toast.makeText(context, "Error occurred while saving photo", Toast.LENGTH_SHORT).show();
                }
            }
        }

        public void getAnswer(String text) {
            img_loading.setVisibility(View.VISIBLE);
            String url = Uri.parse("https://api.wolframalpha.com/v2/query")
                    .buildUpon()
                    .appendQueryParameter("format", "image,plaintext")
                    .appendQueryParameter("output", "JSON")
                    .appendQueryParameter("appid", WOLFRAM_ALPHA_APP_ID)
                    .build()
                    .toString();
            url += "&input=" + text.replace(" ", "+");

            JsonObjectRequest appDataRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    img_loading.setVisibility(View.INVISIBLE);

                    try {
                        JSONObject obj = response.getJSONObject("queryresult");

                        boolean success = Boolean.parseBoolean(obj.getString("success"));
                        if (!success) {
                            textView.setText("Failed to get Answer");
                            return;
                        }

                        //API SUCCESS

                        JSONArray pods = obj.getJSONArray("pods");

                        JSONObject pods1 = pods.getJSONObject(1);

                        JSONArray subPods = pods1.getJSONArray("subpods");
                        JSONObject subPod1 = subPods.getJSONObject(0);

                        String result = subPod1.getString("plaintext");

                        cardView.setVisibility(View.VISIBLE);
                        textView1.setVisibility(View.VISIBLE);
                        textView1.setText(result);
                        Handler handler=new Handler();
                        handler.postDelayed(() -> {
                            textView.setText("");
                            cardView.setVisibility(View.GONE);
                            textView1.setVisibility(View.GONE);
                        },5000);
                        speakResult(result);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, error -> {
                img_loading.setVisibility(View.INVISIBLE);
                textView.setText("");
                speakResult("Sorry, i think i don't know about it.");
            });
            VolleyRequest.getInstance(this).addToRequestQueue(appDataRequest);
        }

        public void startApp(String appName) {
            if(appNameList.contains(appName)) {
                int i=appNameList.indexOf(appName);
                speakResult("Launching "+appName);
                Intent intent = getPackageManager().getLaunchIntentForPackage(packageNameList.get(i));
                startActivity(intent);
            }
            else {
                speakResult("Sorry, i didn't find app named "+appName);
            }
        }
        public void getInstalledApp() {
            List<PackageInfo> packList = getPackageManager().getInstalledPackages(0);
            for (int i = 0; i < packList.size(); i++) {
                packageInfo = packList.get(i);
                appNameList.add(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString().toLowerCase());
                packageNameList.add(packageInfo.applicationInfo.packageName);
            }
        }

    public String getContactNumber(String cname) {

        database = databaseHelper.getReadableDatabase();
        String number="";

        String selectNumber = "SELECT number from table_contact WHERE name='"+cname+"' COLLATE NOCASE";
        Cursor cursor = database.rawQuery(selectNumber,null);

        while(cursor.moveToNext()) {
            number = cursor.getString(cursor.getColumnIndexOrThrow("number"));
            break;
        }

        cursor.close();
        database.close();
        databaseHelper.close();

        return number;
    }

    public void addContactDatabase()
    {
        database = databaseHelper.getWritableDatabase();
        String id,name,phone;

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        phone = pCur.getString(
                                pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        databaseHelper.addContact(name,phone,database);
                    }
                    pCur.close();
                }
            }}

        Toast.makeText(getBaseContext(),"Contacts list updated",Toast.LENGTH_LONG).show();
        cur.close();
        database.close();
        databaseHelper.close();
    }

        public String startAlarm(String input) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent myIntent;
            Calendar calendar = Calendar.getInstance();
            PendingIntent pendingIntent;

            myIntent = new Intent(MainActivity.this, AlarmNotificationReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, 0);

            try {
                String s = input.replaceAll("[^0-9]", "");
                int time = Integer.parseInt(s);
                if (input.contains("minute")) {
                    calendar.add(Calendar.MINUTE, time);
                } else if (input.contains("second")) {
                    calendar.add(Calendar.SECOND, time);
                } else if (input.contains("hour")) {
                    calendar.add(Calendar.HOUR, time);
                }

                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } catch (Exception e) {
                Toast.makeText(context, "Failed to set alarm", Toast.LENGTH_SHORT).show();
            }
            return "Alarm is set";
        }
    @Override
    public void onPartialResults(Bundle bundle) {
    }

    @Override
    public void onEvent(int i, Bundle bundle) {
    }

    public String getError(int errorcode)
    {
        img_loading.setVisibility(View.INVISIBLE);

        String message;
        switch(errorcode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error, please connect to internet";
                speakResult("Please check your internet connection");
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "Please speak something";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "Recognition Service busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }

        return message;
        }

        private void speakResult(String s) {
        tts.speak(s,TextToSpeech.QUEUE_FLUSH,null);
    }

    public void onDestroy()
    {
        super.onDestroy();
        tts.shutdown();
    }
}
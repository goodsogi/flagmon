package com.gntsoft.flagmon.login;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.gntsoft.flagmon.FMCommonActivity;
import com.gntsoft.flagmon.FMConstants;
import com.gntsoft.flagmon.R;
import com.gntsoft.flagmon.server.FMApiConstants;
import com.gntsoft.flagmon.server.ServerResultModel;
import com.gntsoft.flagmon.server.ServerResultParser;
import com.gntsoft.flagmon.setting.PlusNumberPicker;
import com.pluslibrary.server.PlusHttpClient;
import com.pluslibrary.server.PlusInputStreamStringConverter;
import com.pluslibrary.server.PlusOnGetDataListener;
import com.pluslibrary.utils.PlusClickGuard;
import com.pluslibrary.utils.PlusLogger;
import com.pluslibrary.utils.PlusOnClickListener;
import com.pluslibrary.utils.PlusPhoneNumberFinder;
import com.pluslibrary.utils.PlusToaster;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by johnny on 15. 2. 26.
 */
public class SecondSignUpActivity extends FMCommonActivity implements PlusOnGetDataListener {
    private static final int SIGN_UP = 11;
    private static final int SEND_USER_CONTACT = 22;
    final int DRAWABLE_RIGHT = 2;
    String[] sexs = {"남", "여"};
    private EditText mUserSexView;
    private String userPhone;
    private String userContacts;
    private String mAuthKey;

    public void showPolicy(View v) {
        PlusClickGuard.doIt(v);
        Intent intent = new Intent(this, PolicyActivity.class);
        intent.putExtra(FMConstants.KEY_POLICY_TYPE, getPolicyType(v));
        startActivity(intent);
    }

    public void finishSignUp(View v) {
        PlusClickGuard.doIt(v);

        String userEmail = getIntent().getStringExtra(FMConstants.KEY_USER_EMAIL);
        String userPassword = getIntent().getStringExtra(FMConstants.KEY_USER_PASSWORD);
        String userName = getIntent().getStringExtra(FMConstants.KEY_USER_NAME);


        EditText userAgeView = (EditText) findViewById(R.id.userAge);
        String userAge = userAgeView.getText().toString();

        if (userAge.equals("")) {
            PlusToaster.doIt(this, "나이를 입력해주세요");
            return;
        }

        String userPhone = PlusPhoneNumberFinder
                .doIt(SecondSignUpActivity.this);

        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("user_email", userEmail));
        postParams.add(new BasicNameValuePair("user_pw", userPassword));
        postParams.add(new BasicNameValuePair("user_gender", getUserSex()));
        postParams.add(new BasicNameValuePair("user_name", userName));
        // postParams.add(new BasicNameValuePair("where", mArea));
        //생년월일에 나이??
        postParams.add(new BasicNameValuePair("user_birth", userAge));
        postParams.add(new BasicNameValuePair("user_phone", userPhone));

//        SharedPreferences sharedPreference = getSharedPreferences(
//                PlusConstants.PREF_NAME, Context.MODE_PRIVATE);
//        String token = sharedPreference.getString(
//                PlusGcmRegister.PROPERTY_REG_ID, "");
//        postParams.add(new BasicNameValuePair("deviceid", token));
//        postParams.add(new BasicNameValuePair("alarm2", soundNoti));
//        postParams.add(new BasicNameValuePair("alarm1", vibrationNoti));

        new PlusHttpClient(this, this, false).execute(SIGN_UP,
                FMApiConstants.SIGN_UP, new PlusInputStreamStringConverter(),
                postParams);
    }

    @Override
    public void onSuccess(Integer from, Object datas) {
        if (datas == null) return;
        switch (from) {

            case SIGN_UP:

                ServerResultModel model = new ServerResultParser().doIt((String) datas);
                PlusToaster.doIt(this, model.getResult().equals("success") ? "회원가입되었습니다" : "회원가입되지 못했습니다");
                if (model.getResult().equals("success")) {
                    mAuthKey = model.getMsg();
                    getuserContacts();
                }
                break;

            case SEND_USER_CONTACT:

                ServerResultModel model2 = new ServerResultParser().doIt((String) datas);
                if (model2 != null && model2.getResult().equals("success")) {
                    PlusLogger.doIt("연락처 서버 전송 성공!!");
                    finish();
                }
                break;

        }

    }

    public void getuserContacts() {
       UserContactManager manager = new UserContactManager(this);
        manager.run();

    }

    public void sendUserContact(Cursor cursor) {
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("key", mAuthKey));
        //makeContactJson(cursor) 한번 더 호출(evaluate) 하면 cursor를 이미 close했기 때문에 오류발생
        postParams.add(new BasicNameValuePair("user_contacts", makeContactJson(cursor)));


        new PlusHttpClient(this, this, false).execute(SEND_USER_CONTACT,
                FMApiConstants.SEND_USER_CONTACT, new PlusInputStreamStringConverter(),
                postParams);
    }

    public void setUserAge(String userAge) {
        EditText userAgeView = (EditText) findViewById(R.id.userAge);
        userAgeView.setText(userAge);
    }

    private String makeContactJson(Cursor cursor) {
        JSONArray jsonArray = new JSONArray();

         while(cursor.moveToNext()) {
             JSONObject jsonObject = new JSONObject();
             String name = cursor.getString(cursor.getColumnIndex(Build.VERSION.SDK_INT
                     >= Build.VERSION_CODES.HONEYCOMB ?
                     ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
                     ContactsContract.Contacts.DISPLAY_NAME));


             String phoneNo = "";

             String contactId =
                     cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

             Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                     ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);

             while ((phones.moveToNext())) {
                 phoneNo = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

             }

             phones.close();


             try {
                 jsonObject.put("addr_name", name);

                 jsonObject.put("addr_phone", phoneNo);

             } catch (JSONException e) {
                 e.printStackTrace();
             }

             jsonArray.put(jsonObject);

         }
        cursor.close();

        try {
            return URLEncoder.encode(jsonArray.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_signup);

        addListenerToButton();


    }

    private void addListenerToButton() {
        mUserSexView = (EditText) findViewById(R.id.userSex);
        mUserSexView.setOnClickListener(new PlusOnClickListener() {
            @Override
            protected void doIt() {
                showSexDialog();
            }
        });

        mUserSexView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int leftEdgeOfRightDrawable = mUserSexView.getRight()
                            - mUserSexView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
                    // when EditBox has padding, adjust leftEdge like
                    // leftEdgeOfRightDrawable -= getResources().getDimension(R.dimen.edittext_padding_left_right);
                    if (event.getRawX() >= leftEdgeOfRightDrawable) {
                        // clicked on clear icon
                        mUserSexView.setText("");
                        return true;
                    }
                }
                return false;
            }
        });


        final EditText userAgeView = (EditText) findViewById(R.id.userAge);
        userAgeView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int leftEdgeOfRightDrawable = userAgeView.getRight()
                            - userAgeView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
                    // when EditBox has padding, adjust leftEdge like
                    // leftEdgeOfRightDrawable -= getResources().getDimension(R.dimen.edittext_padding_left_right);
                    if (event.getRawX() >= leftEdgeOfRightDrawable) {
                        // clicked on clear icon
                        userAgeView.setText("");
                        return true;
                    }
                }
                return false;
            }
        });
        userAgeView.setOnClickListener(new PlusOnClickListener() {
            @Override
            protected void doIt() {
                showAgePicker();
            }
        });



        final CheckBox servicePolicy = (CheckBox) findViewById(R.id.checkbox_service);
        final CheckBox privacyPolicy = (CheckBox) findViewById(R.id.checkbox_privacy);
        final CheckBox locationPolicy = (CheckBox) findViewById(R.id.checkbox_location);
        servicePolicy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && privacyPolicy.isChecked() && locationPolicy.isChecked())
                    enableFinishButton();
            }
        });


        privacyPolicy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && servicePolicy.isChecked() && locationPolicy.isChecked())
                    enableFinishButton();
            }
        });


        locationPolicy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && privacyPolicy.isChecked() && servicePolicy.isChecked())
                    enableFinishButton();
            }
        });
    }

    private void showAgePicker() {
        PlusNumberPicker picker = new PlusNumberPicker(this, null);
        picker.show();
    }

    private void enableFinishButton() {
        Button finish = (Button) findViewById(R.id.finishSignup);
        finish.setEnabled(true);
    }

    private void showSexDialog() {

        AlertDialog.Builder ab = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT);
        ab.setTitle("선택해주세요.");
        ab.setItems(sexs, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                changeSex(whichButton);

            }
        }).setNegativeButton("닫기",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        ab.show();
    }

    private void changeSex(int whichButton) {
        mUserSexView.setText(sexs[whichButton]);
    }

    private int getPolicyType(View v) {
        switch (v.getId()) {
            case R.id.text_service:
                return FMConstants.POLICY_SERVICE;

            case R.id.text_privacy:
                return FMConstants.POLICY_PRIVACY;
            case R.id.text_location:
                return FMConstants.POLICY_LOCATION;

        }
        return FMConstants.POLICY_SERVICE;
    }

    private String getUserSex() {
        return mUserSexView.getText().toString().equals(sexs[0]) ? "M" : "W";
    }
}

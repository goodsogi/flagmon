package com.gntsoft.flagmon.friend;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gntsoft.flagmon.FMCommonActivity;
import com.gntsoft.flagmon.FMCommonAdapter;
import com.gntsoft.flagmon.FMConstants;
import com.gntsoft.flagmon.R;
import com.gntsoft.flagmon.server.FMApiConstants;
import com.gntsoft.flagmon.server.FriendModel;
import com.gntsoft.flagmon.server.ServerResultModel;
import com.gntsoft.flagmon.server.ServerResultParser;
import com.gntsoft.flagmon.user.UserActivity;
import com.pluslibrary.server.PlusHttpClient;
import com.pluslibrary.server.PlusInputStreamStringConverter;
import com.pluslibrary.server.PlusOnGetDataListener;
import com.pluslibrary.utils.PlusOnClickListener;
import com.pluslibrary.utils.PlusToaster;
import com.pluslibrary.utils.PlusViewHolder;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by johnny on 15. 3. 3.
 */
public class ChooseFriendListAdapter extends FMCommonAdapter<FriendModel> implements
        PlusOnGetDataListener {

    private static final int DELETE_FRIEND = 0;
    private static final int SELECT_FRIEND = 1;
    private final Fragment mFragment;


    public ChooseFriendListAdapter(Context context, Fragment fragment, ArrayList<FriendModel> datas) {
        super(context, R.layout.choose_friend_list_item, datas);
        mFragment = fragment;

    }


    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.choose_friend_list_item,
                    parent, false);
        }

        final FriendModel data = mDatas.get(position);
        TextView name = PlusViewHolder.get(convertView, R.id.name);


        ImageView img = PlusViewHolder.get(convertView, R.id.img);
        mImageLoader.displayImage(data.getProfileImageUrl(), img,
                mOption);
        img.setOnClickListener(new PlusOnClickListener() {
            @Override
            protected void doIt() {
                launchUserPageActivity(data.getUserEmail(), data.getName());
            }
        });
        name.setText(data.getName());

        Button delete = PlusViewHolder.get(convertView, R.id.delete);
        delete.setOnClickListener(new PlusOnClickListener() {
            @Override
            protected void doIt() {
                deleteFriend(data.getIdx());
            }
        });

        Button select = PlusViewHolder.get(convertView, R.id.select);
        select.setOnClickListener(new PlusOnClickListener() {
            @Override
            protected void doIt() {
                selectFriend(data.getIdx());
            }
        });


        return convertView;
    }

    @Override
    public void onSuccess(Integer from, Object datas) {
        if (datas == null)
            return;
        switch (from) {
            case DELETE_FRIEND:
                ServerResultModel model = new ServerResultParser().doIt((String) datas);
                PlusToaster.doIt(mContext, model.getResult().equals("success") ? "친구를 삭제했습니다" : "친구를 삭제하지 못했습니다");
                if (model.getResult().equals("success")) {
                    //추가 액션??
                }
                break;

            case SELECT_FRIEND:
                ServerResultModel model2 = new ServerResultParser().doIt((String) datas);
                if (model2.getResult().equals("success")) {
                    //추가 액션??
                    //친구를 상단으로 이동
                    //리스트 갱신
                    ((ChooseFriendListFragment) mFragment).getDataFromServer();
                }
                break;
        }

    }

    private void launchUserPageActivity(String userEmail, String userName) {
        Intent intent = new Intent(mContext, UserActivity.class);
        intent.putExtra(FMConstants.KEY_USER_EMAIL, userEmail);
        intent.putExtra(FMConstants.KEY_USER_NAME, userName);
        mContext.startActivity(intent);

    }

    private void deleteFriend(String idx) {

        showDeleteFriendAlertDialog(idx);

    }

    private void showDeleteFriendAlertDialog(final String idx) {
        AlertDialog.Builder ab = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_LIGHT);
        ab.setTitle("친구를 삭제하시겠습니까?");
        ab.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                performDeleteFriend(idx);
            }
        });
        ab.show();

    }

    private void performDeleteFriend(String idx) {
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("idx", idx));
        postParams.add(new BasicNameValuePair("key", ((FMCommonActivity) mContext).getUserAuthKey()));


        new PlusHttpClient((android.app.Activity) mContext, this, false).execute(DELETE_FRIEND,
                FMApiConstants.DELETE_FRIEND, new PlusInputStreamStringConverter(),
                postParams);

    }

    private void selectFriend(String idx) {
        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("idx", idx));
        postParams.add(new BasicNameValuePair("key", ((FMCommonActivity) mContext).getUserAuthKey()));


        new PlusHttpClient((android.app.Activity) mContext, this, false).execute(SELECT_FRIEND,
                FMApiConstants.SELECT_FRIEND, new PlusInputStreamStringConverter(),
                postParams);
    }

}

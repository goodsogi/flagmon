package com.gntsoft.flagmon.neighbor;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.gntsoft.flagmon.FMCommonActivity;
import com.gntsoft.flagmon.FMCommonFragment;
import com.gntsoft.flagmon.FMConstants;
import com.gntsoft.flagmon.FMListAdapter;
import com.gntsoft.flagmon.R;
import com.gntsoft.flagmon.detail.DetailActivity;
import com.gntsoft.flagmon.server.FMApiConstants;
import com.gntsoft.flagmon.server.FMListParser;
import com.gntsoft.flagmon.server.FMModel;
import com.gntsoft.flagmon.utils.LoginChecker;
import com.pluslibrary.server.PlusHttpClient;
import com.pluslibrary.server.PlusInputStreamStringConverter;
import com.pluslibrary.server.PlusOnGetDataListener;
import com.pluslibrary.utils.PlusClickGuard;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

public class ListFragment extends FMCommonFragment implements
        PlusOnGetDataListener {

    private static final int GET_LIST_DATA = 0;
    String[] listOptionDatas = {"인기순", "최근 등록순", "거리순"};

    public ListFragment() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getDataFromServer(FMConstants.SORT_BY_POPULAR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list_neighbor,
                container, false);
        return rootView;
    }

    public void showSortPopup(View v) {
        PlusClickGuard.doIt(v);

        AlertDialog.Builder ab = new AlertDialog.Builder(mActivity, AlertDialog.THEME_HOLO_LIGHT);
        ab.setTitle("정렬방식을 선택해주세요.");
        ab.setItems(listOptionDatas, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                doSort(whichButton);

            }
        }).setNegativeButton("닫기",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        ab.show();
    }

    @Override
    public void onSuccess(Integer from, Object datas) {
        if (datas == null)
            return;
        switch (from) {
            case GET_LIST_DATA:
                makeList(new FMListParser().doIt((String) datas));
                break;
        }

    }

    @Override
    protected void addListenerToButton() {
        Button sort = (Button) mActivity.findViewById(R.id.sort);
        sort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortPopup(v);
            }
        });

    }

    private void getDataFromServer(String sortType) {
        double latUL = ((FMCommonActivity) mActivity).getLatUL();
        double lonUL = ((FMCommonActivity) mActivity).getLonUL();
        double latLR = ((FMCommonActivity) mActivity).getLatLR();
        double lonLR = ((FMCommonActivity) mActivity).getLonLR();


        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("list_menu", FMConstants.DATA_TAB_NEIGHBOR));
        postParams.add(new BasicNameValuePair("latUL", String.valueOf(latUL)));
        postParams.add(new BasicNameValuePair("lonUL", String.valueOf(lonUL)));
        postParams.add(new BasicNameValuePair("latLR", String.valueOf(latLR)));
        postParams.add(new BasicNameValuePair("lonLR", String.valueOf(lonLR)));
        postParams.add(new BasicNameValuePair("sort", sortType));
        if (LoginChecker.isLogIn(mActivity)) {
            postParams.add(new BasicNameValuePair("key", getUserAuthKey()));
        }


        new PlusHttpClient(mActivity, this, false).execute(GET_LIST_DATA,
                FMApiConstants.GET_LIST_DATA, new PlusInputStreamStringConverter(),
                postParams);
    }

    private void lauchDetailActivity(String idx) {
        Intent intent = new Intent(mActivity, DetailActivity.class);
        intent.putExtra(FMConstants.KEY_POST_IDX, idx);

        startActivity(intent);
    }

    private void doSort(int whichButton) {


        switch (whichButton) {
            case 0:
                sortByPopular();
                break;

            case 1:
                sortByRecent();
                break;

            case 2:
                sortByDistance();
                break;

        }
    }

    private void sortByDistance() {

        //sort 값 수정!!
        getDataFromServer(FMConstants.SORT_BY_DISTANCE);
    }

    private void sortByRecent() {
        getDataFromServer(FMConstants.SORT_BY_RECENT);
    }

    private void sortByPopular() {
        getDataFromServer(FMConstants.SORT_BY_POPULAR);
    }

    private void makeList(final ArrayList<FMModel> datas) {

        ListView list = (ListView) mActivity
                .findViewById(R.id.list_neighbor);

        if (list == null || datas == null) return;
        list.setAdapter(new FMListAdapter(mActivity,
                datas));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                lauchDetailActivity(datas.get(position).getIdx());
            }
        });
    }


}
package com.gntsoft.flagmon.search;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.gntsoft.flagmon.FMCommonActivity;
import com.gntsoft.flagmon.FMCommonMapFragment;
import com.gntsoft.flagmon.FMConstants;
import com.gntsoft.flagmon.R;
import com.gntsoft.flagmon.detail.DetailActivity;
import com.gntsoft.flagmon.myalbum.SearchLocationListAdapter;
import com.gntsoft.flagmon.server.FMApiConstants;
import com.gntsoft.flagmon.server.FMMapParser;
import com.gntsoft.flagmon.server.FMModel;
import com.gntsoft.flagmon.server.PlaceModel;
import com.gntsoft.flagmon.server.PlaceParser;
import com.gntsoft.flagmon.utils.FMPhotoResizer;
import com.gntsoft.flagmon.utils.LoginChecker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.pluslibrary.server.PlusHttpClient;
import com.pluslibrary.server.PlusInputStreamStringConverter;
import com.pluslibrary.server.PlusOnGetDataListener;
import com.pluslibrary.utils.PlusClickGuard;
import com.pluslibrary.utils.PlusDpPixelConverter;
import com.pluslibrary.utils.PlusToaster;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MapFragment extends FMCommonMapFragment implements
        PlusOnGetDataListener {
    private static final int GET_MAP_DATA = 0;
    private static final int SEARCH_LOCATION = 22;
    String[] mapOptionDatas = {"인기순", "최근 등록순"};


    public MapFragment() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //getDataFromServer(getArguments().getString(FMConstants.KEY_SORT_TYPE));
        performLocationSearch();
    }

    public void performLocationSearch() {

        String keyword = getArguments().getString(FMConstants.KEY_KEYWORD);

            new PlusHttpClient(mActivity, this, false).execute(
                    SEARCH_LOCATION,
                    FMApiConstants.SEARCH_PLACE + "query="
                            + keyword
                            + "&sensor=true" + "&language=ko" + "&key="
                            + FMApiConstants.GOOGLE_APP_KEY,
                    new PlaceParser());



    }

    public void getDataFromServer(String sortType) {
        LatLngBounds bounds = mGoogleMap.getProjection().getVisibleRegion().latLngBounds;

        double left = bounds.southwest.longitude;
        double top = bounds.northeast.latitude;
        double right = bounds.northeast.longitude;
        double bottom = bounds.southwest.latitude;

        ((FMCommonActivity) mActivity).setLatUL(bounds.northeast.latitude);
        ((FMCommonActivity) mActivity).setLonUL(bounds.southwest.longitude);
        ((FMCommonActivity) mActivity).setLatLR(bounds.southwest.latitude);
        ((FMCommonActivity) mActivity).setLonLR(bounds.northeast.longitude);


        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("list_menu", FMConstants.DATA_TAB_NEIGHBOR));
        postParams.add(new BasicNameValuePair("latUL", String.valueOf(bounds.northeast.latitude)));
        postParams.add(new BasicNameValuePair("lonUL", String.valueOf(bounds.southwest.longitude)));
        postParams.add(new BasicNameValuePair("latLR", String.valueOf(bounds.southwest.latitude)));
        postParams.add(new BasicNameValuePair("lonLR", String.valueOf(bounds.northeast.longitude)));
        postParams.add(new BasicNameValuePair("sort", sortType));
        if (LoginChecker.isLogIn(mActivity)) {
            postParams.add(new BasicNameValuePair("key", getUserAuthKey()));
        }


        new PlusHttpClient(mActivity, this, false).execute(GET_MAP_DATA,
                FMApiConstants.GET_MAP_DATA, new PlusInputStreamStringConverter(),
                postParams);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map_search,
                container, false);
        return rootView;
    }

    public void showSortPopup(View v) {
        PlusClickGuard.doIt(v);

        AlertDialog.Builder ab = new AlertDialog.Builder(mActivity, AlertDialog.THEME_HOLO_LIGHT);
        ab.setTitle("정렬방식을 선택해주세요.");
        ab.setItems(mapOptionDatas, new DialogInterface.OnClickListener() {
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
            case GET_MAP_DATA:
                handleMapData(new FMMapParser().doIt((String) datas));
                break;
            case SEARCH_LOCATION:
                makeList((ArrayList<PlaceModel>) datas);
                break;

        }

    }

    private void makeList(final ArrayList<PlaceModel> datas) {

        ListView list = (ListView) mActivity.findViewById(R.id.listSearchLocation);

        if (list == null || datas == null) return;
        list.setVisibility(View.VISIBLE);
        list.setAdapter(new SearchLocationListAdapter(mActivity,
                datas));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                moveToPostion(datas.get(position).getLat(), datas.get(position).getLng());
                getDataFromServer(getArguments().getString(FMConstants.KEY_SORT_TYPE));
            }
        });


        //리스트 크기 제한(50dp??)
        if (datas.size() > 2) {
            //listview를 감싸고 있는 부모 view(컨테이너) 사용
            list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, PlusDpPixelConverter.doIt(mActivity, 72)));
        }
    }

    private void moveToPostion(String lat, String lng) {
        LatLng position = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));


        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, 14);
        mGoogleMap.moveCamera(cameraUpdate);
    }

    @Override
    protected void addListenerToButton() {
        // TODO Auto-generated method stub


        Button sort = (Button) mActivity.findViewById(R.id.sort);
        sort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortPopup(v);
            }
        });
    }

    private void doSort(int whichButton) {


        switch (whichButton) {
            case 0:
                sortByPopular();
                break;

            case 1:
                sortByRecent();
                break;


        }
    }

    private void sortByRecent() {
        getDataFromServer(FMConstants.SORT_BY_RECENT);
    }

    private void sortByPopular() {
        getDataFromServer(FMConstants.SORT_BY_POPULAR);
    }

    private void handleMapData(ArrayList<FMModel> datas) {

        if (datas == null || datas.size() == 0) {
            PlusToaster.doIt(mActivity, "검색결과가 없습니다");
            return;
        }


        for (int i = 0; i < datas.size(); i++) {
            fetchImageFromServer(datas.get(i), i);


        }
    }

    private void fetchImageFromServer(final FMModel mapDataModel, final int position) {
        mImageLoader.loadImage(mapDataModel.getImgUrl(), mOption, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String s, View view) {

            }

            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {

            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {

                showMarkers(bitmap, mapDataModel);


            }

            @Override
            public void onLoadingCancelled(String s, View view) {

            }
        });


    }

    private void showMarkers(Bitmap bitmap, FMModel mapDataModel) {
        LatLng latLng = new LatLng(Double.parseDouble(mapDataModel.getLat()), Double.parseDouble(mapDataModel.getLon()));
        mGoogleMap.addMarker(new MarkerOptions().position(latLng).snippet(mapDataModel.getIdx())
                .icon(getMarKerImg(bitmap)).anchor(0f, 1.0f));
        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                goToDetail(marker.getSnippet());
                return false;
            }
        });
    }

    private void goToDetail(String idx) {
        Intent intent = new Intent(mActivity, DetailActivity.class);
        intent.putExtra(FMConstants.KEY_POST_IDX, idx);

        startActivity(intent);
    }

    private BitmapDescriptor getMarKerImg(Bitmap original) {

        //마스킹 이미지를 xxhdpi 폴더에 넣으면 마스킹이 안됨, xhdpi 폴더에 넣어야 함
        //마스킹
        Bitmap scaledOriginal = FMPhotoResizer.doIt(mActivity,original);
        Bitmap frame = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.thumbnail_1_0001);
        Bitmap mask = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.mask);
        Log.d("mask", "image witdh: " + mask.getWidth() + " height: " + mask.getHeight());
        Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        mCanvas.drawBitmap(scaledOriginal, 0, 0, null);
        mCanvas.drawBitmap(mask, 0, 0, paint);
        mCanvas.drawBitmap(frame, 0, 0, null);
        paint.setXfermode(null);


        return BitmapDescriptorFactory.fromBitmap(result);


    }


}

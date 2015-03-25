package com.gntsoft.flagmon.server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by johnny on 15. 3. 13.
 */
public class FriendListParser {
    public ArrayList<FriendModel> doIt(String rawData) {
        ArrayList<FriendModel> model = new ArrayList<>();

        try {

            JSONArray jsonArray = new JSONArray(rawData);
            for (int i = 0; i < jsonArray.length(); i++) {
                FriendModel data = new FriendModel();
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                JSONObject subJsonObject = (JSONObject) jsonObject.opt("friendlist");

                data.setIdx(subJsonObject.optString("idx"));
                data.setProfileImageUrl(subJsonObject.optString("profile_url"));
                data.setName(subJsonObject.optString("friend_name"));
                data.setUserEmail(subJsonObject.optString("is_favorite"));
                data.setIsFavorite(subJsonObject.optString("friend_email"));

                model.add(data);
            }

        } catch (Exception e) {
            return null;
        }

        return model;
    }
}
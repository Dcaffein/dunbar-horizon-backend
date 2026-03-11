package com.example.DunbarHorizon.account.adapter.in.web.OAuth2.OAuth2UserInfo;

import java.util.Map;

public interface OAuth2UserInfo {
    String getId();
    String getName();
    String getEmail();
    Map<String, Object> getAttributes();
}
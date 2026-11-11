package com.surprise.mortgagesurprise.security.oauth2;

public interface OAuth2UserInfo {
    String getId();
    String getEmail();
    String getFirstName();
    String getLastName();
    boolean isEmailVerified();
}

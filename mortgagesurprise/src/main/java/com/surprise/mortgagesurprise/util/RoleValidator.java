package com.surprise.mortgagesurprise.util;

import com.surprise.mortgagesurprise.security.CustomUserDetails;

public final class RoleValidator {

    public static boolean isAdmin(CustomUserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private RoleValidator() {
    }
}

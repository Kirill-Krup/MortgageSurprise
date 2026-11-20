package com.surprise.mortgagesurprise.security.oauth2;

import java.util.Map;

public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo create(String provider, Map<String, Object> attributes) {
        return switch (provider.toLowerCase()) {
            case "google" -> new GoogleUserInfo(attributes);
            case "github" -> new GitHubUserInfo(attributes);
            default -> throw new IllegalArgumentException("Unsupported OAuth2 provider: " + provider);
        };
    }

    private record GoogleUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {
        @Override
        public String getId() {
            return stringValue(attributes.get("sub"));
        }

        @Override
        public String getEmail() {
            return stringValue(attributes.get("email"));
        }

        @Override
        public String getFirstName() {
            return stringValue(attributes.get("given_name"));
        }

        @Override
        public String getLastName() {
            return stringValue(attributes.get("family_name"));
        }

        @Override
        public boolean isEmailVerified() {
            return Boolean.TRUE.equals(attributes.get("email_verified"));
        }
    }

    private record GitHubUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {
        @Override
        public String getId() {
            Object id = attributes.get("id");
            return id == null ? null : id.toString();
        }

        @Override
        public String getEmail() {
            return stringValue(attributes.get("email"));
        }

        @Override
        public String getFirstName() {
            String name = stringValue(attributes.get("name"));
            if (name != null && !name.isBlank()) {
                return name.trim().split("\\s+", 2)[0];
            }
            return stringValue(attributes.get("login"));
        }

        @Override
        public String getLastName() {
            String name = stringValue(attributes.get("name"));
            if (name == null) {
                return null;
            }
            String[] parts = name.trim().split("\\s+", 2);
            return parts.length == 2 ? parts[1] : null;
        }

        @Override
        public boolean isEmailVerified() {
            return false;
        }
    }

    private static String stringValue(Object value) {
        return value instanceof String string ? string : null;
    }
}

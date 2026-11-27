package com.surprise.mortgagesurprise.dto.abtest;

public record VariantStat(
        String color,
        long totalUsers,
        long totalClicks,
        double averageClicksPerUser
) {}

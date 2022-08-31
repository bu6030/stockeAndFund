package com.buxuesong.account.service;

public interface FundCacheService {
    String getFundInfoFromApi(String code);

    String getFundInfoFromSinaApi(String code);

    String getFundInfoFromApiCache(String code);

    String getFundInfoFromSinaApiCache(String code);
}
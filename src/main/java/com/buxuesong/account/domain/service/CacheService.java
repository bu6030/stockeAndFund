package com.buxuesong.account.domain.service;

import com.buxuesong.account.infrastructure.adapter.rest.GTimgRestClient;
import com.buxuesong.account.infrastructure.adapter.rest.SinaRestClient;
import com.buxuesong.account.infrastructure.adapter.rest.TiantianFundRestClient;
import com.buxuesong.account.infrastructure.adapter.rest.response.StockDayHistoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
@CacheConfig(cacheNames = "cache")
public class CacheService {

    @Autowired
    private SinaRestClient sinaRestClient;

    @Autowired
    private GTimgRestClient gTimgRestClient;

    @Autowired
    private TiantianFundRestClient tiantianFundRestClient;

    @Cacheable(key = "'fund_sina_'+#code")
    public String getFundInfoFromSina(String code) {
        log.info("通过新浪基金缓存接口获取基金，编码：{}", code);
        return sinaRestClient.getFundInfo(code);
    }

    @Cacheable(key = "'fund_'+#code")
    public String getFundInfoFromTiantianFund(String code) {
        log.info("通过天天基金缓存接口获取基金，编码：{}", code);
        return tiantianFundRestClient.getFundInfo(code);
    }

    @Cacheable(key = "'stock_'+#param")
    public String getStockInfo(String param) {
        log.info("通过腾讯股票接口缓存获取股票，编码：{}", param);
        return gTimgRestClient.getStockInfo(param);
    }

    @Cacheable(key = "'stock_day_his_'+#param")
    public ArrayList<StockDayHistoryResponse> getStockDayHistory(String param) {
        log.info("通过新浪股票接口缓存获取股票日线历史，编码：{}", param);
        return sinaRestClient.getStockDayHistory(param);
    }

    @CacheEvict(cacheNames = "cache", allEntries = true)
    public int removeAllCache() {
        log.info("removeAllCache，让缓存失效");
        return 0;
    }

}

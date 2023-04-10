package com.buxuesong.account.infrastructure.adapter.rest;

import com.buxuesong.account.infrastructure.adapter.rest.response.StockDayHistoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@Slf4j
@Service
public class SinaRestClient {

    @Autowired
    private RestTemplate restTemplate;

    private static final String GET_FUND_INFO_SINA_URL = "https://hq.sinajs.cn/?_={timestamp}/&list=sz{code},f_{code}";

    private static final String GET_STOCK_DAY_HISTORY_SINA_URL = "https://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol={code}&scale=240&datalen={datalen}";

    public String getFundInfo(String code) {
        log.info("通过新浪基金接口获取基金，编码：{}， URL：{}", code, GET_FUND_INFO_SINA_URL);
        ResponseEntity<String> response = null;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Referer", "http://finance.sina.com.cn/");
        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        try {
            response = restTemplate.exchange(
                GET_FUND_INFO_SINA_URL, HttpMethod.GET, httpEntity, String.class, System.currentTimeMillis(), code, code);
        } catch (Exception e) {
            log.info("获取新浪基金接口异常: {]", e);
            return null;
        }
        return response.getBody();
    }

    public ArrayList<StockDayHistoryResponse> getStockDayHistory(String code, String dataLen) {
        log.info("通过新浪股票接口获取股票历史，编码：{}， URL：{}", code, GET_STOCK_DAY_HISTORY_SINA_URL);
        ResponseEntity<ArrayList<StockDayHistoryResponse>> response = null;
        try {
            response = restTemplate.exchange(
                GET_STOCK_DAY_HISTORY_SINA_URL, HttpMethod.GET, null,
                new ParameterizedTypeReference<ArrayList<StockDayHistoryResponse>>() {}, code, dataLen);
        } catch (Exception e) {
            log.info("获取新浪股票接口异常: {]", e);
            return null;
        }
        return response.getBody();
    }

    public static void main(String[] args) throws Exception {
        //第1页地址
        String url = "http://vip.stock.finance.sina.com.cn/corp/go.php/vISSUE_ShareBonus/stockid/000550.phtml";    //发送http请求
        Document document = Jsoup.connect(url).get();
        //在id=J_goodsList的div下，获取所有带有data-sku属性的li标签
        Elements lis = document.select("#sharebonus_1");
        lis.forEach(li -> {
            Elements fenhongList = li.select("tbody");
            System.out.println(fenhongList.text());
        }
        );
    }
}

package com.buxuesong.account.domain.model.fund;

import com.alibaba.fastjson.JSONArray;
import com.buxuesong.account.apis.model.request.FundRequest;
import com.buxuesong.account.apis.model.response.SearchFundResult;
import com.buxuesong.account.domain.model.stock.StockEntity;
import com.buxuesong.account.domain.service.CacheService;
import com.buxuesong.account.infrastructure.adapter.rest.SinaRestClient;
import com.buxuesong.account.infrastructure.adapter.rest.TiantianFundRestClient;
import com.buxuesong.account.infrastructure.adapter.rest.response.StockDayHistoryResponse;
import com.buxuesong.account.infrastructure.general.utils.DateTimeUtils;
import com.buxuesong.account.infrastructure.general.utils.UserUtils;
import com.buxuesong.account.infrastructure.persistent.po.FundHisPO;
import com.buxuesong.account.infrastructure.persistent.po.FundJZPO;
import com.buxuesong.account.infrastructure.persistent.po.FundPO;
import com.buxuesong.account.infrastructure.persistent.repository.FundHisMapper;
import com.buxuesong.account.infrastructure.persistent.repository.FundJZMapper;
import com.buxuesong.account.infrastructure.persistent.repository.FundMapper;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class FundEntity {
    @SerializedName("fundcode")
    private String fundCode;
    @SerializedName("name")
    private String fundName;
    private String jzrq;// 净值日期
    private String dwjz;// 当日净值
    private String gsz; // 估算净值
    private String gszzl;// 估算涨跌百分比 即-0.42%
    private String gztime;// gztime估值时间

    private String costPrise;// 持仓成本价
    private String bonds;// 持有份额
    private String app;// 支付宝/东方财富/东方证券
    private String incomePercent;// 收益率
    private String income;// 收益

    private boolean hide;// 是否隐藏
    private String currentDayJingzhi;// 当日净值（每个交易日晚9点之后有新日期数据）
    private String previousDayJingzhi;// 前一日净值
    private String oneYearAgoUpper;
    private String oneSeasonAgoUpper;
    private String oneMonthAgoUpper;
    private String oneWeekAgoUpper;

    public FundEntity() {
    }

    public FundEntity(String fundCode) {
        if (StringUtils.isNotBlank(fundCode)) {
            String[] codeStr = fundCode.split(",");
            if (codeStr.length > 2) {
                this.fundCode = codeStr[0];
                this.costPrise = codeStr[1];
                this.bonds = codeStr[2];
            } else {
                this.fundCode = codeStr[0];
                this.costPrise = "--";
                this.bonds = "--";
            }
        } else {
            this.fundCode = fundCode;
        }
        this.fundName = "--";
    }

    public static void loadFund(FundEntity fund, Map<String, String[]> codeMap) {
        String code = fund.getFundCode();
        if (codeMap.containsKey(code)) {
            String[] codeStr = codeMap.get(code);
            if (codeStr.length > 3) {
                fund.setCostPrise(codeStr[1]);
                fund.setBonds(codeStr[2]);
                fund.setApp(codeStr[3]);
            } else {
                fund.setCostPrise(codeStr[1]);
                fund.setBonds(codeStr[2]);
            }
        }
    }

    public static FundEntity loadFundFromSina(String code, String fundStr) {
        fundStr = fundStr.replaceAll("var hq_str_sz" + code + "=", "")
            .replaceAll("var hq_str_f_" + code + "=", "").replaceAll("\"", "");
        String[] lines = fundStr.split("\n");
        FundEntity fundEntity = new FundEntity();
        String[] fundInfoArr = lines[0].split(",");
        String[] fundJingZhiArr = lines[1].split(",");
        BigDecimal gsz = null;
        BigDecimal dwjz = null;
        if (!lines[0].contains(",")) {
            gsz = new BigDecimal(fundJingZhiArr[1]);
            dwjz = new BigDecimal(fundJingZhiArr[1]);
        } else {
            gsz = new BigDecimal(fundInfoArr[3]);
            dwjz = new BigDecimal(fundInfoArr[2]);
        }
        if (gsz.compareTo(new BigDecimal("0")) == 0) {
            gsz = dwjz;
        }
        BigDecimal gszzl = gsz.subtract(dwjz).divide(gsz, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).setScale(2);
        fundEntity.setFundCode(code);

        fundEntity.setFundName(fundJingZhiArr[0]);
        fundEntity.setJzrq(fundJingZhiArr[4]);
        fundEntity.setDwjz(dwjz.toString());
        fundEntity.setGsz(gsz.toString());
        fundEntity.setGszzl(gszzl.toString());
        fundEntity.setGztime(LocalDateTime.now().toString());
        return fundEntity;
    }

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public String getFundName() {
        return fundName;
    }

    public void setFundName(String fundName) {
        this.fundName = fundName;
    }

    public String getJzrq() {
        return jzrq;
    }

    public void setJzrq(String jzrq) {
        this.jzrq = jzrq;
    }

    public String getDwjz() {
        return dwjz;
    }

    public void setDwjz(String dwjz) {
        this.dwjz = dwjz;
    }

    public String getGsz() {
        return gsz;
    }

    public void setGsz(String gsz) {
        this.gsz = gsz;
    }

    public String getGszzl() {
        return gszzl;
    }

    public void setGszzl(String gszzl) {
        this.gszzl = gszzl;
    }

    public String getGztime() {
        return gztime;
    }

    public void setGztime(String gztime) {
        this.gztime = gztime;
    }

    public String getCostPrise() {
        return costPrise;
    }

    public void setCostPrise(String costPrise) {
        this.costPrise = costPrise;
    }

    public String getBonds() {
        return bonds;
    }

    public void setBonds(String bonds) {
        this.bonds = bonds;
    }

    public String getIncomePercent() {
        return incomePercent;
    }

    public void setIncomePercent(String incomePercent) {
        this.incomePercent = incomePercent;
    }

    public String getIncome() {
        return income;
    }

    public void setIncome(String income) {
        this.income = income;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    public String getCurrentDayJingzhi() {
        return currentDayJingzhi;
    }

    public void setCurrentDayJingzhi(String currentDayJingzhi) {
        this.currentDayJingzhi = currentDayJingzhi;
    }

    public String getPreviousDayJingzhi() {
        return previousDayJingzhi;
    }

    public void setPreviousDayJingzhi(String previousDayJingzhi) {
        this.previousDayJingzhi = previousDayJingzhi;
    }

    public String getOneYearAgoUpper() {
        return oneYearAgoUpper;
    }

    public void setOneYearAgoUpper(String oneYearAgoUpper) {
        this.oneYearAgoUpper = oneYearAgoUpper;
    }

    public String getOneSeasonAgoUpper() {
        return oneSeasonAgoUpper;
    }

    public void setOneSeasonAgoUpper(String oneSeasonAgoUpper) {
        this.oneSeasonAgoUpper = oneSeasonAgoUpper;
    }

    public String getOneMonthAgoUpper() {
        return oneMonthAgoUpper;
    }

    public void setOneMonthAgoUpper(String oneMonthAgoUpper) {
        this.oneMonthAgoUpper = oneMonthAgoUpper;
    }

    public String getOneWeekAgoUpper() {
        return oneWeekAgoUpper;
    }

    public void setOneWeekAgoUpper(String oneWeekAgoUpper) {
        this.oneWeekAgoUpper = oneWeekAgoUpper;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FundEntity fundEntity = (FundEntity) o;
        return Objects.equals(fundCode, fundEntity.fundCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fundCode);
    }

    @Override
    public String toString() {
        return "FundEntity{" +
            "fundCode='" + fundCode + '\'' +
            ", fundName='" + fundName + '\'' +
            ", jzrq='" + jzrq + '\'' +
            ", dwjz='" + dwjz + '\'' +
            ", gsz='" + gsz + '\'' +
            ", gszzl='" + gszzl + '\'' +
            ", gztime='" + gztime + '\'' +
            ", costPrise='" + costPrise + '\'' +
            ", bonds='" + bonds + '\'' +
            ", incomePercent='" + incomePercent + '\'' +
            ", income='" + income + '\'' +
            ", app='" + app + '\'' +
            ", hide='" + hide + '\'' +
            '}';
    }

    @Autowired
    private SinaRestClient sinaRestClient;

    @Autowired
    private TiantianFundRestClient tiantianFundRestClient;

    private static Gson gson = new Gson();
    @Autowired
    private FundMapper fundMapper;
    @Autowired
    private FundHisMapper fundHisMapper;
    @Autowired
    private FundJZMapper fundJZMapper;
    @Autowired
    private CacheService cacheService;

    public List<FundEntity> getFundDetails(List<String> codes) {
        List<FundEntity> funds = new ArrayList<>();
        List<String> codeList = new ArrayList<>();
        Map<String, String[]> codeMap = new HashMap<>();
        for (String str : codes) {
            String[] strArray;
            if (str.contains(",")) {
                strArray = str.split(",");
            } else {
                strArray = new String[] { str };
            }
            codeList.add(strArray[0]);
            codeMap.put(strArray[0], strArray);
        }

        for (String code : codeList) {
            try {
                String result = null;
                if (DateTimeUtils.isTradingTime()) {
                    result = tiantianFundRestClient.getFundInfo(code);
                } else {
                    result = cacheService.getFundInfoFromTiantianFund(code);
                }

                // 天天基金存在基金信息
                if (result != null && !result.equals("jsonpgz();")) {
                    String json = result.substring(8, result.length() - 2);
                    log.info("天天基金结果： {}", json);
                    if (!json.isEmpty()) {
                        FundEntity bean = gson.fromJson(json, FundEntity.class);
                        FundEntity.loadFund(bean, codeMap);

                        BigDecimal now = new BigDecimal(bean.getGsz());
                        String costPriceStr = bean.getCostPrise();
                        if (StringUtils.isNotEmpty(costPriceStr)) {
                            BigDecimal costPriceDec = new BigDecimal(costPriceStr);
                            BigDecimal incomeDiff = now.add(costPriceDec.negate());
                            if (costPriceDec.compareTo(BigDecimal.ZERO) <= 0) {
                                bean.setIncomePercent("0");
                            } else {
                                BigDecimal incomePercentDec = incomeDiff.divide(costPriceDec, 8, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.TEN)
                                    .multiply(BigDecimal.TEN)
                                    .setScale(3, RoundingMode.HALF_UP);
                                bean.setIncomePercent(incomePercentDec.toString());
                            }

                            String bondStr = bean.getBonds();
                            if (StringUtils.isNotEmpty(bondStr)) {
                                BigDecimal bondDec = new BigDecimal(bondStr);
                                BigDecimal incomeDec = incomeDiff.multiply(bondDec)
                                    .setScale(2, RoundingMode.HALF_UP);
                                bean.setIncome(incomeDec.toString());
                            }
                        }
                        List<FundJZPO> fundJZPOs = fundJZMapper.findResent380FundJZByCode(bean.getFundCode());
                        Optional<FundJZPO> optional = fundJZPOs.stream()
                            .filter(item -> item.getFSRQ().equals(bean.getGztime().substring(0, 10))).findAny();
                        // 当日净值已出
                        if (optional.isPresent()) {
                            FundJZPO currentDayFundJZPO = optional.get();
                            int currentDayIndex = fundJZPOs.indexOf(currentDayFundJZPO);
                            int previousDayIndex = currentDayIndex - 1;
                            FundJZPO previousDayFundJZPO = fundJZPOs.get(previousDayIndex);
                            bean.setCurrentDayJingzhi(currentDayFundJZPO.getDWJZ());
                            bean.setPreviousDayJingzhi(previousDayFundJZPO.getDWJZ());
                        }
                        getRecentDateUpper(fundJZPOs, bean);
                        funds.add(bean);
                        log.info("Fund编码:[" + code + "]信息：{}", bean);
                    } else {
                        log.info("Fund编码:[" + code + "]无法获取数据");
                    }
                    // 天天基金不存在基金信息，去新浪查找
                } else {
                    if (DateTimeUtils.isTradingTime()) {
                        result = sinaRestClient.getFundInfo(code);
                    } else {
                        result = cacheService.getFundInfoFromSina(code);
                    }
                    log.info("sina基金结果： {}", result);
                    FundEntity bean = FundEntity.loadFundFromSina(code, result);
                    FundEntity.loadFund(bean, codeMap);
                    BigDecimal now = new BigDecimal(bean.getGsz());
                    String costPriceStr = bean.getCostPrise();
                    if (StringUtils.isNotEmpty(costPriceStr)) {
                        BigDecimal costPriceDec = new BigDecimal(costPriceStr);
                        BigDecimal incomeDiff = now.add(costPriceDec.negate());
                        if (costPriceDec.compareTo(BigDecimal.ZERO) <= 0) {
                            bean.setIncomePercent("0");
                        } else {
                            BigDecimal incomePercentDec = incomeDiff.divide(costPriceDec, 8, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.TEN)
                                .multiply(BigDecimal.TEN)
                                .setScale(3, RoundingMode.HALF_UP);
                            bean.setIncomePercent(incomePercentDec.toString());
                        }

                        String bondStr = bean.getBonds();
                        if (StringUtils.isNotEmpty(bondStr)) {
                            BigDecimal bondDec = new BigDecimal(bondStr);
                            BigDecimal incomeDec = incomeDiff.multiply(bondDec)
                                .setScale(2, RoundingMode.HALF_UP);
                            bean.setIncome(incomeDec.toString());
                        }
                    }
                    List<FundJZPO> fundJZPOs = fundJZMapper.findResent380FundJZByCode(bean.getFundCode());
                    Optional<FundJZPO> optional = fundJZPOs.stream()
                        .filter(item -> item.getFSRQ().equals(bean.getGztime().substring(0, 10))).findAny();
                    // 当日净值已出
                    if (optional.isPresent()) {
                        FundJZPO currentDayFundJZPO = optional.get();
                        int currentDayIndex = fundJZPOs.indexOf(currentDayFundJZPO);
                        int previousDayIndex = currentDayIndex - 1;
                        FundJZPO previousDayFundJZPO = fundJZPOs.get(previousDayIndex);
                        bean.setCurrentDayJingzhi(currentDayFundJZPO.getDWJZ());
                        bean.setPreviousDayJingzhi(previousDayFundJZPO.getDWJZ());
                    }
                    getRecentDateUpper(fundJZPOs, bean);
                    funds.add(bean);
                    log.info("Fund编码:[" + code + "]信息：{}", bean);
                }
            } catch (Exception e) {
                log.info("Fund编码:[" + code + "]异常");
                e.printStackTrace();
            }
        }
        return funds;
    }

    public boolean saveFund(FundRequest fundRequest) {
//        String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        String username = UserUtils.getUsername();
        try {
            String result = tiantianFundRestClient.getFundInfo(fundRequest.getCode());
            FundEntity bean = null;
            if (result != null && !result.equals("jsonpgz();")) {
                String json = result.substring(8, result.length() - 2);
                log.info("天天基金结果： {}", json);
                bean = gson.fromJson(json, FundEntity.class);
            } else {
                result = sinaRestClient.getFundInfo(fundRequest.getCode());
                log.info("sina基金结果： {}", result);
                bean = FundEntity.loadFundFromSina(fundRequest.getCode(), result);
            }
            fundRequest.setName(bean.getFundName());
        } catch (Exception e) {
            log.info("获取基金信息异常 {}", e.getMessage());
            return false;
        }
        FundPO fundPOFromTable = fundMapper.findFundByCode(fundRequest.getCode(), username);
        if (fundPOFromTable != null) {
            fundHisMapper.saveFromFund(fundRequest.getCode(), username);
            fundMapper.updateFund(FundPO.builder().name(fundRequest.getName()).app(fundRequest.getApp()).bonds(fundRequest.getBonds())
                .code(fundRequest.getCode())
                .costPrise(fundRequest.getCostPrise()).hide(fundRequest.isHide()).build(), username);
        } else {
            fundMapper.save(FundPO.builder().name(fundRequest.getName()).app(fundRequest.getApp()).bonds(fundRequest.getBonds())
                .code(fundRequest.getCode())
                .costPrise(fundRequest.getCostPrise()).hide(fundRequest.isHide()).build(), username);
        }
        return true;
    }

    public void deleteFund(FundRequest fundRequest) {
//        String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        String username = UserUtils.getUsername();
        fundHisMapper.saveFromFund(fundRequest.getCode(), username);
        fundMapper.deleteFund(FundPO.builder().app(fundRequest.getApp()).bonds(fundRequest.getBonds()).code(fundRequest.getCode())
            .costPrise(fundRequest.getCostPrise()).hide(fundRequest.isHide()).build(), username);
    }

    public List<String> getFundList(String app) {
//        String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        String username = UserUtils.getUsername();
        return getFundList(app, username);
    }

    public List<String> getFundList(String app, String username) {
        List<FundPO> fund = fundMapper.findAllFund(app, username);
        log.info("APP: {} ,数据库中的基金为：{}", app, fund);
        if (fund == null || fund.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        for (FundPO fundPO : fund) {
            String fundArr = fundPO.getCode() + "," + fundPO.getCostPrise() + "," + fundPO.getBonds() + ","
                + fundPO.getApp();
            list.add(fundArr);
        }
        return list;
    }

    public List<FundHisPO> getFundHisList(String app, String code, String beginDate, String endDate) {
//        String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        String username = UserUtils.getUsername();
        List<FundHisPO> fundHis = fundHisMapper.findAllFundHis(app, code, beginDate, endDate, username);
        FundPO fundPO = fundMapper.findFundByCode(code, username);
        FundHisPO fundHisPO = FundHisPO.builder()
            .app(fundPO.getApp())
            .code(fundPO.getCode())
            .createDate(DateTimeUtils.getLocalDateTime())
            .bonds(fundPO.getBonds())
            .bondsChange("0")
            .costPrise(fundPO.getCostPrise())
            .costPriseChange(new BigDecimal("0"))
            .hide(fundPO.isHide())
            .name(fundPO.getName())
            .build();
        fundHis.add(0, fundHisPO);
        log.info("APP: {} ,数据库中的基金历史为：{}", app, fundHis);
        FundHisPO next = null;
        for (int i = 0; i < fundHis.size(); i++) {
            FundHisPO current = fundHis.get(i);
            if (i + 1 < fundHis.size()) {
                next = fundHis.get(i + 1);
            }
            if (next != null) {
                next.setBondsChange((new BigDecimal(current.getBonds()).subtract(new BigDecimal(next.getBonds()))).toString());
                next.setCostPriseChange(current.getCostPrise().subtract(next.getCostPrise()));
            }
        }
        return fundHis;
    }

    public List<SearchFundResult> searchFundByName(String name) {
        List<SearchFundResult> result = new ArrayList<>();
        String funds = cacheService.searchAllFundsFromEastMoney();
        funds = funds.replace("var r = ", "").replace(";", "");
        JSONArray jsonArray = JSONArray.parseArray(funds);

        // 遍历 JSONArray
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONArray innerArray = jsonArray.getJSONArray(i);
            // 判断内部数组的第三个元素是否包含搜索名称，或者第一个元素是否包括基金编码
            if (innerArray.getString(2).contains(name) || innerArray.getString(0).contains(name)) {
                SearchFundResult searchFundResult = new SearchFundResult();
                searchFundResult.setFundCode(innerArray.getString(0));
                searchFundResult.setFundName(innerArray.getString(2));
                result.add(searchFundResult);
            }
        }
        log.info("通过基金名称: {} 搜索到的结果为：{}", name, result);
        return result;
    }

    private void getRecentDateUpper(List<FundJZPO> fundJZPOs, FundEntity bean) {
        if (fundJZPOs == null || fundJZPOs.size() == 0) {
            bean.setOneYearAgoUpper("0.00");
            bean.setOneSeasonAgoUpper("0.00");
            bean.setOneMonthAgoUpper("0.00");
            bean.setOneWeekAgoUpper("0.00");
            return;
        }
        FundJZPO lastDateDayHistory = fundJZPOs.get(fundJZPOs.size() - 1);
        String latestDateStr = lastDateDayHistory.getFSRQ();
        LocalDate latestDate = LocalDate.parse(latestDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate oneYearAgoDate = latestDate.minusYears(1);
        FundJZPO oneYearAgoDateDayHistory = null;
        for (int i = fundJZPOs.size() - 1; i >= 0; i--) {
            FundJZPO current = fundJZPOs.get(i);
            LocalDate currentDate = LocalDate.parse(current.getFSRQ(), DateTimeFormatter.ISO_LOCAL_DATE);
            if (currentDate.compareTo(oneYearAgoDate) <= 0) {
                oneYearAgoDate = LocalDate.parse(current.getFSRQ(), DateTimeFormatter.ISO_LOCAL_DATE);
                oneYearAgoDateDayHistory = current;
                break;
            }
        }
        LocalDate oneSeasonAgoDate = latestDate.minusMonths(3);
        FundJZPO oneSeasonAgoDateDayHistory = null;
        for (int i = fundJZPOs.size() - 1; i >= 0; i--) {
            FundJZPO current = fundJZPOs.get(i);
            LocalDate currentDate = LocalDate.parse(current.getFSRQ(), DateTimeFormatter.ISO_LOCAL_DATE);
            if (currentDate.compareTo(oneSeasonAgoDate) <= 0) {
                oneSeasonAgoDate = LocalDate.parse(current.getFSRQ(), DateTimeFormatter.ISO_LOCAL_DATE);
                oneSeasonAgoDateDayHistory = current;
                break;
            }
        }
        LocalDate oneMonthAgoDate = latestDate.minusMonths(1);
        FundJZPO oneMonthAgoDateDayHistory = null;
        for (int i = fundJZPOs.size() - 1; i >= 0; i--) {
            FundJZPO current = fundJZPOs.get(i);
            LocalDate currentDate = LocalDate.parse(current.getFSRQ(), DateTimeFormatter.ISO_LOCAL_DATE);
            if (currentDate.compareTo(oneMonthAgoDate) <= 0) {
                oneMonthAgoDate = LocalDate.parse(current.getFSRQ(), DateTimeFormatter.ISO_LOCAL_DATE);
                oneMonthAgoDateDayHistory = current;
                break;
            }
        }
        LocalDate oneWeekAgoDate = latestDate.minusWeeks(1);
        FundJZPO oneWeekAgoDateDayHistory = null;
        for (int i = fundJZPOs.size() - 1; i >= 0; i--) {
            FundJZPO current = fundJZPOs.get(i);
            LocalDate currentDate = LocalDate.parse(current.getFSRQ(), DateTimeFormatter.ISO_LOCAL_DATE);
            if (currentDate.compareTo(oneWeekAgoDate) <= 0) {
                oneWeekAgoDate = LocalDate.parse(current.getFSRQ(), DateTimeFormatter.ISO_LOCAL_DATE);
                oneWeekAgoDateDayHistory = current;
                break;
            }
        }
        log.info("oneYearAgoDate is {}, oneSeasonAgoDate is {}, oneMonthAgoDate is {}, oneWeekAgoDate is {}", oneYearAgoDate,
            oneSeasonAgoDate, oneMonthAgoDate, oneWeekAgoDate);
        if (oneYearAgoDateDayHistory != null) {
            BigDecimal oneYearAgoUpper = (new BigDecimal(bean.getGsz() + ""))
                .subtract(new BigDecimal(oneYearAgoDateDayHistory.getDWJZ() + "")).multiply(new BigDecimal("100"))
                .divide((new BigDecimal(oneYearAgoDateDayHistory.getDWJZ() + "")), 2, BigDecimal.ROUND_UP);
            bean.setOneYearAgoUpper(oneYearAgoUpper + "");
        } else {
            bean.setOneYearAgoUpper("0.00");
        }
        if (oneYearAgoDateDayHistory != null) {
            BigDecimal oneSeasonAgoUpper = (new BigDecimal(bean.getGsz() + ""))
                .subtract(new BigDecimal(oneSeasonAgoDateDayHistory.getDWJZ() + "")).multiply(new BigDecimal("100"))
                .divide((new BigDecimal(oneSeasonAgoDateDayHistory.getDWJZ() + "")), 2, BigDecimal.ROUND_UP);
            bean.setOneSeasonAgoUpper(oneSeasonAgoUpper + "");
        } else {
            bean.setOneSeasonAgoUpper("0.00");
        }
        if (oneYearAgoDateDayHistory != null) {
            BigDecimal oneMonthAgoUpper = (new BigDecimal(bean.getGsz() + ""))
                .subtract(new BigDecimal(oneMonthAgoDateDayHistory.getDWJZ() + "")).multiply(new BigDecimal("100"))
                .divide((new BigDecimal(oneMonthAgoDateDayHistory.getDWJZ() + "")), 2, BigDecimal.ROUND_UP);
            bean.setOneMonthAgoUpper(oneMonthAgoUpper + "");
        } else {
            bean.setOneMonthAgoUpper("0.00");
        }
        if (oneYearAgoDateDayHistory != null) {
            BigDecimal oneWeekAgoUpper = (new BigDecimal(bean.getGsz() + ""))
                .subtract(new BigDecimal(oneWeekAgoDateDayHistory.getDWJZ() + "")).multiply(new BigDecimal("100"))
                .divide((new BigDecimal(oneWeekAgoDateDayHistory.getDWJZ() + "")), 2, BigDecimal.ROUND_UP);
            bean.setOneWeekAgoUpper(oneWeekAgoUpper + "");
        } else {
            bean.setOneWeekAgoUpper("0.00");
        }
        log.info("oneYearAgoUpper is {}, oneSeasonAgoUpper is {}, oneMonthAgoUpper is {}, oneWeekAgoUpper is {}", bean.getOneYearAgoUpper(),
            bean.getOneSeasonAgoUpper(), bean.getOneMonthAgoUpper(), bean.getOneWeekAgoUpper());
    }

}

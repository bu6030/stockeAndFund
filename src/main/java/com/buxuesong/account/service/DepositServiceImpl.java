package com.buxuesong.account.service;

import com.buxuesong.account.model.BuyOrSellStockRequest;
import com.buxuesong.account.model.FundBean;
import com.buxuesong.account.model.StockBean;
import com.buxuesong.account.model.TradingDateResponse;
import com.buxuesong.account.persist.dao.DepositMapper;
import com.buxuesong.account.persist.entity.Deposit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class DepositServiceImpl implements DepositService {

    private static final String GET_TRADE_DATE_URL = "http://www.szse.cn/api/report/exchange/onepersistenthour/monthList";

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private DepositMapper depositMapper;
    @Autowired
    private FundService fundService;
    @Autowired
    private StockService stockService;

    @Override
    public Deposit getDepositByDate(String date) {
        return depositMapper.findDepositByDate(date);
    }

    @Override
    public void deposit() {
        LocalDate date = LocalDate.now();
        if (!isTradingDate(date.toString())) {
            log.info("非交易日期，不统计");
            return;
        }

        Deposit deposit = depositMapper.findDepositByDate(date.toString());
        if (deposit != null) {
            log.info("已经存在当日盈利汇总： {}", deposit);
            return;
        }

        BigDecimal fundTotalDayIncome = depositFundDayIncome();
        BigDecimal stockTotalDayIncome = depositStockDayIncome();
        BigDecimal totalDayIncome = stockTotalDayIncome.add(fundTotalDayIncome);

        BigDecimal fundTotalMarketValue = depositFundMarketValue();
        BigDecimal stockTotalMarketValue = depositStockMarketValue();
        BigDecimal totalMarketValue = stockTotalMarketValue.add(fundTotalMarketValue);
        log.info("当日盈利: {}, 总市值: {}", totalDayIncome, totalMarketValue);
        depositMapper.save(Deposit
            .builder()
            .date(LocalDate.now().toString())
            .fundDayIncome(fundTotalDayIncome)
            .stockDayIncome(stockTotalDayIncome)
            .totalDayIncome(totalDayIncome)
            .fundMarketValue(fundTotalMarketValue)
            .stockMarketValue(stockTotalMarketValue)
            .totalMarketValue(totalMarketValue)
            .build());
    }

    @Override
    public void deleteDeposit() {
        LocalDate date = LocalDate.now();
        log.info("Delete deposit date : {}", date);
        depositMapper.deleteDeposit(date.toString());
    }

    @Override
    public List<Deposit> getDepositList(String beginDate, String endDate) {
        List<Deposit> list = depositMapper.getDepositList(beginDate, endDate);
        log.info("Get deposit list between {} and {} : {}", beginDate, endDate, list);
        return list;
    }

    private BigDecimal depositFundDayIncome() {
        List<String> fundListFrom = fundService.getFundList(null);
        List<FundBean> funds = fundService.getFundDetails(fundListFrom);
        BigDecimal fundTotalDayIncome = new BigDecimal("0");
        for (FundBean fund : funds) {
            BigDecimal dayIncome = (new BigDecimal(fund.getGszzl())
                .multiply(new BigDecimal(fund.getDwjz())).multiply(new BigDecimal(fund.getBonds()))
                .divide(new BigDecimal("100"))).setScale(2, BigDecimal.ROUND_HALF_UP);
            fundTotalDayIncome = fundTotalDayIncome.add(dayIncome);
        }
        log.info("基金当日盈利: {}", fundTotalDayIncome);
        return fundTotalDayIncome;
    }

    private BigDecimal depositStockDayIncome() {
        List<String> stockListFrom = stockService.getStockList(null);
        List<StockBean> stocks = stockService.getStockDetails(stockListFrom);
        BigDecimal stockTotalDayIncome = new BigDecimal("0");
        for (StockBean stock : stocks) {
            int maxBuyOrSellBonds = 0;
            BigDecimal todayBuyIncome = new BigDecimal("0");
            BigDecimal todaySellIncom = new BigDecimal("0");
            BigDecimal dayIncome = new BigDecimal("0");
            for (BuyOrSellStockRequest buyOrSellStockRequest : stock.getBuyOrSellStockRequestList()) {
                // 当天购买过
                if (buyOrSellStockRequest.getType().equals("1")) {
                    maxBuyOrSellBonds = maxBuyOrSellBonds + buyOrSellStockRequest.getBonds();
                    log.info("买入价格: {}", buyOrSellStockRequest.getPrice());
                    log.info("当前价格: {}", stock.getNow());
                    BigDecimal buyIncome = (new BigDecimal(stock.getNow()))
                        .subtract(new BigDecimal(buyOrSellStockRequest.getPrice() + ""))
                        .multiply(new BigDecimal(buyOrSellStockRequest.getBonds() + ""));
                    todayBuyIncome = todayBuyIncome.add(buyIncome);
                    log.info("买入收益： {}", todayBuyIncome);
                }
                // 当天卖出过
                if (buyOrSellStockRequest.getType().equals("2")) {
                    todaySellIncom = todaySellIncom.add(new BigDecimal(buyOrSellStockRequest.getIncome() + ""));
                    log.info("卖出收益： {}", todaySellIncom);
                }
            }
            log.info("买卖最大数: {}", maxBuyOrSellBonds);
            if (maxBuyOrSellBonds < Integer.parseInt(stock.getBonds())) {
                BigDecimal restBonds = (new BigDecimal(stock.getBonds())).subtract(new BigDecimal(maxBuyOrSellBonds + ""));
                log.info("剩余股数： {}", restBonds);
                dayIncome = (new BigDecimal(stock.getChange())).multiply(restBonds).setScale(2,
                    BigDecimal.ROUND_HALF_UP);
            } else {
                dayIncome = new BigDecimal("0").setScale(2,
                    BigDecimal.ROUND_HALF_UP);
                ;
            }
            dayIncome = dayIncome.add(todayBuyIncome).add(todaySellIncom);
            stockTotalDayIncome = stockTotalDayIncome.add(dayIncome);
        }
        log.info("股票当日盈利: {}", stockTotalDayIncome);
        return stockTotalDayIncome;

    }

    private BigDecimal depositFundMarketValue() {
        List<String> fundListFrom = fundService.getFundList(null);
        List<FundBean> funds = fundService.getFundDetails(fundListFrom);
        BigDecimal fundTotalMarketValue = new BigDecimal("0");
        for (FundBean fund : funds) {
            BigDecimal marketValue = (new BigDecimal(fund.getGsz())
                .multiply(new BigDecimal(fund.getBonds())).setScale(2, BigDecimal.ROUND_HALF_UP));
            fundTotalMarketValue = fundTotalMarketValue.add(marketValue);
        }
        log.info("基金总市值: {}", fundTotalMarketValue);
        return fundTotalMarketValue;
    }

    private BigDecimal depositStockMarketValue() {
        List<String> stockListFrom = stockService.getStockList(null);
        List<StockBean> stocks = stockService.getStockDetails(stockListFrom);
        BigDecimal stockTotalMarketValue = new BigDecimal("0");
        for (StockBean stock : stocks) {
            BigDecimal marketValue = (new BigDecimal(stock.getNow()).multiply(new BigDecimal(stock.getBonds()))).setScale(2,
                BigDecimal.ROUND_HALF_UP);
            stockTotalMarketValue = stockTotalMarketValue.add(marketValue);
        }
        log.info("股票总市值: {}", stockTotalMarketValue);
        return stockTotalMarketValue;

    }

    private boolean isTradingDate(String date) {
        List<TradingDateResponse.TradingDate> tradingDates = getTradingDate().getData();
        TradingDateResponse.TradingDate tradingDate = tradingDates.stream()
            .filter(s -> date.equals(s.getJyrq()))
            .findFirst().get();
        log.info("当天交易状态: {}", tradingDate);
        if ("1".equals(tradingDate.getJybz())) {
            return true;
        }
        return false;
    }

    @Override
    public TradingDateResponse getTradingDate() {
        log.info("获取交易日期，URL：{}", GET_TRADE_DATE_URL);
        ResponseEntity<TradingDateResponse> response = null;
        try {
            response = restTemplate.exchange(
                GET_TRADE_DATE_URL, HttpMethod.GET, null, TradingDateResponse.class);
        } catch (Exception e) {
            log.info("获取天天基金接口异常: {]", e);
            return null;
        }
        return response.getBody();
    }
}

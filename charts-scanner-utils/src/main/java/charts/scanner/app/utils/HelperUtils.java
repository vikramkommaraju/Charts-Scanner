package charts.scanner.app.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hp.gagawa.java.elements.A;

import charts.scanner.app.components.TickerQuote;
import charts.scanner.app.components.TickerQuoteResponse;
import charts.scanner.app.models.IBDRecord;
import charts.scanner.app.models.MatchingScansRecord;
import charts.scanner.app.models.PriceActionRecord;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;
import charts.scanner.app.models.StrategyHisoryRecord;
import charts.scanner.app.models.repositories.IBDRecordsRepository;
import charts.scanner.app.models.repositories.ScannedRecordsRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Contains all the helper functions
 * 
 * @author vkommaraju
 *
 */
@Component
@Slf4j
public class HelperUtils {

	@Autowired
	private ScannedRecordsRepository scansRepo;
	
	@Autowired
	private IBDRecordsRepository ibdRepo;
	
	
	private Map<String, String> exchangeMapping = ImmutableMap.of("NASD", "NASDAQ", 
			"NASDAQ", "NASDAQ", "NYSE", "NYSE");

	public String getToday(boolean isDateOnly) {
		return formatDate(getDate(), isDateOnly);
	}
	
	public String getFriendlyDate(String date, String timestamp) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SS yyyy-MM-dd");
			Date d = sdf.parse(timestamp+" "+date);
			SimpleDateFormat sdfs = new SimpleDateFormat("MM/dd hh:mm a");
			return sdfs.format(d);
		} catch (ParseException e) {
			return date+" "+timestamp;
		}
	}
	private String formatDate(Date d, boolean isDateOnly) {
		String format = isDateOnly ? "yyyy-MM-dd" : "HH:mm:ss.SS MM-dd-yyyy";
		return new SimpleDateFormat(format).format(d);
	}
	
	public String getPastDate(int lookbackDays) {
		return formatDate(DateUtils.addDays(getDate(),-lookbackDays), true);
	}
	
	public Map<String, List<ScanStrategy>> getRecordsToStrategiesMap() {
		return getRecordsToStrategiesMap(getToday(true), getToday(true));
	}
	
	public Date getDate() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
		Date currentDate = calendar.getTime();
		return currentDate;
	}
	
	public Map<String, List<ScanStrategy>> getRecordsToStrategiesMap(String startDate, String endDate) {
		List<ScannedRecord> scannedRecords = scansRepo.findAllRecordsByDateRange(startDate, endDate);
		Map<String, List<ScanStrategy>> matchedRecords = Maps.newConcurrentMap();
		for(ScannedRecord record : scannedRecords) {
			
			if(!matchedRecords.containsKey(record.getTicker())) {
				matchedRecords.put(record.getTicker(), Lists.newArrayList());
			}
			List<ScanStrategy> strategies = matchedRecords.get(record.getTicker());
			strategies.add(record.getStrategy());
			matchedRecords.put(record.getTicker(), strategies);		
		}
		return matchedRecords;
	}
	
	public Map<String, List<ScannedRecord>> getRecordHistoryByDate(String ticker, String startDate, String endDate) {
		Map<String, List<ScannedRecord>> dateToScansMap = Maps.newHashMap();
		List<ScannedRecord> records = scansRepo.findAllRecordsByTickerAndDateRange(ticker, startDate, endDate);
		for(ScannedRecord record : records) {
			
			if(!dateToScansMap.containsKey(record.getDateScanned())) {
				dateToScansMap.put(record.getDateScanned(), Lists.newArrayList());
			}
			
			List<ScannedRecord> scans = dateToScansMap.get(record.getDateScanned());
			scans.add(record);
			dateToScansMap.put(record.getDateScanned(), scans);
			
		}
		return dateToScansMap;
	}
	
	public boolean isExistingRecordForStrategy(String ticker, ScanStrategy strategy) {
		ScannedRecord exisingRecord = scansRepo.findRecordByDateAndTickerAndStrategy(getToday(true), ticker, strategy);
		return exisingRecord != null;
	}

	public double calculateYeild(double scannedPrice, Double currentPrice) {
		return ((currentPrice-scannedPrice)/(scannedPrice))*100;
	}
	
	public Map<String, TickerQuote> getTickerToQouteMap(TickerQuoteResponse response) {
		Map<String, TickerQuote> tickerToQuoteMap = Maps.newHashMap();
		for(TickerQuote quote : response.getStockQuotes()) {
			tickerToQuoteMap.put(quote.getSymbol(), quote);
		}
		return tickerToQuoteMap;
	}
	
	public Set<MatchingScansRecord> sortRecordsByMatchCount(Map<String, List<ScanStrategy>> matchedRecords) {
		Set<MatchingScansRecord> matchingRecordsInOrder = Sets.newTreeSet(Collections.reverseOrder());
		for(Entry<String, List<ScanStrategy>> matchedRecord : matchedRecords.entrySet()) {
			
			if(shouldSkipRecord(matchedRecords, matchedRecord)) {
				continue;
			}
			matchingRecordsInOrder.add(newMatchingRecord(matchedRecord));
		}
		return matchingRecordsInOrder;
	}
	private boolean shouldSkipRecord(Map<String, List<ScanStrategy>> matchedRecords,
			Entry<String, List<ScanStrategy>> matchedRecord) {
		return matchedRecords.get(matchedRecord.getKey()).size() < 2;
	}

	private MatchingScansRecord newMatchingRecord(Entry<String, List<ScanStrategy>> matchedRecord) {
		String ticker = matchedRecord.getKey();
		List<ScanStrategy> matchedStrategies = matchedRecord.getValue();
		int matchCount = matchedStrategies.size();
		
		return MatchingScansRecord.builder()
				.matchCount(matchCount)
				.ticker(ticker)
				.matchedStrategies(matchedStrategies)
				.build();
	}
	
	public PriorityQueue<PriceActionRecord> getPriorityQueueWithYield(List<ScannedRecord> records,
			TickerQuoteResponse response, double minYield) {
		PriorityQueue<PriceActionRecord> queue = new PriorityQueue<PriceActionRecord>();
		if (response.getStockQuotes() != null) {
			Map<String, TickerQuote> tickerToQouteMap = getTickerToQouteMap(response);
			for (String ticker : tickerToQouteMap.keySet()) {
				updateRecordAndAddToQueue(queue, tickerToQouteMap, getFirstRecordForTicker(ticker, records), minYield);
			}
		}
		return queue;
	}

	public ScannedRecord getFirstRecordForTicker(String ticker, List<ScannedRecord> records) {
		for (ScannedRecord record : records) {
			if (record.getTicker().equals(ticker)) {
				return record;
			}
		}
		return null;
	}

	public void updateRecordAndAddToQueue(PriorityQueue<PriceActionRecord> queue,
			Map<String, TickerQuote> tickerToQouteMap, ScannedRecord record, double minYield) {
		if(record == null) {
			return;
		}
		double scanPrice = record.getPrice();
		TickerQuote currentQuote = tickerToQouteMap.get(record.getTicker());
		if(scanPrice > 0.0 && currentQuote != null) {
			double currentPrice = currentQuote.getPrice();
			if (currentPrice > 0) { // For some reason the API returns 0 sometimes for a few tickers
				double yield = ((currentPrice - scanPrice) / scanPrice * 100);
				if(yield >= minYield) { // Records with more than min return
					queue.offer(PriceActionRecord.builder().ticker(record.getTicker()).scanPrice(record.getPrice())
							.yield(Double.valueOf(String.format("%.2f", yield))).scanDate(record.getDateScanned())
							.currentPrice(currentPrice).exchange(record.getExchange()).build());					
				}
 			} 
		}
	}

	public List<ScannedRecord> getRecordsForStrategySinceLastWeek(ScanStrategy strategy) {
		List<ScannedRecord> records = scansRepo.findAllRecordsByStrategy(getPastDate(7), getToday(true), strategy);
		return records;
	}
	
	public List<ScannedRecord> getRecordsForStrategyForToday(ScanStrategy strategy) {
		List<ScannedRecord> records = scansRepo.findAllRecordsByStrategy(getToday(true), getToday(true), strategy);
		return records;
	}
	
	public List<ScannedRecord> getRecordsForToday() {
		List<ScannedRecord> records = scansRepo.findAllRecordsByDate(getToday(true));
		return records;
	}
	
	public List<ScannedRecord> getRecordsForTheWeek() {
		List<ScannedRecord> records = scansRepo.findAllRecordsByDateRange(getPastDate(7), getToday(true));
		return records;
	}
	
	public String getExchangeMapping(String exchange) {
		return exchangeMapping.get(exchange);
	}
	
	
	public String getLinkForTicker(String exchange, String ticker) {
		return "<a href=\"https://www.tradingview.com/chart/?symbol="+getExchangeMapping(exchange)+":"+ticker+"\">Open</a>";
	}
	
	public String italisize(String text) {
		return "<i>"+text+"<//i>";
	}
	
	public String bold(String text) {
		return "<b>"+text+"<//b>";
	}
	
	public String getStrategyHistory(String ticker) {
		Map<String, List<ScannedRecord>> dateToScansMap = getRecordHistoryByDate(ticker, getPastDate(7), getToday(true));
		StringBuilder strBuilder = new StringBuilder();
		int dayIndex = 7;
		while(dayIndex > 0) {
			String key = getPastDate(dayIndex); // To traverse the map in time order since simply iterating the map wont guarantee time sequence
			if(dateToScansMap.containsKey(key)) {
				List<ScannedRecord> scansMatched = dateToScansMap.get(key);
				scansMatched.stream().forEach(rec -> {
					String scanDate = key;
					String strategy = rec.getStrategy().toString();
					String timestamp = rec.getTimestamp();
					String dateTime = getFriendlyDate(scanDate, timestamp);
					String price = "$"+rec.getPrice();
					strBuilder.append("["+strategy+" matched at " + dateTime +". Price was "+price+"]=========>>>>" + ", ");
				});
			}
			
			dayIndex--;
					
		}
		
		return strBuilder.toString();
	}
	
	public List<List<String>> getRowsFromQueue(PriorityQueue<PriceActionRecord> priorityQueue) {
		List<List<String>> allRows = Lists.newArrayList();
		while(!priorityQueue.isEmpty()) {
			PriceActionRecord record = priorityQueue.poll();
			List<String> reportRow = Lists.newArrayList();
			String ticker = record.getTicker();
			String dateScanned = record.getScanDate();
			Double scanPrice = record.getScanPrice();
			Double currentPrice = record.getCurrentPrice();
			Double yield = record.getYield();
			String strategyHistory = getStrategyHistory(ticker);
			reportRow.add(bold(ticker));
			reportRow.add(dateScanned);
			reportRow.add("$"+scanPrice+"");
			reportRow.add("$"+currentPrice+"");
			reportRow.add(bold(yield+"%"));
			reportRow.add(italisize(strategyHistory));
			reportRow.add(getLinkForTicker(record.getExchange(), record.getTicker()));
			allRows.add(reportRow);
		}
		
		return allRows;
	}
	
	public String getReportHeader(boolean isDaily) {
		return isDaily ? "Daily Leaderboard" : "Weekly Leaderboard";
	}
	
	public String getTrendingReportLabel(boolean isDaily) {
		return "These stocks have been trending " + (isDaily ? "Today!" : "This Week!");
	}
	
	public boolean isIBDStock(String ticker) {
		return ibdRepo.findRecordByTicker(ticker) != null;
	}
	
	public IBDRecord getIBDRecord(String ticker) {
		return ibdRepo.findRecordByTicker(ticker);
	}
}

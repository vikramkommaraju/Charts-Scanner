package charts.scanner.app.utils;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import charts.scanner.app.components.TickerQuote;
import charts.scanner.app.components.TickerQuoteResponse;
import charts.scanner.app.models.MatchingScansRecord;
import charts.scanner.app.models.PriceActionRecord;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;
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
	private ScannedRecordsRepository repository;
	
	public String getToday(boolean isDateOnly) {
		return formatDate(getDate(), isDateOnly);
	}
	
	private String formatDate(Date d, boolean isDateOnly) {
		String format = isDateOnly ? "yyyy-MM-dd" : "HH:mm:ss.SS MM-dd-yyyy ";
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
		List<ScannedRecord> scannedRecords = repository.findAllRecordsByDateRange(startDate, endDate);
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
	
	public boolean isExistingRecordForStrategy(String ticker, ScanStrategy strategy) {
		ScannedRecord exisingRecord = repository.findRecordByDateAndTickerAndStrategy(getToday(true), ticker, strategy);
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
				if(yield > minYield) { // Records with more than min return
					queue.offer(PriceActionRecord.builder().ticker(record.getTicker()).scanPrice(record.getPrice())
							.yield(Double.valueOf(String.format("%.2f", yield))).scanDate(record.getDateScanned())
							.build());					
				}
 			} 
		}
	}

	public List<ScannedRecord> getRecordsForStrategySinceLastWeek(ScanStrategy strategy) {
		List<ScannedRecord> records = repository.findAllRecordsByStrategy(getPastDate(7), getToday(true), strategy);
		return records;
	}
	
	public List<ScannedRecord> getRecordsForToday() {
		List<ScannedRecord> records = repository.findAllRecordsByDate(getToday(true));
		return records;
	}
	
	public List<ScannedRecord> getRecordsForTheWeek() {
		List<ScannedRecord> records = repository.findAllRecordsByDateRange(getPastDate(7), getToday(true));
		return records;
	}
	
}

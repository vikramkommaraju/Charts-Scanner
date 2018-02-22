package charts.scanner.app.utils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import charts.scanner.app.components.TickerQuote;
import charts.scanner.app.components.TickerQuoteResponse;
import charts.scanner.app.models.MatchingScansRecord;
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
		return formatDate(new Date(), isDateOnly);
	}
	
	private String formatDate(Date d, boolean isDateOnly) {
		String format = isDateOnly ? "yyyy-MM-dd" : "HH:mm:ss.SS MM-dd-yyyy ";
		return new SimpleDateFormat(format).format(d);
	}
	
	public String getPastDate(int lookbackDays) {
		return formatDate(DateUtils.addDays(new Date(),-lookbackDays), true);
	}
	
	public Map<String, List<ScanStrategy>> getRecordsToStrategiesMap() {
		return getRecordsToStrategiesMap(getToday(true), getToday(true));
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
}

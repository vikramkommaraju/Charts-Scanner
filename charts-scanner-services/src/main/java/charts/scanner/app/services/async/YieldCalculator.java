package charts.scanner.app.services.async;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import charts.scanner.app.components.TickerQuote;
import charts.scanner.app.components.TickerQuoteResponse;
import charts.scanner.app.models.PriceActionRecord;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;
import charts.scanner.app.models.YieldResult;
import charts.scanner.app.models.YieldResult.YieldResultBuilder;
import charts.scanner.app.models.repositories.ScannedRecordsRepository;
import charts.scanner.app.utils.HelperUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for calculating the yield. Invoked by YeildScheduler
 * 
 * @author vkommaraju
 *
 */
@Service
@Slf4j
public class YieldCalculator {

	@Autowired
	private HelperUtils utils;
	
	@Autowired
	private ScannedRecordsRepository repository;
	
	@Autowired
	private PriceLookupService priceService;
	
	private static final int QUEUE_SIZE = 5;
	
	@Async
	public CompletableFuture<YieldResult> calculate(ScanStrategy strategy) {
		
		YieldResultBuilder resultBuilder = YieldResult.builder().strategy(strategy);
		
		try {
			PriorityQueue<PriceActionRecord> queue = new PriorityQueue<PriceActionRecord>(QUEUE_SIZE);
			List<ScannedRecord> records = getRecordsForStrategySinceLastWeek(strategy);
			TickerQuoteResponse response = getPricesForRecords(records);
			calculateYieldAndPopulateQueue(queue, records, response);
			resultBuilder.queue(queue).foundRecords(records != null && records.size() > 0);
		} catch (Exception e) {
			log.info("Failed to calculate yield for : " + strategy);
			e.printStackTrace();
		}
		
		return CompletableFuture.completedFuture(resultBuilder.build());
		
	}

	private void calculateYieldAndPopulateQueue(PriorityQueue<PriceActionRecord> queue, List<ScannedRecord> records,
			TickerQuoteResponse response) {
		if(response.getStockQuotes() != null) {
			Map<String, TickerQuote> tickerToQouteMap = utils.getTickerToQouteMap(response);
			for(ScannedRecord record : records) {
				updateRecordAndAddToQueue(queue, tickerToQouteMap, record);
			}				
		}
	}

	private void updateRecordAndAddToQueue(PriorityQueue<PriceActionRecord> queue,
			Map<String, TickerQuote> tickerToQouteMap, ScannedRecord record) {
		double scanPrice = record.getPrice();
		TickerQuote currentQuote = tickerToQouteMap.get(record.getTicker());
		if(scanPrice > 0.0 && currentQuote != null) {
			double currentPrice = currentQuote.getPrice();
			double yield = ((currentPrice-scanPrice)/scanPrice * 100);
			if(queue.size() == 5) {
				queue.poll();
			}
			queue.add(PriceActionRecord.builder().ticker(record.getTicker())
					.scanPrice(record.getPrice())
					.yield(Double.valueOf(String.format("%.2f", yield)))
					.scanDate(record.getDateScanned()).build());
		}
	}

	private TickerQuoteResponse getPricesForRecords(List<ScannedRecord> records)
			throws InterruptedException, ExecutionException {
		Set<String> tickers = records.stream().map(record -> record.getTicker()).collect(Collectors.toSet());
		TickerQuoteResponse response = priceService.lookup(tickers).get();
		return response;
	}

	private List<ScannedRecord> getRecordsForStrategySinceLastWeek(ScanStrategy strategy) {
		List<ScannedRecord> records = repository.findAllRecordsByStrategy(utils.getPastDate(7), utils.getToday(true), strategy);
		return records;
	}
	
}

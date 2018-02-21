package charts.scanner.app.services.async;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import charts.scanner.app.components.TickerQuote;
import charts.scanner.app.components.TickerQuoteResponse;
import charts.scanner.app.models.ScanResult;
import charts.scanner.app.models.ScanResult.JobStage;
import charts.scanner.app.models.ScanResult.ScanResultBuilder;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;
import charts.scanner.app.models.repositories.ScannedRecordsRepository;
import charts.scanner.app.utils.HelperUtils;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ScannerService {

	@Autowired
	WebScraperService driverManager;
	
	@Autowired
	private PriceLookupService priceService;
	
	@Autowired
	private HelperUtils utils;
	
	@Autowired
	private ScannedRecordsRepository repository;
	
	@Async
	public CompletableFuture<ScanResult> scan(ScanStrategy strategy) {
		
		ScanResultBuilder resultBuilder = begin(strategy);
		try {
			compute(strategy, resultBuilder);
		} catch (Exception e) {
			log.info("Scan failed for : " + strategy);
			e.printStackTrace();
		} finally {
			end(strategy, resultBuilder);
		}
				
		return CompletableFuture.completedFuture(resultBuilder.build());
	}

	private void compute(ScanStrategy strategy, ScanResultBuilder resultBuilder) throws Exception {
		List<ScannedRecord> scannedRecords = runScan(strategy, resultBuilder);
		if(hasRecords(scannedRecords)) {
			TickerQuoteResponse response = lookup(resultBuilder, scannedRecords);
			List<ScannedRecord> recordsToSave = filter(resultBuilder, scannedRecords, response);
			
			if(hasRecords(recordsToSave)) {
				resultBuilder.isFoundNewRecords(true);
				log.info("["+strategy+"] found "+recordsToSave.size()+" records.");
				save(resultBuilder, recordsToSave);	
			}
		}
	}

	private boolean hasRecords(List<ScannedRecord> scanResult) {
		return scanResult.size() > 0;
	}

	private void end(ScanStrategy strategy, ScanResultBuilder resultBuilder) {
		resultBuilder.stage(JobStage.COMPLETED).isDone(true);
	}

	private ScanResultBuilder begin(ScanStrategy strategy) {
		ScanResultBuilder resultBuilder = ScanResult.builder().strategy(strategy);
		return resultBuilder;
	}

	private void save(ScanResultBuilder resultBuilder, List<ScannedRecord> recordsToSave) {
		resultBuilder.stage(JobStage.SAVING);
		repository.save(recordsToSave);
		resultBuilder.scannedRecords(recordsToSave);
	}

	private List<ScannedRecord> filter(ScanResultBuilder resultBuilder, List<ScannedRecord> scanResult,
			TickerQuoteResponse response) {
		resultBuilder.stage(JobStage.FILTERING);
		assignPricesToRecords(scanResult, response);
		List<ScannedRecord> newRecords = purgeLowPricedRecords(scanResult);
		List<ScannedRecord> recordsToSave = purgeExistingRecords(newRecords);
		return recordsToSave;
	}

	private TickerQuoteResponse lookup(ScanResultBuilder resultBuilder, List<ScannedRecord> scanResult) throws Exception {
		resultBuilder.stage(JobStage.PRICE_LOOKUP);
		Set<String> tickers = extractTickersFromResult(scanResult);
		TickerQuoteResponse response = priceService.lookup(tickers).get();
		return response;
	}

	private List<ScannedRecord> runScan(ScanStrategy strategy, ScanResultBuilder resultBuilder) throws Exception {
		resultBuilder.stage(JobStage.SCANNING);
		return driverManager.scrape(strategy).get();
	}
	
	private List<ScannedRecord> purgeLowPricedRecords(List<ScannedRecord> scanResult) {
		List<ScannedRecord> recordsToSave = scanResult.stream()
				.filter(record -> record.getPrice() >= 15)
				.collect(Collectors.toList());
		return recordsToSave;
	}

	private List<ScannedRecord> purgeExistingRecords(List<ScannedRecord> scannedRecords) {
		List<ScannedRecord> newRecords = Lists.newArrayList();
		for(ScannedRecord record : scannedRecords) {
			if(!utils.isExistingRecordForStrategy(record.getTicker(), record.getStrategy())) {
				newRecords.add(record);
			} 
		}
		return newRecords;
	}	
	
	private void assignPricesToRecords(List<ScannedRecord> scanResult, TickerQuoteResponse response) {
		Map<String, TickerQuote> tickerToQuoteMap = utils.getTickerToQouteMap(response);
		for(int i=0; i<scanResult.size(); i++) {
			ScannedRecord record = scanResult.get(i);
			TickerQuote quote = tickerToQuoteMap.get(record.getTicker());
			if(quote != null) {
				record.setPrice(quote.getPrice());
			}
		}
	}

	private Set<String> extractTickersFromResult(List<ScannedRecord> scanResult) {
		Set<String> tickers = scanResult.stream()
				.map(record -> record.getTicker())
				.collect(Collectors.toSet());
		return tickers;
	}
}

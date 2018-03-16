package charts.scanner.app.tasks.scheduling;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import charts.scanner.app.models.IBDRecord;
import charts.scanner.app.models.ScanResult;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;
import charts.scanner.app.services.MailContentGenerator;
import charts.scanner.app.services.MailSenderService;
import charts.scanner.app.services.ScannerService;
import charts.scanner.app.utils.HelperUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for scheduling periodic scans and sending a single batch notification email
 * 
 */
@Service
@Slf4j
public class ScanScheduler {

	@Autowired
	private ScannerService scanner;
	
	@Autowired
	private MailContentGenerator contentGenerator;
	
	@Autowired
	private MailSenderService mailerService;
	
	@Autowired
	private HelperUtils utils;
		
	@Scheduled(fixedRate = 5*60*1000)
	//@Scheduled(cron="0 0/5 7-15 * * ?") //Every 5 mins from 7AM-3PM
    public void schedule() throws InterruptedException {
		
		log.info("Scan Scheduler started at : " + utils.getDate());
		
		try {
			List<CompletableFuture<ScanResult>> allResults = runScans();
			List<String> emailContent = compileResultsIntoEmail(allResults);
			List<ScannedRecord> allScannedRecords = getAllScannedRecordsFromResults(allResults);
			checkIBDAndNotify(allScannedRecords);
		} catch (Exception e) {
			log.error("Failed to complete all scans. Reason: " + e.getMessage());
			e.printStackTrace();
		}
		
		log.info("Scan Scheduler ended at : " + utils.getDate());
	}

	private void sendNotification(String subject, String emailContent) throws Exception {
		if(emailContent.length() > 0) {
			notify(subject, emailContent);					
		} 
	}

	private String getScansSubject() {
		String subject = "["+utils.getToday(false)+"]  New Scans";
		return subject;
	}
	
	private String getIBDEmailSubject() {
		String subject = "["+utils.getToday(false)+"]  IBD Top 50 Stocks In Scans";
		return subject;
	}

	private List<CompletableFuture<ScanResult>> runScans() {
		List<CompletableFuture<ScanResult>> allResults = Lists.newArrayList();
		for(ScanStrategy strategy : ScanStrategy.values()) {			
			CompletableFuture<ScanResult> scanResult = scanner.scan(strategy);
			allResults.add(scanResult);
		}
		
		CompletableFuture.allOf(allResults.toArray(new CompletableFuture[allResults.size()]))
					    .join();
		return allResults;
	}
	
	private void checkIBDAndNotify(List<ScannedRecord> allScannedRecords) throws Exception {
		List<Pair<IBDRecord, ScanStrategy>> ibdScans = allScannedRecords.stream().filter(scannedRecord -> {
			return utils.isIBDStock(scannedRecord.getTicker());
		}).map(record -> {
			return Pair.of(utils.getIBDRecord(record.getTicker()), record.getStrategy());
		}).collect(Collectors.toList());
		
		Map<IBDRecord, List<ScanStrategy>> getIBDRecordToStrategiesMap = getMapping(ibdScans);
		
		for(Entry<IBDRecord, List<ScanStrategy>> entry : getIBDRecordToStrategiesMap.entrySet()) {
			IBDRecord record = entry.getKey();
			List<ScanStrategy> strategies = entry.getValue();
			String compositeRating = record.getCompositeRating();
			String epsRating = record.getEpsRating();
			String subject =  "["+utils.getToday(true)+"]" + record.getTicker() + " with IBD Ratings (" + compositeRating + ", " + epsRating + ") found!";
			List<List<String>> ratingsRows = getRowsFromIBDRecord(record);
			String ratingsInfo = contentGenerator.generate("Matched today in : " + strategies.toString(), "Next earnings are on: " + record.getEpsDue(), 
					ImmutableList.of("Ticker", 
							"Composite Rating", 
							"EPS Rating",
							"RS Rating",
							"SMR Rating",
							"ACC/DS Rating",
							"Earnings Growth (Latest Qtr to 3 Qtrs Ago)",
							"PE Ratio", 
							"Sales % latest Qtr", 
							"Sales growth last 3 years", 
							"Annual ROE",
							"Distance from Pivot",
							"Funds % Increase"), ratingsRows);
			
			Map<String, List<ScannedRecord>> dateToScansMap = utils.getRecordHistoryByDate(record.getTicker(), utils.getPastDate(10), utils.getToday(true));
			List<List<String>> strategiesRows = getRowsFromStrategyHistory(dateToScansMap);
			String strategyHistory = contentGenerator.generate("Strategy History", "The following strategies have been matched in the past week", 
					ImmutableList.of("Date", "Strategies Matched", "Price on the day"), strategiesRows);
			String chartLink = "Chart: " + utils.getLinkForTicker(record.getExchange(), record.getTicker());
			sendNotification(subject, ratingsInfo+"\n"+strategyHistory+"\n"+chartLink);
		}

	}

	

	private Map<IBDRecord, List<ScanStrategy>> getMapping(List<Pair<IBDRecord, ScanStrategy>> ibdScans) {
		Map<IBDRecord, List<ScanStrategy>> map = Maps.newHashMap();
		
		for(Pair<IBDRecord, ScanStrategy> pair : ibdScans) {
			IBDRecord record = pair.getLeft();
			ScanStrategy strategy = pair.getRight();
			
			if(!map.containsKey(record)) {
				map.put(record, Lists.newArrayList());
			}
			List<ScanStrategy> strategies = map.get(record);
			strategies.add(strategy);
			
			map.put(record, strategies);
		}
		return map;
	}

	@SuppressWarnings("finally")
	private List<String> compileResultsIntoEmail(List<CompletableFuture<ScanResult>> allResults) {
		List<String> emailContent = allResults.stream().filter(future -> {
			try {
				return future.get().isFoundNewRecords();
			} catch (Exception e1) { return false; }
		}).map(future -> {
			StringBuilder content = new StringBuilder();
			ScanResult result;
			try {
				result = future.get();
				if(result.isFoundNewRecords()) {
					ScanStrategy strategy = result.getStrategy();
					List<ScannedRecord> records = result.getScannedRecords();
					appendResults(content, strategy, records);					
				}
			} catch (Exception e) {
				log.info("Failed to parse scan result");
				e.printStackTrace();
			} finally {
				return content.toString();				
			}
		}).collect(Collectors.toList());
		return emailContent;
	}

	private List<ScannedRecord> getAllScannedRecordsFromResults(List<CompletableFuture<ScanResult>> allResults) {
		List<ScannedRecord> allRecords = allResults.stream().filter(future -> {
			try {
				return future.get().isFoundNewRecords();
			} catch (Exception e) {
				return false;
			}
		}).map(future -> {
			try {
				return future.get().getScannedRecords();
			} catch (Exception e) {
				return null;
			}
		}).flatMap(Collection::stream)
		.collect(Collectors.toList());
		
		return allRecords;
	}
	
	private void appendResults(StringBuilder content, ScanStrategy strategy, List<ScannedRecord> records) {
		List<List<String>> rowData = getRowsFromRecords(strategy, records);
		if(rowData.size() > 0) {
			content.append(contentGenerator.generate(strategy.toString(), getReportLabel(), 
					ImmutableList.of("Ticker", "Strategy History", "Chart"), rowData));						
		}
	}
	
	private String getReportLabel() {
		return "New scans found today";
	}

	private void notify(String subject, String content) throws Exception {
		mailerService.send(subject, content);	
	}

	private List<List<String>> getRowsFromRecords(ScanStrategy strategy, List<ScannedRecord> recordsToSave) {
		List<List<String>> allRows = Lists.newArrayList();
		String startDate = utils.getPastDate(7);
		String endDate = utils.getToday(true);
		Map<String, List<ScanStrategy>> recordsToStrategiesMap = utils.getRecordsToStrategiesMap(startDate, endDate);
		for(ScannedRecord record : recordsToSave) {
			List<String> reportRow = Lists.newArrayList();
			String ticker = record.getTicker();
			List<ScanStrategy> matchedStrategies = recordsToStrategiesMap.get(ticker);
			List<String> strategies = matchedStrategies.stream().map(str -> str.toString()).filter(str -> !str.equals(strategy.toString())).collect(Collectors.toList());
			reportRow.add(ticker);
			reportRow.add(strategies.size() > 0 ? strategies.toString() : "None matching");
			reportRow.add(utils.getLinkForTicker(record.getExchange(), record.getTicker()));
			allRows.add(reportRow);
		}
		return allRows;
	}
	
	private List<List<String>> getRowsFromIBDRecord(IBDRecord record) {
		List<List<String>> allRows = Lists.newArrayList();
		List<String> reportRow = Lists.newArrayList();
		
		reportRow.add(record.getTicker());
		reportRow.add(record.getCompositeRating());
		reportRow.add(record.getEpsRating());
		reportRow.add(record.getRsRating());
		reportRow.add(record.getSmrRating());
		reportRow.add(record.getAccDisRating());
		reportRow.add(record.getEarningsLastQtr()+","+record.getEarnings1QtrAgo()+","+record.getEarnings2QtrsAgo()+","+record.getEarnings3QtrsAgo());
		reportRow.add(record.getPe());
		reportRow.add(record.getSalesGrowthLastQtr());
		reportRow.add(record.getSalesGrowth3Yrs());
		reportRow.add(record.getAnnualRoe());
		reportRow.add(record.getPercentOffHigh());
		reportRow.add(record.getFundsIncreasePercent());
		
		allRows.add(reportRow);
		return allRows;		
	}
	
	private List<List<String>> getRowsFromStrategyHistory(Map<String, List<ScannedRecord>> dateToMatchedStrategies) {
		List<List<String>> allRows = Lists.newArrayList();
		SortedSet<String> dateKeySet = new TreeSet<String>(Collections.reverseOrder());
		dateKeySet.addAll(dateToMatchedStrategies.keySet());
		for (String dateScanned : dateKeySet) { 
			List<ScanStrategy> strategies = dateToMatchedStrategies.get(dateScanned).stream().map(record -> record.getStrategy()).collect(Collectors.toList());
			List<String> reportRow = Lists.newArrayList();
			reportRow.add(dateScanned);
			reportRow.add(strategies.toString());
			reportRow.add("$"+dateToMatchedStrategies.get(dateScanned).get(0).getPrice());
			allRows.add(reportRow);
		}
		
		return allRows;	
	}
}

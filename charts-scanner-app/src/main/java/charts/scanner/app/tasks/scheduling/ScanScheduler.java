package charts.scanner.app.tasks.scheduling;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import charts.scanner.app.models.ScanResult;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;
import charts.scanner.app.services.async.MailerService;
import charts.scanner.app.services.async.NewScansMailContentGenerator;
import charts.scanner.app.services.async.ScannerService;
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
	private NewScansMailContentGenerator contentGenerator;
	
	@Autowired
	private MailerService mailerService;
	
	@Autowired
	private HelperUtils utils;
		
	//@Scheduled(fixedRate = 5*60*1000)
	//@Scheduled(cron="0 0/5 7-15 * * ?") //Every 5 mins from 7AM-3PM
    public void schedule() throws InterruptedException {
		
		log.info("Scan Scheduler started at : " + utils.getDate());
		
		try {
			List<CompletableFuture<ScanResult>> allResults = runScans();
			List<String> emailContent = compileResultsIntoEmail(allResults);
			sendNotification(emailContent);
		} catch (Exception e) {
			log.error("Failed to complete all scans. Reason: " + e.getMessage());
		}
		
		log.info("Scan Scheduler ended at : " + utils.getDate());
	}

	private void sendNotification(List<String> emailContent) throws Exception {
		if(emailContent.size() > 0) {
			notify(getSubject(), emailContent.toString());					
		} 
	}

	private String getSubject() {
		String subject = "["+utils.getToday(false)+"]  New Scans";
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

	private void appendResults(StringBuilder content, ScanStrategy strategy, List<ScannedRecord> records) {
		List<List<String>> rowData = getRowsFromRecords(strategy, records);
		if(rowData.size() > 0) {
			content.append(contentGenerator.generate(strategy.toString(), rowData));						
		}
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
			allRows.add(reportRow);
		}
		return allRows;
	}

	@Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ScannerTask-");
        executor.initialize();
        return executor;
    }
	
}

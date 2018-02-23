package charts.scanner.app.tasks.scheduling;

import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import charts.scanner.app.models.PriceActionRecord;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.StrategyYieldResult;
import charts.scanner.app.services.async.MailerService;
import charts.scanner.app.services.async.StrategyYieldCalculator;
import charts.scanner.app.services.async.YieldMailContentGenerator;
import charts.scanner.app.utils.HelperUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for scheduling periodic yeild calculations
 *
 */
@Service
@Slf4j
public class StrategyYieldScheduler {
	
	@Autowired
	private YieldMailContentGenerator contentGenerator;
	
	@Autowired
	private MailerService mailerService;
	
	@Autowired
	private StrategyYieldCalculator calculator;
	
	@Autowired
	private HelperUtils utils;
	
	
	@Scheduled(fixedRate = 30*60*1000)
	//@Scheduled(cron="0 0/30 7-15 * * ?") //Every 30 mins from 7AM-3PM
    public void schedule() throws InterruptedException {
		log.info("Yield Scheduler started at : " + utils.getDate());
		
		try {
			List<CompletableFuture<StrategyYieldResult>> allResults = runYield();
			List<String> emailContent = compileResultsIntoEmail(allResults);
			sendNotification(emailContent);
		} catch (Exception e) {
			log.error("Failed to complete all yields. Reason: " + e.getMessage());
		}
		
		log.info("Yield Scheduler ended at : " + utils.getDate());
	}
	
	@SuppressWarnings("finally")
	private List<String> compileResultsIntoEmail(List<CompletableFuture<StrategyYieldResult>> allResults) {
		List<String> emailContent = allResults.stream().filter(future -> {
			try {
				return future.get().isFoundRecords();
			} catch (Exception e1) { return false; }
		}).map(future -> {
			StringBuilder content = new StringBuilder();
			StrategyYieldResult result;
			try {
				result = future.get();
				appendResults(content, result.getStrategy(), result.getQueue());
			} catch (Exception e) {
				log.info("Failed to parse yield result");
				e.printStackTrace();
			} finally {
				return content.toString();				
			}
		}).collect(Collectors.toList());
		return emailContent;
	}

	private void appendResults(StringBuilder content, ScanStrategy strategy, PriorityQueue<PriceActionRecord> priorityQueue) {
		List<List<String>> rowData = getRowsFromQueue(strategy, priorityQueue);
		if(rowData.size() > 0) {
			content.append(contentGenerator.generate(strategy.toString(), rowData));						
		}
	}

	private List<List<String>> getRowsFromQueue(ScanStrategy strategy, PriorityQueue<PriceActionRecord> priorityQueue) {
		List<List<String>> allRows = Lists.newArrayList();
		while(!priorityQueue.isEmpty()) {
			PriceActionRecord record = priorityQueue.poll();
			List<String> reportRow = Lists.newArrayList();
			String ticker = record.getTicker();
			String dateScanned = record.getScanDate();
			Double scanPrice = record.getScanPrice();
			Double yield = record.getYield();
			reportRow.add(ticker);
			reportRow.add(dateScanned);
			reportRow.add(scanPrice+"");
			reportRow.add(yield+"");
			allRows.add(reportRow);
		}
		
		return allRows;
	}

	private void sendNotification(List<String> emailContent) throws Exception {
		if(emailContent.size() > 1) {
			notify(getSubject(), emailContent.toString());					
		} 
	}

	private void notify(String subject, String content) throws Exception {
		mailerService.send(subject, content);	
	}
	
	private String getSubject() {
		String subject = "["+utils.getToday(false)+"]  Strategy Yield Report";
		return subject;
	}
	
	private List<CompletableFuture<StrategyYieldResult>> runYield() throws Exception {
		Thread.sleep(5000);
		List<CompletableFuture<StrategyYieldResult>> allResults = Lists.newArrayList();
		for(ScanStrategy strategy : ScanStrategy.values()) {			
			CompletableFuture<StrategyYieldResult> yeildResult = calculator.calculate(strategy);
			allResults.add(yeildResult);
		}
		
		CompletableFuture.allOf(allResults.toArray(new CompletableFuture[allResults.size()]))
					    .join();
		return allResults;
	}

}

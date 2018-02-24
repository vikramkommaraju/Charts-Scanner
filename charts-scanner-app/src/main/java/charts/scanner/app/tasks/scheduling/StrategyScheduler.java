package charts.scanner.app.tasks.scheduling;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import charts.scanner.app.models.PriceActionRecord;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;
import charts.scanner.app.models.StrategyYieldResult;
import charts.scanner.app.models.TrendingTodayResult;
import charts.scanner.app.services.async.MailContentGenerator;
import charts.scanner.app.services.async.MailerService;
import charts.scanner.app.services.async.StrategyYieldCalculator;
import charts.scanner.app.utils.HelperUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for scheduling periodic yeild calculations
 *
 */
@Service
@Slf4j
public class StrategyScheduler {
	
	@Autowired
	private MailContentGenerator contentGenerator;
	
	@Autowired
	private MailerService mailerService;
	
	@Autowired
	private StrategyYieldCalculator calculator;
	
	@Autowired
	private HelperUtils utils;
	
	
	@Scheduled(fixedRate = 2*60*60*1000)
	//@Scheduled(cron="0 0/30 7-15 * * ?") //Every 30 mins from 7AM-3PM
    public void schedule() throws InterruptedException {
		log.info("Yield Scheduler started at : " + utils.getDate());
		
		try {
			runDailyStartegyYeild();
			runWeeklyStartegyYield();
			
		} catch (Exception e) {
			log.error("Failed to complete all yields. Reason: " + e.getMessage());
		}
		
		log.info("Yield Scheduler ended at : " + utils.getDate());
	}
	
	private void runDailyStartegyYeild() throws Exception {
		run(true);
	}

	private void runWeeklyStartegyYield() throws Exception  {
		run(false);
	}

	private void run(boolean isDaily) throws Exception {
		List<CompletableFuture<StrategyYieldResult>> allResults = runYield(isDaily);
		List<String> emailContent = compileResultsIntoEmail(allResults, isDaily);
		String subject = getSubject(isDaily);
		sendNotification(subject, emailContent);
	}
	
	private String getSubject(boolean isDaily) {
		String subject = "["+utils.getToday(false)+"] "+ (isDaily ? "Daily Strategy Report" : "Weekly Strategy Report");
		return subject;
	}
	
	
	@SuppressWarnings("finally")
	private List<String> compileResultsIntoEmail(List<CompletableFuture<StrategyYieldResult>> allResults, boolean isDaily) {
		List<String> emailContent = allResults.stream().filter(future -> {
			try {
				return future.get().isFoundRecords();
			} catch (Exception e1) { return false; }
		}).map(future -> {
			StringBuilder content = new StringBuilder();
			StrategyYieldResult result;
			try {
				result = future.get();
				appendResults(content, result.getStrategy(), result.getQueue(), isDaily);
			} catch (Exception e) {
				log.info("Failed to parse yield result");
				e.printStackTrace();
			} finally {
				return content.toString();				
			}
		}).collect(Collectors.toList());
		return emailContent;
	}

	private void appendResults(StringBuilder content, ScanStrategy strategy, PriorityQueue<PriceActionRecord> priorityQueue, boolean isDaily) {
		List<List<String>> rowData = utils.getRowsFromQueue(priorityQueue);
		if(rowData.size() > 0) {
			content.append(contentGenerator.generate(strategy.toString(), getReportLabel(isDaily), rowData));
		}
	}
	
	private String getReportLabel(boolean isDaily) {
		if(isDaily) {
			return "These scans have shown a daily yield of at least " + getDailyYeild();
		} else {
			return "These scans have shown a weekly yield of at least " + getWeeklyYield();
		}
	}
	
	private double getWeeklyYield() {
		return 5.0;
	}

	private double getDailyYeild() {
		return 2.0;
	}

	private void sendNotification(String subject, List<String> emailContent) throws Exception {
		if(emailContent.size() > 0) {
			mailerService.send(subject, emailContent.toString());						
		} 
	}
	
	private List<CompletableFuture<StrategyYieldResult>> runYield(boolean isDaily) throws Exception {
		List<CompletableFuture<StrategyYieldResult>> allResults = Lists.newArrayList();
		for(ScanStrategy strategy : ScanStrategy.values()) {			
			CompletableFuture<StrategyYieldResult> yeildResult = calculator.calculate(strategy, isDaily, isDaily ? getDailyYeild() : getWeeklyYield());
			allResults.add(yeildResult);
		}
		
		CompletableFuture.allOf(allResults.toArray(new CompletableFuture[allResults.size()]))
					    .join();
		return allResults;
	}
}

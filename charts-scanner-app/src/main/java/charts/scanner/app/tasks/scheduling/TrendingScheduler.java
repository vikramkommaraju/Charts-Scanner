package charts.scanner.app.tasks.scheduling;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import charts.scanner.app.models.PriceActionRecord;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.TrendingTodayResult;
import charts.scanner.app.services.async.MailerService;
import charts.scanner.app.services.async.TrendingCalculator;
import charts.scanner.app.services.async.TrendingTodayMailContentGenerator;
import charts.scanner.app.utils.HelperUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for scheduling trending today calculator
 *
 */
@Service
@Slf4j
public class TrendingScheduler {
	
	@Autowired
	private TrendingTodayMailContentGenerator contentGenerator;
	
	@Autowired
	private MailerService mailerService;
	
	@Autowired
	private TrendingCalculator calculator;
	
	@Autowired
	private HelperUtils utils;
	
	
	//@Scheduled(fixedRate = 60*60*1000)
	//@Scheduled(cron="0 0/30 7-15 * * ?") //Every 30 mins from 7AM-3PM
    public void schedule() throws InterruptedException {
		
		log.info("Trending Scheduler started at : " + utils.getDate());
		
		try {
			runDailyTrending();
			runWeeklyTrending();
		} catch (Exception e) {
			log.error("Failed to run trending jobs. Reason: " + e.getMessage());
		}
		
		log.info("Trending Scheduler ended at : " + utils.getDate());
	}

	private void runDailyTrending() throws Exception {
		run(true);
	}
	
	private void runWeeklyTrending() throws Exception {
		run(false);
	}
	
	private void run(boolean isDaily) throws Exception {
		Thread.sleep(1000);
		CompletableFuture<TrendingTodayResult> result = runYield(isDaily);
		String emailContent = composeEmailFromResult(result);
		String subject = getSubject(isDaily);
		sendNotification(subject, emailContent);
	}

	private String getSubject(boolean isDaily) {
		String subject = "["+utils.getToday(false)+"]  Trending " + (isDaily ? "Today" : "This Week");
		return subject;
	}
	
	private String composeEmailFromResult(CompletableFuture<TrendingTodayResult> future) {
		try {
			TrendingTodayResult result = future.get();
			String content = result.isFoundRecords() ? getContent(result) : null;
			return content;
		} catch (Exception e) {
			return null;
		}
	}

	private String getContent(TrendingTodayResult result) {
		List<List<String>> rowData = getRowsFromQueue(result.getQueue(), result.isDaily());
		if(rowData.size() > 0) {
			return (contentGenerator.generate(result.isDaily() ? "Daily" : "Weekly" + " Leaderboard", rowData));						
		} else {
			return null;
		}
		
	}

	private List<List<String>> getRowsFromQueue(PriorityQueue<PriceActionRecord> priorityQueue, boolean isDaily) {
		List<List<String>> allRows = Lists.newArrayList();
		while(!priorityQueue.isEmpty()) {
			PriceActionRecord record = priorityQueue.poll();
			List<String> reportRow = Lists.newArrayList();
			String ticker = record.getTicker();
			Double scanPrice = record.getScanPrice();
			Double yield = record.getYield();
			Map<String, List<ScanStrategy>> recordsToStrategiesMap = isDaily ? utils.getRecordsToStrategiesMap() :
				utils.getRecordsToStrategiesMap(utils.getPastDate(7), utils.getToday(true));
			List<ScanStrategy> matchedStrategies = recordsToStrategiesMap.get(ticker);
			reportRow.add(ticker);
			reportRow.add(scanPrice+"");
			reportRow.add(yield+"");
			reportRow.add(matchedStrategies+"");
			reportRow.add(utils.getLinkForTicker(record.getExchange(), record.getTicker()));
			allRows.add(reportRow);
		}
		
		return allRows;
	}

	private void sendNotification(String subject, String emailContent) throws Exception {
		if(emailContent != null) {
			mailerService.send(subject, emailContent);					
		} 
	}

	private CompletableFuture<TrendingTodayResult> runYield(boolean isDaily) {
		CompletableFuture<TrendingTodayResult> result = calculator.calculate(isDaily);
		CompletableFuture.allOf(result).join();
		return result;
	}

}

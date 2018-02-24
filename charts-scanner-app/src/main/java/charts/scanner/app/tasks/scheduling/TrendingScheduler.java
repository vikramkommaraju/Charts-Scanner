package charts.scanner.app.tasks.scheduling;

import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import charts.scanner.app.models.PriceActionRecord;
import charts.scanner.app.models.TrendingTodayResult;
import charts.scanner.app.services.async.MailContentGenerator;
import charts.scanner.app.services.async.MailerService;
import charts.scanner.app.services.async.TrendingCalculator;
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
	private MailContentGenerator contentGenerator;
	
	@Autowired
	private MailerService mailerService;
	
	@Autowired
	private TrendingCalculator calculator;
	
	@Autowired
	private HelperUtils utils;
	
	
	@Scheduled(fixedRate = 60*60*1000)
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
		CompletableFuture<TrendingTodayResult> result = runYield(isDaily);
		String emailContent = composeEmailFromResult(result, isDaily);
		String subject = getSubject(isDaily);
		sendNotification(subject, emailContent);
	}

	private String getSubject(boolean isDaily) {
		String subject = "["+utils.getToday(false)+"]  Trending " + (isDaily ? "Today" : "This Week");
		return subject;
	}
	
	private String composeEmailFromResult(CompletableFuture<TrendingTodayResult> future, boolean isDaily) {
		try {
			TrendingTodayResult result = future.get();
			PriorityQueue<PriceActionRecord> queue = result.getQueue();
			if(result.isFoundRecords()) {
				return contentGenerator.generate(getReportTitle(isDaily), getReportLabel(isDaily), utils.getRowsFromQueue(queue));
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
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

	private String getReportTitle(boolean isDaily) {
		if(isDaily) {
			return "Daily Leaderboard";
		} else {
			return "Weekly Leaderboard";
		}
	}

	private void sendNotification(String subject, String emailContent) throws Exception {
		if(emailContent != null) {
			mailerService.send(subject, emailContent);					
		} 
	}

	private CompletableFuture<TrendingTodayResult> runYield(boolean isDaily) {
		CompletableFuture<TrendingTodayResult> result = calculator.calculate(isDaily, isDaily ? getDailyYeild() : getWeeklyYield());
		CompletableFuture.allOf(result).join();
		return result;
	}

}

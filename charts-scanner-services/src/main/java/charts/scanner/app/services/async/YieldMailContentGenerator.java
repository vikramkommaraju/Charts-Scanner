package charts.scanner.app.services.async;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import lombok.extern.slf4j.Slf4j;

/**
 * Content generator for yield email reports.
 * 
 * @author vkommaraju
 *
 */
@Service
@Slf4j
public class YieldMailContentGenerator extends MailContentGenerator {

	List<String> columnHeaders = ImmutableList.of("Ticker", "Date Scanned", "Scan Price", "Yield");
	
	@Override
	protected String getReportLabel() {
		return "Stocks Weekly Leaderboard (More than 2% yield)";
	}

	@Override
	protected List<String> getColumnHeaders() {
		return columnHeaders;
	}
}

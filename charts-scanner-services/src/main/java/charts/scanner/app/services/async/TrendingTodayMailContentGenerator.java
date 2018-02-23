package charts.scanner.app.services.async;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import lombok.extern.slf4j.Slf4j;

/**
 * Content generator for trending today email reports.
 * 
 * @author vkommaraju
 *
 */
@Service
public class TrendingTodayMailContentGenerator extends MailContentGenerator {

	List<String> columnHeaders = ImmutableList.of("Ticker", "Scan Price", "Yield", "Strategies Matched");
	
	@Override
	protected String getReportLabel() {
		return "These stocks have the best yields";
	}

	@Override
	protected List<String> getColumnHeaders() {
		return columnHeaders;
	}
}

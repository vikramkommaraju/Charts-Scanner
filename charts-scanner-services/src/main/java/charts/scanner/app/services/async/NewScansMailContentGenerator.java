package charts.scanner.app.services.async;

import java.util.List;

import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import lombok.extern.slf4j.Slf4j;

/**
 * Content generator for new scans
 * 
 * @author vkommaraju
 *
 */
@Service
@Slf4j
public class NewScansMailContentGenerator extends MailContentGenerator {

	List<String> columnHeaders = ImmutableList.of("Ticker", "Also Matched Within Past Week", "Chart");
	
	@Override
	protected String getReportLabel() {
		return "New scans found with this strategy";
	}

	@Override
	protected List<String> getColumnHeaders() {
		return columnHeaders;
	}
}

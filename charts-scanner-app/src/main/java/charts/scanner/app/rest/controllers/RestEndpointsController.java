package charts.scanner.app.rest.controllers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import charts.scanner.app.models.MatchingScansRecord;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;
import charts.scanner.app.models.repositories.ScannedRecordsRepository;
import charts.scanner.app.utils.HelperUtils;

/**
 * Exposes REST API end points for service usage and consumption
 * 
 * @author vkommaraju
 *
 */
@RestController
public class RestEndpointsController {
	
	@Autowired
	private ScannedRecordsRepository repository;
	
	@Autowired
	private HelperUtils utils;
	
	@RequestMapping("/")
	public String home(Map<String, Object> model) {
		return "index";
	}
	
	/**
	 * Get all scans for the given strategy
	 * Usage: /strategy?name=EMA_55_CROSSOVER
	 */
	@RequestMapping("/strategy")
	public List<ScannedRecord> scannedRecordsByStrategy(@RequestParam("name") ScanStrategy strategy) {
		return repository.findAllRecordsByDateAndStrategy(utils.getToday(true), strategy);
	}
	
	/**
	 * Get all scans that matched mulitple strategies. Order by match count
	 * Usage: /matching
	 */
	@RequestMapping("/matching")
	public Set<MatchingScansRecord> matchingScannedRecords() {
		Map<String, List<ScanStrategy>> matchedRecords = utils.getRecordsToStrategiesMap();
		Set<MatchingScansRecord> matchingRecordsInOrder = utils.sortRecordsByMatchCount(matchedRecords);
		return matchingRecordsInOrder;
	}
	
	/**
	 * Get info about a ticker
	 * Usage: /ticker?name=AAPL
	 */
	@RequestMapping("/ticker")
	public List<ScannedRecord> scannedRecordsByTicker(@RequestParam("name") String ticker) {
		return repository.findAllRecordsByTicker(ticker.toUpperCase());
	}
	
}

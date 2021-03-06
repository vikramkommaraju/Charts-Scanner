package charts.scanner.app.models;

import java.util.PriorityQueue;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Models the result of Yield calculator. Holds yields in a PriorityQueue
 * 
 * @author vkommaraju
 *
 */
@Component
@Data
@Builder @AllArgsConstructor @NoArgsConstructor
public class StrategyYieldResult {

	private ScanStrategy strategy;
	private PriorityQueue<PriceActionRecord> queue;
	private boolean foundRecords;
}

package charts.scanner.app.models;

import java.util.PriorityQueue;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Models the result of Trending today calculator. Holds results in a Priority Queue
 * 
 * @author vkommaraju
 *
 */
@Component
@Data
@Builder @AllArgsConstructor @NoArgsConstructor
public class TrendingTodayResult {

	private PriorityQueue<PriceActionRecord> queue;
	private boolean foundRecords;
	private boolean isDaily;
}

package charts.scanner.app.models;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a scanned record with all matching strategies
 *  
 * @author vkommaraju
 *
 */
@Component
@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class MatchingScansRecord implements Comparable<MatchingScansRecord> {

	private String ticker;
	private List<ScanStrategy> matchedStrategies;
	private int matchCount;
	
	@Override
	public int compareTo(MatchingScansRecord that) {
		return this.matchCount - that.matchCount;
	}
}

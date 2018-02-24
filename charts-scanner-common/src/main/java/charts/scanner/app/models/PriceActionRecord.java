package charts.scanner.app.models;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contains the calculated price yield for a given stock
 * 
 * @author vkommaraju
 *
 */
@Component
@Data
@Builder @AllArgsConstructor @NoArgsConstructor
public class PriceActionRecord implements Comparable<PriceActionRecord> {
	
	private String ticker;
	private String scanDate;
	private Double scanPrice;
	private Double currentPrice;
	private Double yield;
	private String exchange;
	
	@Override
	public int compareTo(PriceActionRecord that) {
		return that.yield.compareTo(this.yield) == 0 ? this.ticker.compareTo(that.ticker) : that.yield.compareTo(this.yield);
	}
	
}

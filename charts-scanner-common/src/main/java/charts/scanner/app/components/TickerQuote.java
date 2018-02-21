package charts.scanner.app.components;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modeled for JSON representation of a stock quote. This represents a single stock
 * quote that is contained in a list as part of {@link TickerQuoteResponse}
 * 
 * @author vkommaraju
 *
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TickerQuote {
	
	@JsonProperty("1. symbol")
	private String symbol;
	
	@JsonProperty("2. price")
	private double price;
}

package charts.scanner.app.components;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modeled for JSON representation of the response returned as part of call to RealTimePriceService
 * @author vkommaraju
 *
 */
@Component
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class TickerQuoteResponse {

	@JsonProperty("Meta Data")
	private Map<String, String> metaData;
	
	@JsonProperty("Stock Quotes")
	private List<TickerQuote> stockQuotes;
	
}

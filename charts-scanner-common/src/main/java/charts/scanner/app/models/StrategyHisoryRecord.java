package charts.scanner.app.models;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
@Data
@Builder @AllArgsConstructor @NoArgsConstructor
public class StrategyHisoryRecord {
	
	private ScanStrategy strategy;
	private String dateMatched;
	private double priceWhenMatched;

}

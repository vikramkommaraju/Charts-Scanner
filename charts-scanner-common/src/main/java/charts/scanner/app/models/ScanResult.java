package charts.scanner.app.models;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Models the response of the ScannerService
 * 
 * @author vkommaraju
 *
 */
@Component
@Data
@Builder @AllArgsConstructor @NoArgsConstructor
public class ScanResult {
	
	private ScanStrategy strategy;
	private List<ScannedRecord> scannedRecords;
	private JobStage stage;
	private boolean isDone;
	private boolean isFoundNewRecords;
	
	public enum JobStage {
		SCANNING,
		PRICE_LOOKUP,
		FILTERING,
		SAVING,
		COMPLETED
	}

}

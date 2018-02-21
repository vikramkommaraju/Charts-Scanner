package charts.scanner.app.models;

import java.util.PriorityQueue;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
@Data
@Builder @AllArgsConstructor @NoArgsConstructor
public class YieldResult {

	private ScanStrategy strategy;
	private PriorityQueue<PriceActionRecord> queue;
	private boolean foundRecords;
}

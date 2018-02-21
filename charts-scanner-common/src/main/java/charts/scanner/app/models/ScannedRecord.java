package charts.scanner.app.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder @NoArgsConstructor @AllArgsConstructor
public class ScannedRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	private String ticker;
	private String name;
	private String exchange;
	private String sector;
	private String industry;
	private String dateScanned;
	private String timestamp;
	private double price;
	private ScanStrategy strategy;
}

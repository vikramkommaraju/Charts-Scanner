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
public class IBDRecord {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	//Basic info
	String ticker;
	String name;
	String exchange;
	
	//Ratings
	String compositeRating;
	String epsRating;
	String rsRating;
	String smrRating;
	String accDisRating;
	
	//Earnings
	String epsDue;	
	String earningsLastQtr;
	String earnings1QtrAgo;
	String earnings2QtrsAgo;
	String earnings3QtrsAgo;
	String pe;
	
	//Sales and profit
	String salesGrowthLastQtr;
	String salesGrowth3Yrs;
	String annualRoe;

	//Price
	String percentOffHigh;
	
	//Holdings
	String fundsIncreasePercent;
	
	
}

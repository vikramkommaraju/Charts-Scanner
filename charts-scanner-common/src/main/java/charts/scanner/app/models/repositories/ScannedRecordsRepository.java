package charts.scanner.app.models.repositories;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;

/**
 * Interface to perform CRUD operations on {@link ScannedRecord}s
 * 
 * @author vkommaraju
 *
 */
public interface ScannedRecordsRepository extends CrudRepository<ScannedRecord, Long> {

	@Query("select record from ScannedRecord record where record.dateScanned = ?1 and record.ticker = ?2 and record.strategy = ?3")
	ScannedRecord findRecordByDateAndTickerAndStrategy(String dateInMMDDYYYFormat, String ticker, ScanStrategy strategy);
	
	@Query("select record from ScannedRecord record where record.dateScanned = ?1 and record.strategy = ?2")
	List<ScannedRecord> findAllRecordsByDateAndStrategy(String dateInMMDDYYYFormat, ScanStrategy strategy);
	
	@Query("select record from ScannedRecord record where record.dateScanned >= ?1 and record.dateScanned <= ?2")
	List<ScannedRecord> findAllRecordsByDateRange(String startDateInMMDDYYY, String endDateInMMDDYYYY);
	
	@Query("select record from ScannedRecord record where record.dateScanned >= ?1 and record.dateScanned <= ?2 and record.strategy = ?3")
	List<ScannedRecord> findAllRecordsByStrategy(String startDate, String endDate, ScanStrategy strategy);
	
	@Query("select record from ScannedRecord record where record.ticker = ?1")
	List<ScannedRecord> findAllRecordsByTicker(String ticker);
		
}

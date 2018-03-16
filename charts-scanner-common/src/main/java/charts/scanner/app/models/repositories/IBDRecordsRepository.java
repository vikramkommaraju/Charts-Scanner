package charts.scanner.app.models.repositories;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import charts.scanner.app.models.IBDRecord;
import charts.scanner.app.models.ScannedRecord;

/**
 * Interface to perform CRUD operations on {@link IBDRecord}
 * 
 * @author vkommaraju
 *
 */
public interface IBDRecordsRepository extends CrudRepository<IBDRecord, Long> {

	@Query("select record from IBDRecord record where record.ticker = ?1")
	IBDRecord findRecordByTicker(String ticker);
		
}

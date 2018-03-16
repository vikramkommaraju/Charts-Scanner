package charts.scanner.app.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import charts.scanner.app.models.IBDRecord;
import charts.scanner.app.models.repositories.IBDRecordsRepository;
import charts.scanner.app.utils.FileUtils;
import charts.scanner.app.utils.HelperUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IBDStocksLoader implements CommandLineRunner {

	@Autowired
	private FileUtils utils;
	
	@Autowired
	private HelperUtils helper;
	
	
	private final IBDRecordsRepository repository;
	
	@Autowired
	public IBDStocksLoader(IBDRecordsRepository repository) {
		this.repository = repository;
	}
	
	@Override
	public void run(String... strings) throws Exception {
		repository.deleteAll();
		List<IBDRecord> records = utils.getIBDList();
		log.info("Loaded record: " + records.get(0));
		repository.save(records);
	}
}
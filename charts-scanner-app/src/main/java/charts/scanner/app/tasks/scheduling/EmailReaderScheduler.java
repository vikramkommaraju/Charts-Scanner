package charts.scanner.app.tasks.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import charts.scanner.app.services.MailReader;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for periodically reading emails from inbox
 * 
 */
@Service
@Slf4j
public class EmailReaderScheduler {

	@Autowired
	MailReader reader;
	
	//@Scheduled(fixedRate = 5*60*1000)
	public void schedule() throws Exception {
		log.info("Checking inbox...");
		reader.read();
	}
}

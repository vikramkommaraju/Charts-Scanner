package charts.scanner.app.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import charts.scanner.app.models.IBDRecord;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper methods required parse and read files
 * 
 * @author vkommaraju
 *
 */
@Service
@Slf4j
public class FileUtils {

	public List<IBDRecord> getIBDList() throws IOException {
		String fileName = "/Users/vkommaraju/Downloads/ibd.csv";
		File file = new File(fileName);
	    List<String> lines = Files.readLines(file, Charset.defaultCharset());
	    List<IBDRecord> records = transformLinesToIBDRecords(lines);
	    return records;
	}
	
	private List<IBDRecord> transformLinesToIBDRecords(List<String> lines) {
		List<IBDRecord> records = Lists.newArrayList();
		for(int i=1; i<lines.size(); i++) {
			String line = lines.get(i);
			String[] columns = line.split(",");
			records.add(IBDRecord.builder()
					.ticker(columns[1].replaceAll("^\"|\"$", ""))
					.name(columns[2].replaceAll("^\"|\"$", ""))
					.compositeRating(columns[3].replaceAll("^\"|\"$", ""))
					.epsRating(columns[4].replaceAll("^\"|\"$", ""))
					.rsRating(columns[5].replaceAll("^\"|\"$", ""))
					.smrRating(columns[6].replaceAll("^\"|\"$", ""))
					.accDisRating(columns[7].replaceAll("^\"|\"$", ""))
					.epsDue(columns[8].replaceAll("^\"|\"$", ""))
					.earningsLastQtr(columns[9].replaceAll("^\"|\"$", ""))
					.earnings1QtrAgo(columns[10].replaceAll("^\"|\"$", ""))
					.earnings2QtrsAgo(columns[11].replaceAll("^\"|\"$", ""))
					.earnings3QtrsAgo(columns[12].replaceAll("^\"|\"$", ""))
					.earningGrowth3Yrs(columns[13].replaceAll("^\"|\"$", ""))
					.pe(columns[14].replaceAll("^\"|\"$", ""))
					.salesGrowthLastQtr(columns[15].replaceAll("^\"|\"$", ""))
					.salesGrowth3Yrs(columns[16].replaceAll("^\"|\"$", ""))
					.annualRoe(columns[17].replaceAll("^\"|\"$", ""))
					.percentOffHigh(columns[18].replaceAll("^\"|\"$", ""))
					.fundsIncreasePercent(columns[19].replaceAll("^\"|\"$", ""))
					.exchange(columns[20].replaceAll("^\"|\"$", ""))
					.build());
		}
		return records;
		
	}
}

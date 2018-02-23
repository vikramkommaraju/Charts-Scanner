package charts.scanner.app.services.async;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import charts.scanner.app.configuration.StockChartsConfig;
import charts.scanner.app.models.ScanStrategy;
import charts.scanner.app.models.ScannedRecord;
import charts.scanner.app.models.StockExchange;
import charts.scanner.app.utils.HelperUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to scrape StockCharts
 * 
 * @author vkommaraju
 *
 */
@Service
@Slf4j
public class WebScraperService {

	@Autowired
	private HelperUtils utils;
	
	@Autowired
	private StockChartsConfig config;
	
	private WebDriver driver;
	private boolean usePhantom = true;
	
	@PostConstruct
	private void login() {
		gotoLoginPage();
		enterUsernameAndPassword();
		clickSubmitButton();
	}

	@Async
	public CompletableFuture<List<ScannedRecord>> scrape(ScanStrategy strategy) throws Exception {
		WebDriver window = newWindow();
		List<ScannedRecord> result = null;
		try {
			goToStrategyPage(window, strategy);
			result = extractRecordsFromPageSoucre(window, strategy);
		} catch (Exception e) {
			throw e;
		} finally {
			window.quit();
		}
		return CompletableFuture.completedFuture(result);
	}

	private WebDriver newWindow() {
		WebDriver window = newDriver();
		window.get(config.getLoginUrl());
		for(Cookie cookie : driver.manage().getCookies()) {
			try {
				window.manage().addCookie(cookie);
			} catch (Exception e) {}
		}
		return window;
	}
	
	private WebDriver newDriver() {
		DesiredCapabilities capabilities = getCapabilities();
		WebDriver driver = createDriver(capabilities);
		setDriverProps(driver);
		return driver;	
	}

	private WebDriver createDriver(DesiredCapabilities capabilities) {
		if(usePhantom) {
			return new PhantomJSDriver(capabilities);			
		} else {
			File driverFile = new File("src/main/resources/chromedriver");
			System.setProperty("webdriver.chrome.driver", driverFile.getAbsolutePath());
	        ChromeOptions options = new ChromeOptions();
	        options.addArguments("headless");
			return new ChromeDriver(options);
		}
	}

	private void setDriverProps(WebDriver driver) {
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
	}

	private DesiredCapabilities getCapabilities() {
		DesiredCapabilities capabilities = null;
		String[] args = new  String[] {"--webdriver-loglevel=NONE"};
		Logger.getLogger(PhantomJSDriverService.class.getName()).setLevel(Level.OFF);
		
		if(usePhantom) {
			capabilities = DesiredCapabilities.phantomjs();
			capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, args);
			capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "src/main/resources/phantomjs");			
			capabilities.setJavascriptEnabled(true);
		} 
		return capabilities;
	}
	
	private void goToStrategyPage(WebDriver driver, ScanStrategy strategy) throws InterruptedException {
		String url = strategy.url();
		driver.navigate().to(url);
	}
	
	private List<ScannedRecord> extractRecordsFromPageSoucre(WebDriver window, ScanStrategy strategy) {
		WebElement table = getScanResultsTable(window);
		List<WebElement> tableRows = getTableRows(table);
		List<ScannedRecord> scannedRecords = createScannedRecordsFromRows(strategy, tableRows);
		return scannedRecords;
	}

	private List<ScannedRecord> createScannedRecordsFromRows(ScanStrategy strategy, List<WebElement> tableRows) {
		List<ScannedRecord> scannedRecords = Lists.newArrayList();		
		for(int i=1; i<tableRows.size(); i++) {
			List<WebElement> tableData = tableRows.get(i).findElements(By.tagName("td"));
			ScannedRecord record = createRecordFromTableData(tableData, strategy);
			if(!shouldFilterRecord(record)) {
				scannedRecords.add(record);				
			}
		}
		return scannedRecords;
	}

	private boolean shouldFilterRecord(ScannedRecord record) {
		return isNotNYSEOrNASD(record) ||
			   tickerHasSpecialChars(record);
	}

	private boolean tickerHasSpecialChars(ScannedRecord record) {
		return (record.getTicker().contains(".") || record.getTicker().contains("/"));
	}
	
	private boolean isNotNYSEOrNASD(ScannedRecord record) {
		return !(StockExchange.NYSE.toString().equals(record.getExchange()) ||
			   StockExchange.NASD.toString().equals(record.getExchange()));
	}

	private List<WebElement> getTableRows(WebElement table) {
		return table.findElements(By.tagName("tr"));
	}

	private WebElement getScanResultsTable(WebDriver driver) {
		return driver.findElement(By.id("scc-scans-resultstable"));
	}
	
	private ScannedRecord createRecordFromTableData(List<WebElement> tableData, ScanStrategy strategy) {
		return ScannedRecord.builder().dateScanned(utils.getToday(true))
				.ticker(getText(tableData, 1)).name(getText(tableData, 2))
				.exchange(getText(tableData, 3)).sector(getText(tableData, 4))
				.industry(getText(tableData, 5)).strategy(strategy)
				.timestamp(utils.getToday(false).split(" ")[0]).build();
	}

	private String getText(List<WebElement> tableData, int index) {
		String data = null;
		try {
			data = tableData.get(index).getText();
		} catch(Exception e) {}
		return data;
	}
	
	private void enterUsernameAndPassword() {
		WebElement username = driver.findElement(By.xpath("//*[@id=\"form_UserID\"]"));
		WebElement password = driver.findElement(By.xpath("//*[@id=\"loginform\"]/fieldset/div[2]/input"));
		username.sendKeys(config.getUserName());
		password.sendKeys(config.getPassword());
	}
	
	private void clickSubmitButton() {
		WebElement submitButton = driver.findElement(By.xpath("//*[@id=\"loginform\"]/fieldset/button"));
		submitButton.click();
	}
	
	private void gotoLoginPage() {
		if(driver == null) {
			driver = newDriver();
		}
		driver.get(config.getLoginUrl());
	}
	
}

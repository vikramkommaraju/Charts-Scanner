package charts.scanner.app.configuration;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration parameters common to the app
 * 
 * @author vkommaraju
 *
 */
@Configuration
@Getter @Setter
public class AppConfig {

	@Value("${system.sleep.short}") 
	private long shortSleep;
		
	@Value("${system.sleep.medium}") 
	private long mediumSleep;
	
	@Value("${system.sleep.long}") 
	private long longSleep;
	
	@Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("TaskExecutorPool-");
        executor.initialize();
        return executor;
    }
}

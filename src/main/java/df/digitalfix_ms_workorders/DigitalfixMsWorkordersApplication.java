package df.digitalfix_ms_workorders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"df.digitalfix_ms_workorders", "com.digitalfix.workorders"})
public class DigitalfixMsWorkordersApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigitalfixMsWorkordersApplication.class, args);
	}

}

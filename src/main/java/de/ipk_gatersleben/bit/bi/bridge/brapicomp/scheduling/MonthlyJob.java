package de.ipk_gatersleben.bit.bi.bridge.brapicomp.scheduling;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ipk_gatersleben.bit.bi.bridge.brapicomp.Config;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.testing.config.TestCollection;
import de.ipk_gatersleben.bit.bi.bridge.brapicomp.utils.RunnerService;

public class MonthlyJob implements org.quartz.Job {
	private static final Logger LOGGER = LogManager.getLogger(MonthlyJob.class.getName());
	
    public MonthlyJob() {
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
    	LOGGER.info("Monthly is executing.");
        ObjectMapper mapper = new ObjectMapper();

        InputStream inJson = TestCollection.class.getResourceAsStream("/collections/CompleteBrapiTest." + Config.get("scheduledVersion") + ".json");
        TestCollection tc;
		try {
			tc = mapper.readValue(inJson, TestCollection.class);
			RunnerService.TestAllEndpointsWithFreq(tc, "monthly");
		} catch (IOException | SQLException e1) {
			e1.printStackTrace();
		}
    }
}

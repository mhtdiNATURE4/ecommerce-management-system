package com.market.ecommerce.reports;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReportSchedulerCronTest {

    @Test
    public void cronNextRun_calculatesNext() {
        String cron = "0 0/5 * * * *"; // every 5 minutes
        CronExpression exp = CronExpression.parse(cron);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime next = exp.next(now.plusSeconds(1));
        assertNotNull(next);
    }
}

package com.chadtalty.pipeline;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LoggingSmokeTest {
    private static final Logger log = LoggerFactory.getLogger("com.chadtalty.pipeline.smoke");

    @Test
    void debugShouldPrint() {
        log.debug("DEBUG is ON");
        log.info("INFO is ON");
    }
}

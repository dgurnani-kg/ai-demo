package com.konrad.library.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Injected rather than called statically, so tests can pin time without mocking statics. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

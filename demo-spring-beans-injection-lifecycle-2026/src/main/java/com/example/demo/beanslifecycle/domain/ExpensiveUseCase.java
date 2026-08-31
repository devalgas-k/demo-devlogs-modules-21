package com.example.demo.beanslifecycle.domain;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ExpensiveUseCase {

    private final ObjectProvider<ExpensiveBean> expensiveBeanProvider;
    private final Clock clock;

    public ExpensiveUseCase(ObjectProvider<ExpensiveBean> expensiveBeanProvider, Clock clock) {
        this.expensiveBeanProvider = expensiveBeanProvider;
        this.clock = clock;
    }

    public RunResult run(long input) {
        Instant startAt = Instant.now(clock);

        long beforeGetNs = System.nanoTime();
        ExpensiveBean bean = expensiveBeanProvider.getObject();
        long afterGetNs = System.nanoTime();

        long beforeComputeNs = System.nanoTime();
        long output = bean.compute(input);
        long afterComputeNs = System.nanoTime();

        return new RunResult(
                startAt,
                bean.instanceNumber(),
                afterGetNs - beforeGetNs,
                afterComputeNs - beforeComputeNs,
                output
        );
    }

    public record RunResult(
            Instant startAt,
            int beanInstanceNumber,
            long getBeanDurationNs,
            long computeDurationNs,
            long output
    ) {
    }
}


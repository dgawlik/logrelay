package com.github.dgawlik.logrelay.app;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Service {

    private final Persistence persistence;

    private final Logger logger = org.slf4j.LoggerFactory.getLogger(Service.class);

    public Service(Persistence persistence) {
        this.persistence = persistence;
    }

    public List<Integer> factorizeNumber(int number) {
        logger.atInfo().addKeyValue("number", number)
                .log("Factorizing number {}", number);

        if (persistence.getResult(number) == null) {
            List<Integer> result = doFactorize(number);
            persistence.saveResult(number, result);
            return result;
        } else {
            return persistence.getResult(number);
        }
    }

    public List<Integer> doFactorize(int number) {
        int it = number;
        List<Integer> factors = new ArrayList<>();
        while (it > 1) {
            for (int i = 2; i <= it; i++) {
                if (it % i == 0) {
                    it /= i;
                    factors.add(i);
                    break;
                }
            }
        }

        return factors;
    }
}

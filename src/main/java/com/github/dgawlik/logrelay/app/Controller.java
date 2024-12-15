package com.github.dgawlik.logrelay.app;


import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.List;

public class Controller {

    private final Service service;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(Controller.class);

    public Controller (Service service) {
        this.service = service;
    }


    public List<Integer> factorizeNumber(int number, String username) {
        MDC.put("user", username);
        var result = service.factorizeNumber(number);
        logger.atInfo().addKeyValue("number", number)
                .addKeyValue("result", result)
                .log("Factorized number {} to {}", number, result);
        return result;
    }


}

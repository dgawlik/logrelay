package com.github.dgawlik.logrelay.app;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Persistence {

    private Logger logger = org.slf4j.LoggerFactory.getLogger(Persistence.class);
    private Map<Integer, List<Integer>> db = new HashMap<>();

    public void saveResult(int number, List<Integer> result) {
        db.put(number, result);
        logger.atInfo().addKeyValue("number", number)
                .log("Saved result for number {}", number);
    }

    public List<Integer> getResult(int number) {
        logger.atInfo().addKeyValue("number", number)
                .log("Retrieving result for number {}", number);

        if (db.containsKey(number)) {
            logger.atInfo().addKeyValue("number", number)
                    .log("Result present for number {}", number);
            return db.get(number);
        } else {
            logger.atInfo().log("No result found for number {}", number);
            return null;
        }
    }

}

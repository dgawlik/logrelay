package com.github.dgawlik.logrelay.app;

import org.slf4j.Logger;

import java.util.List;
import java.util.Random;

public class App {

    static String elasticApi = "ZVFKbXlwTUJoQUxDanpDMDVkNWs6TE55QnJNZHhRbS1fTDNjMHZ4SXp5Zw==";

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {

        var users = List.of("Alice", "Bob", "Charlie");

        for (int i=0;i<1000;i++){
            var number = new Random().nextInt(10000);
            aRequest(number, users.get(i % 3));
        }

    }

    private static void aRequest(int number, String forUser) {
        Persistence persistence = new Persistence();
        Service service = new Service(persistence);
        Controller controller = new Controller(service);
        controller.factorizeNumber(number, forUser);
    }
}

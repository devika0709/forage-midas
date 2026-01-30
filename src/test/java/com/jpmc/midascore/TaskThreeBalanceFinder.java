package com.jpmc.midascore;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
public class TaskThreeBalanceFinder {
    static final Logger logger = LoggerFactory.getLogger(TaskThreeBalanceFinder.class);

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private UserPopulator userPopulator;

    @Autowired
    private FileLoader fileLoader;

    @Autowired
    private UserRepository userRepository;

    @Test
    void find_waldorf_balance() throws InterruptedException {
        userPopulator.populate();
        String[] transactionLines = fileLoader.loadStrings("/test_data/mnbvcxz.vbnm");
        for (String transactionLine : transactionLines) {
            kafkaProducer.send(transactionLine);
        }
        Thread.sleep(2000);

        // Find waldorf's user record
        UserRecord waldorf = null;
        Iterable<UserRecord> allUsers = userRepository.findAll();
        for (UserRecord user : allUsers) {
            if ("waldorf".equals(user.getName())) {
                waldorf = user;
                break;
            }
        }

        if (waldorf != null) {
            float balance = waldorf.getBalance();
            int balanceRoundedDown = (int) Math.floor(balance);
            
            logger.info("==========================================================");
            logger.info("WALDORF'S FINAL BALANCE: {}", balance);
            logger.info("WALDORF'S BALANCE (ROUNDED DOWN): {}", balanceRoundedDown);
            logger.info("==========================================================");
            
            System.out.println("\n\n");
            System.out.println("==========================================================");
            System.out.println("ANSWER: " + balanceRoundedDown);
            System.out.println("==========================================================");
            System.out.println("\n\n");
        } else {
            logger.error("Waldorf user not found!");
        }
    }
}

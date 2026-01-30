package com.jpmc.midascore.listener;

import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {
    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);
    
    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core-group")
    public void listen(Transaction transaction) {
        logger.info("Received transaction: {}", transaction);
        logger.info("Transaction details - SenderId: {}, RecipientId: {}, Amount: {}", 
                    transaction.getSenderId(), 
                    transaction.getRecipientId(), 
                    transaction.getAmount());
        
        // For debugging purposes - this is where we'll set a breakpoint
        // or watch the logs to capture transaction amounts
        processTransaction(transaction);
    }
    
    private void processTransaction(Transaction transaction) {
        // This method is intentionally simple for Task 2
        // Future tasks will add processing logic here
        logger.debug("Processing transaction with amount: {}", transaction.getAmount());
    }
}

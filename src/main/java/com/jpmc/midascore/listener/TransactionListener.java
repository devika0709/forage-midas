package com.jpmc.midascore.listener;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Component
public class TransactionListener {
    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);
    private static final String INCENTIVE_API_URL = "http://localhost:9090/incentive";
    
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
    
    public TransactionListener(UserRepository userRepository, TransactionRepository transactionRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
    }
    
    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core-group")
    public void listen(Transaction transaction) {
        logger.info("Received transaction: {}", transaction);
        logger.info("Transaction details - SenderId: {}, RecipientId: {}, Amount: {}", 
                    transaction.getSenderId(), 
                    transaction.getRecipientId(), 
                    transaction.getAmount());
        
        processTransaction(transaction);
    }
    
    @Transactional
    private void processTransaction(Transaction transaction) {
        // Validate sender exists
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        if (sender == null) {
            logger.warn("Transaction rejected: Invalid sender ID {}", transaction.getSenderId());
            return;
        }
        
        // Validate recipient exists
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());
        if (recipient == null) {
            logger.warn("Transaction rejected: Invalid recipient ID {}", transaction.getRecipientId());
            return;
        }
        
        // Validate sender has sufficient balance
        if (sender.getBalance() < transaction.getAmount()) {
            logger.warn("Transaction rejected: Insufficient balance for sender {} (balance: {}, required: {})",
                    sender.getName(), sender.getBalance(), transaction.getAmount());
            return;
        }
        
        // All validations passed - process the transaction
        logger.info("Transaction valid - Processing...");
        
        // Call incentive API to get incentive amount
        float incentiveAmount = 0f;
        try {
            Incentive incentive = restTemplate.postForObject(INCENTIVE_API_URL, transaction, Incentive.class);
            if (incentive != null) {
                incentiveAmount = incentive.getAmount();
                logger.info("Incentive received: {}", incentiveAmount);
            }
        } catch (Exception e) {
            logger.error("Error calling incentive API: {}", e.getMessage());
            // Continue processing transaction even if incentive API fails
        }
        
        // Update balances
        // Deduct transaction amount from sender
        sender.setBalance(sender.getBalance() - transaction.getAmount());
        // Add transaction amount + incentive to recipient
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentiveAmount);
        
        // Save updated users
        userRepository.save(sender);
        userRepository.save(recipient);
        
        // Record transaction with incentive
        TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount(), incentiveAmount);
        transactionRepository.save(record);
        
        logger.info("Transaction processed successfully: {} sent {} to {} with incentive {} (New balances: sender={}, recipient={})",
                sender.getName(), transaction.getAmount(), recipient.getName(), incentiveAmount,
                sender.getBalance(), recipient.getBalance());
    }
}

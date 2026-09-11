package com.example.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    public boolean sendSms(String phoneNumber, String message) {
        // TODO: Integrate with actual SMS provider (Twilio, Vonage, etc.)
        log.info("Sending SMS to {}: {}", phoneNumber, message);
        return true;
    }
}
package com.example.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    public boolean sendSms(String phoneNumber, String message) {
        // TODO: Integrate with actual SMS provider (Twilio, Vonage, etc.)
        log.info("Sending SMS to {}: {}", phoneNumber, message);
        return true;
    }
}

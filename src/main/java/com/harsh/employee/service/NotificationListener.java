package com.harsh.employee.service;

import com.harsh.employee.model.OvertimeSettledEvent;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatchSettlementSms(OvertimeSettledEvent event) {
        try {
            // External SMS provider gateway call goes here
            // smsGateway.send(event.getPhoneNumber(), "Settled amount: " + event.getTotalSettledAmount());
        } catch (Exception ex) {
            // Log issues cleanly without triggering transaction rollbacks
        }
    }
}

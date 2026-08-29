package com.bcb.billing;

import com.bcb.billing.exceptions.InsufficientBalanceException;
import com.bcb.billing.exceptions.LimitExceededException;
import com.bcb.client.Client;
import com.bcb.domain.MessagePriority;
import com.bcb.domain.PlanType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BillingService {

    public void validateAndCharge(Client client, MessagePriority messagePriority) {
        BigDecimal cost = messagePriority.getCost();

        if (PlanType.PREPAID.equals(client.getPlanType())) {
            chargePrePaid(client, cost);
            return;
        }

        chargePostPaid(client, cost);
    }

    private void chargePostPaid(Client client, BigDecimal cost) {
        if (client.getMonthlyUsage().add(cost).compareTo(client.getMonthlyLimit()) > 0) {
            throw new LimitExceededException("Limite excedido");
        }

        client.setMonthlyUsage(client.getMonthlyUsage().add(cost));
    }

    private void chargePrePaid(Client client, BigDecimal cost) {
        if (client.getBalance().compareTo(cost) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente");
        }

        client.setBalance(client.getBalance().subtract(cost));

    }
}

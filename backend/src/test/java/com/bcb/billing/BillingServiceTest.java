package com.bcb.billing;

import com.bcb.billing.exceptions.InsufficientBalanceException;
import com.bcb.billing.exceptions.LimitExceededException;
import com.bcb.client.Client;
import com.bcb.domain.MessagePriority;
import com.bcb.domain.PlanType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillingServiceTest {

    private final BillingService billingService = new BillingService();

    @Test
    void prepaidComDezReaisEnviandoCincoMensagensNormaisFicaComOitoSetentaECinco() {
        Client client = Client.builder()
                .planType(PlanType.PREPAID)
                .balance(new BigDecimal("10.00"))
                .build();

        for (int i = 0; i < 5; i++) {
            billingService.validateAndCharge(client, MessagePriority.NORMAL);
        }

        assertThat(client.getBalance()).isEqualByComparingTo("8.75");
    }

    @Test
    void prepaidSemSaldoSuficienteBloqueiaSemDebitar() {
        Client client = Client.builder()
                .planType(PlanType.PREPAID)
                .balance(new BigDecimal("0.10"))
                .build();

        assertThatThrownBy(() -> billingService.validateAndCharge(client, MessagePriority.NORMAL))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(client.getBalance()).isEqualByComparingTo("0.10");
    }

    @Test
    void postpaidComLimiteDeCinquentaJaUsouQuarentaEnviandoDezMensagensNormaisRestaSeteECinquenta() {
        Client client = Client.builder()
                .planType(PlanType.POSTPAID)
                .monthlyLimit(new BigDecimal("50.00"))
                .monthlyUsage(new BigDecimal("40.00"))
                .build();

        for (int i = 0; i < 10; i++) {
            billingService.validateAndCharge(client, MessagePriority.NORMAL);
        }

        BigDecimal remaining = client.getMonthlyLimit().subtract(client.getMonthlyUsage());
        assertThat(remaining).isEqualByComparingTo("7.50");
    }

    @Test
    void postpaidExcedendoLimiteBloqueiaSemDebitar() {
        Client client = Client.builder()
                .planType(PlanType.POSTPAID)
                .monthlyLimit(new BigDecimal("50.00"))
                .monthlyUsage(new BigDecimal("49.90"))
                .build();

        assertThatThrownBy(() -> billingService.validateAndCharge(client, MessagePriority.NORMAL))
                .isInstanceOf(LimitExceededException.class);

        assertThat(client.getMonthlyUsage()).isEqualByComparingTo("49.90");
    }
}

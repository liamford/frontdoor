package com.payments.frontdoor.activities;


import com.payments.frontdoor.exception.PaymentProcessingException;
import com.payments.frontdoor.model.CrossBoarderActivityType;
import com.payments.frontdoor.model.CrossBoarderPaymentDetails;
import com.payments.frontdoor.model.PaymentIsoStatus;
import io.temporal.spring.boot.ActivityImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@ActivityImpl(workers = {"cb-payment-worker"})
public class CrossBoarderPaymentActivityImpl implements CrossBoarderPaymentActivity {

    private static final String CORRELATION_ID_HEADER = "x-correlation-id";

    private static final Map<CrossBoarderActivityType, ActivityConfig> ACTIVITY_CONFIGS = Map.of(
            CrossBoarderActivityType.DEBIT, new ActivityConfig("debit-error", PaymentIsoStatus.ACTC),
            CrossBoarderActivityType.DEBIT_COMPENSATION, new ActivityConfig("reverseDebit-error", PaymentIsoStatus.ACTC),
            CrossBoarderActivityType.RESERVE_CURRENCY, new ActivityConfig("reserve-error", PaymentIsoStatus.ACTC),
            CrossBoarderActivityType.RELEASE_CURRENCY, new ActivityConfig("release-error", PaymentIsoStatus.ACSC),
            CrossBoarderActivityType.SANCTIONS_CHECK, new ActivityConfig("check-error", PaymentIsoStatus.ACTC),
            CrossBoarderActivityType.TRANSFER_TO_CORRESPONDENT, new ActivityConfig("fund-error", PaymentIsoStatus.ACSC),
            CrossBoarderActivityType.RECALL_FUNDS, new ActivityConfig("recall-error", PaymentIsoStatus.ACSC),
            CrossBoarderActivityType.CREDIT_BENEFICIARY, new ActivityConfig("credit-error", PaymentIsoStatus.ACSC),
            CrossBoarderActivityType.REFUND_BENEFICIARY, new ActivityConfig("refund-error", PaymentIsoStatus.ACSC)
    );

    @Override
    public PaymentIsoStatus debitAccount(CrossBoarderPaymentDetails input) {
        return executeActivity(input, CrossBoarderActivityType.DEBIT, "Debiting account");
    }

    @Override
    public PaymentIsoStatus debitCompensation(CrossBoarderPaymentDetails input) {
        return executeActivity(input, CrossBoarderActivityType.DEBIT_COMPENSATION, "Reversing debit");
    }

    @Override
    public PaymentIsoStatus reserveCurrency(CrossBoarderPaymentDetails input) {
        return executeActivity(input, CrossBoarderActivityType.RESERVE_CURRENCY, "Reserving currency");
    }

    @Override
    public PaymentIsoStatus releaseCurrency(CrossBoarderPaymentDetails input) {
        return executeActivity(input, CrossBoarderActivityType.RELEASE_CURRENCY, "Releasing currency");
    }

    @Override
    public PaymentIsoStatus performSanctionsCheck(CrossBoarderPaymentDetails input) {
        return executeActivity(input, CrossBoarderActivityType.SANCTIONS_CHECK, "Performing sanctions check");
    }

    @Override
    public PaymentIsoStatus transferToCorrespondentBank(CrossBoarderPaymentDetails input) {
        return executeActivity(input, CrossBoarderActivityType.TRANSFER_TO_CORRESPONDENT, "Transferring to correspondent bank");
    }

    @Override
    public PaymentIsoStatus recallFunds(CrossBoarderPaymentDetails input) {
        return executeActivity(input, CrossBoarderActivityType.RECALL_FUNDS, "Recalling funds");
    }

    @Override
    public PaymentIsoStatus creditBeneficiary(CrossBoarderPaymentDetails input) {
        return executeActivity(input, CrossBoarderActivityType.CREDIT_BENEFICIARY, "Crediting beneficiary");
    }

    @Override
    public PaymentIsoStatus refundBeneficiary(CrossBoarderPaymentDetails input) {
        return executeActivity(input, CrossBoarderActivityType.REFUND_BENEFICIARY, "Refunding beneficiary");
    }

    private PaymentIsoStatus executeActivity(CrossBoarderPaymentDetails input, CrossBoarderActivityType activityType, String activityDescription) {
        log.info("Starting activity: {} for payment: {}", activityDescription, input.getPaymentId());

        try {
            String correlationId = getCorrelationId(input);
            ActivityConfig config = ACTIVITY_CONFIGS.get(activityType);
            boolean success = !correlationId.endsWith(config.errorSuffix());

            if (!success) {
                throw new PaymentProcessingException("Payment processing failed");
            }

            log.info("Completed activity: {} with status: {} for payment: {}",
                    activityDescription, config.successStatus(), input.getPaymentId());

            return config.successStatus();

        } catch (PaymentProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentProcessingException("Payment processing failed", e);
        }
    }

    private String getCorrelationId(CrossBoarderPaymentDetails input) {
        return Optional.ofNullable(input.getHeaders())
                .map(headers -> headers.get(CORRELATION_ID_HEADER))
                .orElseThrow(() -> new IllegalArgumentException("Correlation ID is required"));
    }


    private record ActivityConfig(String errorSuffix, PaymentIsoStatus successStatus) {}
}

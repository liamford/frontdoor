package com.payments.frontdoor.activity.unit;

import com.payments.frontdoor.activities.PaymentActivity;
import com.payments.frontdoor.activities.PaymentActivityImpl;
import com.payments.frontdoor.activities.PaymentStepStatus;
import com.payments.frontdoor.exception.PaymentAuthorizationFailedException;
import com.payments.frontdoor.model.PaymentAuthorizationResponse;
import com.payments.frontdoor.model.PaymentDetails;
import com.payments.frontdoor.model.PaymentInstruction;
import com.payments.frontdoor.service.PaymentApiConnector;
import com.payments.frontdoor.service.PaymentDispatcherService;
import com.payments.frontdoor.swagger.model.Account;
import io.temporal.testing.TestActivityExtension;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
class PaymentActivityTest {

    PaymentDispatcherService paymentDispatcherService = Mockito.mock(PaymentDispatcherService.class);
    PaymentApiConnector paymentApiConnector = Mockito.mock(PaymentApiConnector.class);



    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @RegisterExtension
    public  final TestActivityExtension activityExtension =
            TestActivityExtension.newBuilder()
                    .setActivityImplementations(new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService))
                    .build();

    private PaymentInstruction createPaymentInstruction() {
        Account debtor = new Account();
        debtor.setAccountNumber("123456789");
        debtor.setAccountName("John Doe");
        Account creditor = new Account();
        creditor.setAccountNumber("987654321");
        creditor.setAccountName("Jane Doe");
        return PaymentInstruction.builder()
                .paymentId("12345")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .debtor(debtor)
                .creditor(creditor)
                .paymentReference("REF123")
                .paymentDate(LocalDate.now())
                .headers(new HashMap<String, String>() {{
                    put("X-Idempotency-Key", "12345");
                    put("x-correlation-id", "TX-" + System.currentTimeMillis());
                }})
                .build();
    }

    @Test
    void testInitiatePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentDetails paymentDetails = PaymentDetails.builder()
                .paymentId("12345")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .paymentReference("REF123")
                .paymentDate(LocalDate.now())
                .build();

        PaymentInstruction response = activity.initiatePayment(paymentDetails);

        assertNotNull(response);
        assertEquals(paymentDetails.getPaymentId(), response.getPaymentId());
    }

    @Test
    void testManagePaymentOrder() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        boolean response = activity.managePaymentOrder(paymentInstruction);

        assertTrue(response);
    }

    @Test
    void testAuthorizePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();
        PaymentAuthorizationResponse authorizeResponse = PaymentAuthorizationResponse.builder()
                .status("success")
                .build();
        when(paymentApiConnector.callAuthorizePayment(paymentInstruction)).thenReturn(authorizeResponse);
        boolean response = activity.authorizePayment(paymentInstruction);

        assertTrue(response);
    }

    @Test
    void testUnAuthorizePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();
        PaymentAuthorizationResponse authorizeResponse = PaymentAuthorizationResponse.builder()
                .status("failed")
                .build();
        when(paymentApiConnector.callAuthorizePayment(paymentInstruction)).thenReturn(authorizeResponse);
        assertThrowsExactly(
                PaymentAuthorizationFailedException.class,
                () -> activity.authorizePayment(paymentInstruction)
        );

    }

    @Test
    void testExecutePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();
        PaymentDispatcherService paymentDispatcherService1 = Mockito.mock(PaymentDispatcherService.class);
        doNothing().when(paymentDispatcherService1).dispatchPayment(paymentInstruction, PaymentStepStatus.EXECUTED, null);
        PaymentStepStatus response = activity.executePayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentStepStatus.EXECUTED, response);
    }

    @Test
    void testClearAndSettlePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentStepStatus response = activity.clearAndSettlePayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentStepStatus.CLEARED, response);
    }

    @Test
    void testSendNotification() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentStepStatus response = activity.sendNotification(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentStepStatus.NOTIFIED, response);
    }

    @Test
    void testReconcilePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentStepStatus response = activity.reconcilePayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentStepStatus.RECONCILED, response);
    }

//    @Test
//    void testPostPayment() {
//        PaymentActivity activity = new PaymentActivityImpl(paymentDispatcherService);
//        PaymentInstruction paymentInstruction = createPaymentInstruction();
//
//        PaymentStepStatus response = activity.postPayment(paymentInstruction);
//
//        assertNotNull(response);
//        assertEquals(PaymentStepStatus.POSTED, response);
//    }

    @Test
    void testGenerateReports() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentStepStatus response = activity.generateReports(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentStepStatus.REPORTED, response);
    }

    @Test
    void testArchivePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentStepStatus response = activity.archivePayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentStepStatus.ARCHIVED, response);
    }

    @Test
    void testRefundPayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentApiConnector, paymentDispatcherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentStepStatus response = activity.refundPayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentStepStatus.REFUND, response);
    }
}
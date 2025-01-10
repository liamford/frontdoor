package com.payments.frontdoor.activity.unit;

import com.payments.frontdoor.activities.PaymentActivity;
import com.payments.frontdoor.activities.PaymentActivityImpl;
import com.payments.frontdoor.service.PaymentDispacherService;
import com.payments.frontdoor.swagger.model.Account;
import com.payments.frontdoor.swagger.model.PaymentResponse;
import io.temporal.testing.TestActivityExtension;
import model.PaymentDetails;
import model.PaymentInstruction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@RunWith(MockitoJUnitRunner.class)
 class PaymentActivityTest {

    PaymentDispacherService paymentDispacherService = Mockito.mock(PaymentDispacherService.class);



    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @RegisterExtension
    public  final TestActivityExtension activityExtension =
            TestActivityExtension.newBuilder()
                    .setActivityImplementations(new PaymentActivityImpl(paymentDispacherService))
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
                .build();
    }

    @Test
    void testInitiatePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
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
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        boolean response = activity.managePaymentOrder(paymentInstruction);

        assertTrue(response);
    }

    @Test
    void testAuthorizePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        boolean response = activity.authorizePayment(paymentInstruction);

        assertTrue(response);
    }

    @Test
    void testExecutePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();
        PaymentDispacherService paymentDispacherService1 = Mockito.mock(PaymentDispacherService.class);
        doNothing().when(paymentDispacherService1).dispatchPayment(anyString());
        PaymentResponse.StatusEnum response = activity.executePayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentResponse.StatusEnum.ACSP, response);
    }

    @Test
    void testClearAndSettlePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentResponse.StatusEnum response = activity.clearAndSettlePayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentResponse.StatusEnum.ACSC, response);
    }

    @Test
    void testSendNotification() {
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentResponse.StatusEnum response = activity.sendNotification(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentResponse.StatusEnum.ACSP, response);
    }

    @Test
    void testReconcilePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentResponse.StatusEnum response = activity.reconcilePayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentResponse.StatusEnum.ACSP, response);
    }

    @Test
    void testPostPayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentResponse.StatusEnum response = activity.postPayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentResponse.StatusEnum.ACSP, response);
    }

    @Test
    void testGenerateReports() {
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentResponse.StatusEnum response = activity.generateReports(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentResponse.StatusEnum.ACSP, response);
    }

    @Test
    void testArchivePayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentResponse.StatusEnum response = activity.archivePayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentResponse.StatusEnum.ACSP, response);
    }

    @Test
    void testRefundPayment() {
        PaymentActivity activity = new PaymentActivityImpl(paymentDispacherService);
        PaymentInstruction paymentInstruction = createPaymentInstruction();

        PaymentResponse.StatusEnum response = activity.refundPayment(paymentInstruction);

        assertNotNull(response);
        assertEquals(PaymentResponse.StatusEnum.RJCT, response);
    }
}
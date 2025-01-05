package model;


@lombok.Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Builder
public class WorkflowData {
    private String paymentReference;
    private String paymentAmount;
    private String paymentCurrency;
    private String paymentMethod;
    private String paymentStatus;

}

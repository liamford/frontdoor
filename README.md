# BIAN Payment Implementation with Temporal

This project demonstrates the implementation of BIAN payment workflows using Temporal in Java.

##  BIAN Payment Processing
BIAN (Banking Industry Architecture Network) provides a framework for standardizing banking services. This project implements BIAN payment workflows using Temporal to ensure reliable and scalable payment processing.  
Key Components
PaymentActivity: Contains the activities related to payment processing, such as sending notifications and updating payment status.
PaymentWorkflow: Defines the workflow for processing payments, including validation, execution, and notification steps.
RefundWorkflow: Defines the workflow for processing refunds, ensuring that payments can be reversed if necessary.
Payment Workflow Steps
Validation: Validate the payment details, including debtor and creditor information.
Execution: Execute the payment by transferring funds from the debtor's account to the creditor's account.

## Project Structure

- **Languages and Frameworks**: Java, Gradle
- **Testing Framework**: JUnit 5 - Gatling
- **Workflow Engine**: Temporal

## Prerequisites

- Java 21
- Gradle
- Docker
- Helm

## Setting Up Temporal

To set up Temporal using the Helm chart, follow these steps:

1. Add the Temporal Helm repository:

    ```sh
    helm repo add temporal https://charts.temporal.io
    ```

2. Update your Helm repositories:

    ```sh
    helm repo update
    ```

3. Install the Temporal Helm chart:

    ```sh
    helm install temporaltest temporal/temporal
    ```

## Getting Started

### Building the Project

To build the project, run the following command:

```sh
./gradlew build



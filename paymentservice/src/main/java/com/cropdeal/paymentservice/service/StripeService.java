//
//package com.cropdeal.paymentservice.service;
//
//import com.cropdeal.paymentservice.client.CropClient;
//import com.cropdeal.paymentservice.client.NotificationClient;
//import com.cropdeal.paymentservice.client.OrderClient;
//import com.cropdeal.paymentservice.dto.*;
//import com.cropdeal.paymentservice.repository.PaymentRepository;
//import com.stripe.Stripe;
//import com.stripe.exception.StripeException;
//import com.stripe.model.checkout.Session;
//import com.stripe.param.checkout.SessionCreateParams;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
//import io.github.resilience4j.retry.annotation.Retry;
//
//import java.time.LocalDate;
//
//@Service
//public class StripeService {
//
//    @Value("${stripe.api.key}")
//    private String secretKey;
//
//    private final PaymentRepository paymentRepository;
//    private final CropClient cropClient;
//    private final OrderClient orderClient;
//    private final NotificationClient notificationClient;
//
//    public StripeService(PaymentRepository paymentRepository, CropClient cropClient, OrderClient orderClient, NotificationClient notificationClient) {
//        this.paymentRepository = paymentRepository;
//        this.cropClient = cropClient;
//        this.orderClient = orderClient;
//        this.notificationClient = notificationClient;
//    }
//
//    @CircuitBreaker(name = "cropClientBreaker", fallbackMethod = "fallbackCrop")
//    @Retry(name = "cropClientBreaker", fallbackMethod = "fallbackCrop")
//    public Crop getCropByName(String cropName) {
//        return cropClient.getCropByName(cropName).getBody();
//    }
//
//    public Crop fallbackCrop(String cropName, Exception e) {
//        System.out.println("Fallback for crop service triggered: " + e.getMessage());
//        return null;
//    }
//
//    @CircuitBreaker(name = "orderClientBreaker", fallbackMethod = "fallbackOrder")
//    @Retry(name = "orderClientBreaker", fallbackMethod = "fallbackOrder")
//    public Order getOrderById(Long orderId) {
//        return orderClient.getOrderById(orderId).getBody();
//    }
//
//    public Order fallbackOrder(Long orderId, Exception e) {
//        System.out.println("Fallback for order service triggered: " + e.getMessage());
//        return null;
//    }
//
//    @CircuitBreaker(name = "notificationClientBreaker", fallbackMethod = "fallbackNotification")
//    @Retry(name = "notificationClientBreaker", fallbackMethod = "fallbackNotification")
//    public void sendPaymentEmail(EmailRequest emailRequest) {
//        notificationClient.sendPaymentEmail(emailRequest);
//    }
//
//    public void fallbackNotification(EmailRequest emailRequest, Exception e) {
//        System.out.println("Fallback for notification service triggered: " + e.getMessage());
//    }
//
//    public StripeResponse checkoutCrops(PurchaseRequest purchaseRequest) {
//        Stripe.apiKey = secretKey;
//
//        Crop crop = getCropByName(purchaseRequest.getCrop());
//
//        if (crop == null) {
//            return StripeResponse.builder()
//                    .status("FAILURE")
//                    .message("Crop not found!")
//                    .build();
//        }
//
//        Long cropId = crop.getId();
//
//        SessionCreateParams.LineItem.PriceData.ProductData productData =
//                SessionCreateParams.LineItem.PriceData.ProductData.builder()
//                        .setName(purchaseRequest.getCrop())
//                        .build();
//
//        SessionCreateParams.LineItem.PriceData cropPrice =
//                SessionCreateParams.LineItem.PriceData.builder()
//                        .setCurrency(purchaseRequest.getCurrency() == null ? "USD" : purchaseRequest.getCurrency())
//                        .setUnitAmount(purchaseRequest.getAmount())
//                        .setProductData(productData)
//                        .build();
//
//        SessionCreateParams.LineItem lineItem =
//                SessionCreateParams.LineItem.builder()
//                        .setQuantity((long) purchaseRequest.getQuantity())
//                        .setPriceData(cropPrice)
//                        .build();
//
//        SessionCreateParams params =
//                SessionCreateParams.builder()
//                        .setMode(SessionCreateParams.Mode.PAYMENT)
//                        .setSuccessUrl("http://localhost:8085/payment/success/id/" + cropId + "/order/" + purchaseRequest.getOrderId() + "?quantity=" + purchaseRequest.getQuantity())
//                        .setCancelUrl("http://localhost:8085/payment/cancel")
//                        .addLineItem(lineItem)
//                        .build();
//
//        try {
//            Session session = Session.create(params);
//            paymentRepository.save(purchaseRequest);
//
//            return StripeResponse.builder()
//                    .status("SUCCESS")
//                    .message("Payment session created successfully")
//                    .sessionId(session.getId())
//                    .sessionUrl(session.getUrl())
//                    .build();
//
//        } catch (StripeException e) {
//            return StripeResponse.builder()
//                    .status("FAILURE")
//                    .message("Stripe session creation failed: " + e.getMessage())
//                    .build();
//        }
//    }
//
//    public String deleteCropById(Long id) {
//        return cropClient.deleteCrop(id).getBody();
//    }
//
//    public String handlePaymentSuccess(Long cropId, Long orderId, int quantity) {
//        cropClient.reduceAndDelete(cropId, quantity);
//        orderClient.updateOrderStatus(orderId, "SUCCESS");
//
//        Crop crop = cropClient.getCropById(cropId).getBody();
//        Order order = orderClient.getOrderById(orderId).getBody();
//        PurchaseRequest payment = paymentRepository.findByOrderId(orderId);
//
//        double amount = crop.getPricePerKg() * quantity;
//        String transactionId = payment.getId().toString();
//
//        EmailRequest emailRequest = new EmailRequest();
//        emailRequest.setTo(order.getDealerEmail());
//        emailRequest.setCropName(crop.getName());
//        emailRequest.setAmount(amount);
//        emailRequest.setTransactionId(transactionId);
//        emailRequest.setPaymentDate(LocalDate.now().toString());
//        emailRequest.setQuantity(quantity);
//        emailRequest.setCurrency(payment.getCurrency());
//
//        sendPaymentEmail(emailRequest);
//
//        return "✅ Payment processed successfully. Order status updated and a confirmation email has been sent.";
//    }
//}






package com.cropdeal.paymentservice.service;

import com.cropdeal.paymentservice.client.CropClient;
import com.cropdeal.paymentservice.client.NotificationClient;
import com.cropdeal.paymentservice.client.OrderClient;
import com.cropdeal.paymentservice.dto.*;
import com.cropdeal.paymentservice.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String secretKey;

    private final PaymentRepository paymentRepository;
    private final CropClient cropClient;
    private final OrderClient orderClient;
    private final NotificationClient notificationClient;

    public StripeService(PaymentRepository paymentRepository, CropClient cropClient, OrderClient orderClient, NotificationClient notificationClient) {
        this.paymentRepository = paymentRepository;
        this.cropClient = cropClient;
        this.orderClient = orderClient;
        this.notificationClient = notificationClient;
    }

    // Implementing the Circuit Breaker for Crop Client
    @CircuitBreaker(name = "cropClientBreaker", fallbackMethod = "fallbackCrop")
    @Retry(name = "cropClientBreaker", fallbackMethod = "fallbackCrop")
    public Crop getCropByName(String cropName) {
        return cropClient.getCropByName(cropName).getBody();
    }

    public Crop fallbackCrop(String cropName, Exception e) {
        System.out.println("⚠️ Fallback triggered for getCropByName: " + e.getMessage());
        return null;
    }

    public StripeResponse checkoutCrops(PurchaseRequest purchaseRequest) {
        Stripe.apiKey = secretKey;

        Crop crop = getCropByName(purchaseRequest.getCrop());

        // ✅ Handle fallback result to avoid 500 error
        if (crop == null) {
        System.out.println("❌ Crop is null — returning fallback response.");
        return StripeResponse.builder()
        .status("FAILURE")
        .message("Crop not found or crop-service is unavailable. Please try again later.")
        .build();
        }

        Long cropId = crop.getId();

        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(purchaseRequest.getCrop())
                        .build();

        SessionCreateParams.LineItem.PriceData cropPrice =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(purchaseRequest.getCurrency() == null ? "USD" : purchaseRequest.getCurrency())
                        .setUnitAmount(purchaseRequest.getAmount())
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setQuantity((long) purchaseRequest.getQuantity())
                        .setPriceData(cropPrice)
                        .build();

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("http://localhost:8085/payment/success/id/" + cropId + "/order/" + purchaseRequest.getOrderId() + "?quantity=" + purchaseRequest.getQuantity())
                        .setCancelUrl("http://localhost:8085/payment/cancel")
                        .addLineItem(lineItem)
                        .build();

        try {
            Session session = Session.create(params);
            paymentRepository.save(purchaseRequest);

            return StripeResponse.builder()
                    .status("SUCCESS")
                    .message("Payment session created successfully")
                    .sessionId(session.getId())
                    .sessionUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {
            return StripeResponse.builder()
                    .status("FAILURE")
                    .message("Stripe session creation failed: " + e.getMessage())
                    .build();
        }
    }

    public String deleteCropById(Long id) {
        return cropClient.deleteCrop(id).getBody();
    }

    public String handlePaymentSuccess(Long cropId, Long orderId, int quantity) {
        cropClient.reduceAndDelete(cropId, quantity);
        orderClient.updateOrderStatus(orderId, "SUCCESS");

        Crop crop = cropClient.getCropById(cropId).getBody();
        Order order = orderClient.getOrderById(orderId).getBody();
        PurchaseRequest payment = paymentRepository.findByOrderId(orderId);

        double amount = crop.getPricePerKg() * quantity;
        String transactionId = payment.getId().toString();

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(order.getDealerEmail());
        emailRequest.setCropName(crop.getName());
        emailRequest.setAmount(amount);
        emailRequest.setTransactionId(transactionId);
        emailRequest.setPaymentDate(LocalDate.now().toString());
        emailRequest.setQuantity(quantity);
        emailRequest.setCurrency(payment.getCurrency());

        notificationClient.sendPaymentEmail(emailRequest);

        return "✅ Payment processed successfully. Order status updated and a confirmation email has been sent.";
    }
}

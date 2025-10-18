public class PaymentProcess implements IPaymentProcess {
    @Override
    public Payment processPayment(Order order, double paid) {
        return new Payment(Integer.parseInt(order.getOrderId()), order.getTotal(), paid);
    }
}

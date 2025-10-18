public interface IPaymentProcess {
    Payment processPayment(Order order, double paid);
}

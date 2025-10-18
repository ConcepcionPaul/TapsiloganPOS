import java.util.List;
import java.util.ArrayList;
 
import java.security.SecureRandom;

public class Payment {
    private static final SecureRandom RNG = new SecureRandom();
    private int paymentId;
    private int orderId;
    private double amountDue; // total due after VAT/discounts
    private double paid;
    private String timestamp;
    // Detailed receipt fields
    private List<PaymentLine> items = new ArrayList<>();
    private double subtotal;   // sum of unitPrice*qty before discount/VAT
    private double vat;        // computed VAT amount
    private double discount;   // computed discount amount (absolute)
    private double change;     // paid - amountDue (persisted for convenience)
    private String receipt;    // textual receipt for quick viewing

    public Payment() {}

    public Payment(int orderId, double amountDue, double paid) {
        // random 6-digit id per payment (100000-999999)
        this.paymentId = 100000 + RNG.nextInt(900000);
        this.orderId = orderId;
        this.amountDue = amountDue;
        this.paid = paid;
        this.timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public double getAmountDue() { return amountDue; }
    public void setAmountDue(double amountDue) { this.amountDue = amountDue; }

    public double getPaid() { return paid; }
    public void setPaid(double paid) { this.paid = paid; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getChange() { return change == 0 ? (paid - amountDue) : change; }
    public void setChange(double change) { this.change = change; }

    public List<PaymentLine> getItems() { return items; }
    public void setItems(List<PaymentLine> items) { this.items = items; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getVat() { return vat; }
    public void setVat(double vat) { this.vat = vat; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public String getReceipt() { return receipt; }
    public void setReceipt(String receipt) { this.receipt = receipt; }

    public static class PaymentLine {
        private String name;
        private int quantity;
        private double unitPrice;
        private double lineTotal;

        public PaymentLine() {}
        public PaymentLine(String name, int quantity, double unitPrice) {
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = unitPrice * quantity;
        }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
        public double getLineTotal() { return lineTotal; }
        public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
    }
}

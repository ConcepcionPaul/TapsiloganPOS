public interface Stockable {
    void addStock(int amount);
    boolean consume(int amount);
    int getStock();
    void setStock(int stock);
}

public class Table {
    private int tableNumber;
    private int capacity;
    private boolean occupied = false;

    public Table() {}

    public Table(int tableNumber, int capacity) {
        this.tableNumber = tableNumber;
        this.capacity = capacity;
    }

    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }

    @Override
    public String toString() {
        return "Table " + tableNumber + " (" + capacity + " persons) - " + (occupied ? "Occupied" : "Available");
    }
}

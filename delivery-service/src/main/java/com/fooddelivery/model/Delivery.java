package com.fooddelivery.model;

public class Delivery {
    private int id;
    private int orderId;
    private int userId;
    private String status; // e.g. PENDING, OUT_FOR_DELIVERY, DELIVERED
    private Integer assignedPartnerId; // Nullable if not assigned yet

    // Constructors
    public Delivery() {}

    public Delivery(int id, int orderId, int userId, String status, Integer assignedPartnerId) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.assignedPartnerId = assignedPartnerId;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAssignedPartnerId() { return assignedPartnerId; }
    public void setAssignedPartnerId(Integer assignedPartnerId) { this.assignedPartnerId = assignedPartnerId; }
}

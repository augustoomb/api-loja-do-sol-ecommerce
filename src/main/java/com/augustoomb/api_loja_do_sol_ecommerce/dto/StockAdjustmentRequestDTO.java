package com.augustoomb.api_loja_do_sol_ecommerce.dto;

public class StockAdjustmentRequestDTO {

    private int newStock;
    private String reason;
    private String reference;

    public int getNewStock() {
        return newStock;
    }

    public void setNewStock(int newStock) {
        this.newStock = newStock;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}

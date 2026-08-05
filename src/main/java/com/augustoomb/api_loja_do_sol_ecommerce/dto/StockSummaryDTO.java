package com.augustoomb.api_loja_do_sol_ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"totalProducts", "totalUnitsInStock", "lowStockProducts", "outOfStockProducts"})
public class StockSummaryDTO {

    private long totalProducts;
    private long totalUnitsInStock;
    private long lowStockProducts;
    private long outOfStockProducts;

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalUnitsInStock() {
        return totalUnitsInStock;
    }

    public void setTotalUnitsInStock(long totalUnitsInStock) {
        this.totalUnitsInStock = totalUnitsInStock;
    }

    public long getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(long lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }

    public long getOutOfStockProducts() {
        return outOfStockProducts;
    }

    public void setOutOfStockProducts(long outOfStockProducts) {
        this.outOfStockProducts = outOfStockProducts;
    }
}

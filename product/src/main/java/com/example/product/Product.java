package com.example.product;


public class Product {
    public int productId;
    public double price;

    public Product(double price, int productId) {
        this.price = price;
        this.productId = productId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}

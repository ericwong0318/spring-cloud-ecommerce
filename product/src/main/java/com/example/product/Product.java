package com.example.product;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "products")
@CompoundIndex(name = "idx_category_name", def = "{'categoryId': 1, 'name': 'text'}")
@CompoundIndex(name = "idx_variant_attributes", def = "{'variants.attributes': 1}")
public class Product {

    @Id
    private String id;

    @Indexed(name = "idx_product_name")
    private String name;

    private String description;

    @Indexed(name = "idx_category_id")
    private String categoryId;

    private String categoryName;

    private BigDecimal price;

    private String thumbnailImageId;

    private List<String> imageIds = new ArrayList<>();

    private List<ProductVariant> variants = new ArrayList<>();

    private Integer stockQuantity = 0;

    public Product() {
    }

    public Product(String id, String name, String description, BigDecimal price, String categoryId, String categoryName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public Product(String id, String name, String description, BigDecimal price, String categoryId, String categoryName,
                   String thumbnailImageId, List<String> imageIds, List<ProductVariant> variants) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.thumbnailImageId = thumbnailImageId;
        this.imageIds = imageIds != null ? imageIds : new ArrayList<>();
        this.variants = variants != null ? variants : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getThumbnailImageId() {
        return thumbnailImageId;
    }

    public void setThumbnailImageId(String thumbnailImageId) {
        this.thumbnailImageId = thumbnailImageId;
    }

    public List<String> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<String> imageIds) {
        this.imageIds = imageIds;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
    }

    public void removeVariant(ProductVariant variant) {
        variants.remove(variant);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
package com.example.product;

/**
 * Response body for image upload: the GridFS file ID and the URL to fetch the image.
 */
public record ImageUploadResponse(String imageId, String url) {
}

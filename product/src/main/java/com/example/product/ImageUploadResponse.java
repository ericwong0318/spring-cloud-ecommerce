package com.example.product;

/**
 * Response body for image upload: the generated image ID and the URL to fetch the image.
 */
public record ImageUploadResponse(String imageId, String url) {
}

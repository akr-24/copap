-- V5: store uploaded image filename per product.
-- Allows each product to have a custom image (e.g. "ABCD1234.png")
-- instead of always assuming the default ".jpg" extension.
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS image_filename VARCHAR(255);

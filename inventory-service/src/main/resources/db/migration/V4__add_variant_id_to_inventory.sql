-- Add variant_id column to inventory table to reference ProductVariant
ALTER TABLE inventory ADD COLUMN variant_id BIGINT UNIQUE;

-- Create index for variant_id lookups
CREATE INDEX idx_inventory_variant_id ON inventory(variant_id);

-- Update existing records to have a variant_id (assuming variant_id = product_id for backward compatibility)
-- This is a migration step; in reality variant_id should be set from product service events
UPDATE inventory SET variant_id = product_id WHERE variant_id IS NULL;

-- Make variant_id NOT NULL after data migration
ALTER TABLE inventory ALTER COLUMN variant_id SET NOT NULL;

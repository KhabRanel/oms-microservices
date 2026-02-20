CREATE TABLE inventory_items (
    product_id UUID PRIMARY KEY,
    total_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_reserved_not_negative CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_total_not_negative CHECK (total_quantity >= 0),
    CONSTRAINT chk_reserved_not_exceed_total CHECK (reserved_quantity <= total_quantity)
);
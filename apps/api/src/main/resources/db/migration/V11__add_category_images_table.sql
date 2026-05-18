-- ============================================================================
-- V11__add_category_images_table.sql
-- Add category_images table to support icons, banners, and thumbnails.
-- ============================================================================

CREATE TABLE category_images (
    id          BIGINT  GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id BIGINT  NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    path        TEXT    NOT NULL CHECK (length(path) BETWEEN 1 AND 2048),
    width       INTEGER CHECK (width  IS NULL OR width  > 0),
    height      INTEGER CHECK (height IS NULL OR height > 0),
    bytes       BIGINT  CHECK (bytes  IS NULL OR bytes  > 0),
    mime_type   TEXT    CHECK (mime_type IS NULL OR mime_type ~ '^[a-z]+/[a-z0-9.+-]+$'),
    image_type  TEXT    NOT NULL CHECK (image_type IN ('icon', 'banner', 'thumbnail')),
    position    INTEGER NOT NULL DEFAULT 0 CHECK (position >= 0),
    is_primary  BOOLEAN NOT NULL DEFAULT false,
    UNIQUE (category_id, position)
);

CREATE INDEX idx_category_images_category_id ON category_images(category_id);
CREATE INDEX idx_category_images_type ON category_images(category_id, image_type);

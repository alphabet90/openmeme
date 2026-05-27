-- Grant DELETE on memes and categories for the category purge flow.
-- V8 granted SELECT, INSERT, UPDATE on all tables; V10 added DELETE on
-- meme_images and meme_tags. The purgeCategories flow needs DELETE on
-- memes and categories as well.
GRANT DELETE ON memes      TO srv_memes;
GRANT DELETE ON categories TO srv_memes;

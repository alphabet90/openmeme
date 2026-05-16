-- Grant DELETE on child tables that the API needs to replace rows.
-- V8 only granted SELECT, INSERT, UPDATE; the reindex flow deletes existing
-- rows from meme_images and meme_tags before inserting new ones.
GRANT DELETE ON meme_images TO srv_memes;
GRANT DELETE ON meme_tags   TO srv_memes;

ALTER TABLE reservation_messages
    ADD COLUMN attachment_file_id INTEGER NULL
        REFERENCES stored_files(id) ON DELETE SET NULL;

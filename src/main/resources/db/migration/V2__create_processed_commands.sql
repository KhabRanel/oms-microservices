CREATE TABLE processed_commands (
    command_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
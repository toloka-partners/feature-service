-- Create notifications table
CREATE SEQUENCE notification_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE notifications (
    id BIGINT PRIMARY KEY DEFAULT nextval('notification_id_seq'),
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    link VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create notification_recipients table
CREATE SEQUENCE notification_recipient_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE notification_recipients (
    id BIGINT PRIMARY KEY DEFAULT nextval('notification_recipient_id_seq'),
    notification_id BIGINT NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    recipient VARCHAR(255) NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(notification_id, recipient)
);

-- Create indexes for better performance
CREATE INDEX idx_notifications_event_id ON notifications(event_id);
CREATE INDEX idx_notifications_event_type ON notifications(event_type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notification_recipients_notification_id ON notification_recipients(notification_id);
CREATE INDEX idx_notification_recipients_recipient ON notification_recipients(recipient);
CREATE INDEX idx_notification_recipients_read_at ON notification_recipients(read_at);
-- Sample feature usage data for testing
INSERT INTO feature_usage (id, user_id, feature_code, product_code, action_type, timestamp, context, ip_address, user_agent)
VALUES
    (nextval('feature_usage_id_seq'), 'user1@example.com', 'IDEA-1', 'intellij', 'FEATURE_VIEWED', NOW() - INTERVAL '2 hours', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user1@example.com', 'IDEA-2', 'intellij', 'FEATURE_VIEWED', NOW() - INTERVAL '1 hour', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user2@example.com', 'IDEA-1', 'intellij', 'FEATURE_VIEWED', NOW() - INTERVAL '3 hours', '{"source": "mobile"}', '192.168.1.101', 'Mobile App'),
    (nextval('feature_usage_id_seq'), 'user2@example.com', 'GO-3', 'goland', 'FEATURE_CREATED', NOW() - INTERVAL '4 hours', '{"source": "web"}', '192.168.1.101', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user3@example.com', 'IDEA-1', 'intellij', 'FEATURE_UPDATED', NOW() - INTERVAL '5 hours', '{"source": "api"}', '192.168.1.102', 'API Client'),
    (nextval('feature_usage_id_seq'), 'user3@example.com', 'IDEA-2', 'intellij', 'FEATURE_DELETED', NOW() - INTERVAL '6 hours', '{"source": "web"}', '192.168.1.102', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user1@example.com', 'IDEA-1', 'intellij', 'FEATURES_LISTED', NOW() - INTERVAL '30 minutes', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user2@example.com', NULL, 'intellij', 'PRODUCT_VIEWED', NOW() - INTERVAL '1 hour', '{"source": "web"}', '192.168.1.101', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user3@example.com', NULL, 'intellij', 'PRODUCT_CREATED', NOW() - INTERVAL '2 days', '{"source": "admin"}', '192.168.1.102', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user1@example.com', NULL, 'intellij', 'RELEASE_VIEWED', NOW() - INTERVAL '8 hours', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user2@example.com', NULL, 'intellij', 'RELEASE_CREATED', NOW() - INTERVAL '1 day', '{"source": "admin"}', '192.168.1.101', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user3@example.com', 'IDEA-1', 'intellij', 'COMMENT_ADDED', NOW() - INTERVAL '10 hours', '{"source": "web"}', '192.168.1.102', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user1@example.com', 'IDEA-2', 'intellij', 'FAVORITE_ADDED', NOW() - INTERVAL '12 hours', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user2@example.com', 'GO-3', 'goland', 'FAVORITE_REMOVED', NOW() - INTERVAL '15 hours', '{"source": "web"}', '192.168.1.101', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user3@example.com', 'IDEA-1', 'intellij', 'FEATURE_VIEWED', NOW() - INTERVAL '20 minutes', '{"source": "web"}', '192.168.1.102', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user4@example.com', 'IDEA-2', 'intellij', 'FEATURE_VIEWED', NOW() - INTERVAL '25 minutes', '{"source": "web"}', '192.168.1.103', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user4@example.com', 'GO-3', 'goland', 'FEATURE_VIEWED', NOW() - INTERVAL '35 minutes', '{"source": "mobile"}', '192.168.1.103', 'Mobile App'),
    (nextval('feature_usage_id_seq'), 'user5@example.com', 'IDEA-1', 'intellij', 'FEATURE_CREATED', NOW() - INTERVAL '40 minutes', '{"source": "api"}', '192.168.1.104', 'API Client'),
    (nextval('feature_usage_id_seq'), 'user5@example.com', 'IDEA-2', 'intellij', 'FEATURE_UPDATED', NOW() - INTERVAL '50 minutes', '{"source": "web"}', '192.168.1.104', 'Mozilla/5.0'),
    (nextval('feature_usage_id_seq'), 'user1@example.com', 'GO-3', 'goland', 'FEATURE_VIEWED', NOW() - INTERVAL '1 minute', '{"source": "web"}', '192.168.1.100', 'Mozilla/5.0');

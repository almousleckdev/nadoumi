CREATE TABLE nad_support_meeting (
  id bigint NOT NULL AUTO_INCREMENT,
  ticket_id bigint NOT NULL COMMENT 'FK to nad_support_ticket',
  student_id bigint NOT NULL COMMENT 'FK to sys_user (student)',
  staff_id bigint NOT NULL COMMENT 'FK to sys_user (staff)',
  start_time datetime NOT NULL,
  duration_minutes int NOT NULL,
  status varchar(20) NOT NULL COMMENT 'SCHEDULED, COMPLETED, CANCELLED',
  meeting_url varchar(255) DEFAULT NULL,
  create_by varchar(64) DEFAULT '',
  create_time datetime DEFAULT NULL,
  update_by varchar(64) DEFAULT '',
  update_time datetime DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_nad_meeting_student (student_id),
  KEY idx_nad_meeting_staff (staff_id),
  KEY idx_nad_meeting_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='1:1 online meetings booked from Support';

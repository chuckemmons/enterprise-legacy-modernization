-- Loaded before every DAO test; all writes belong to the private H2 in-memory database.
DELETE FROM case_notices;
DELETE FROM audit_events;
DELETE FROM case_documents;
DELETE FROM case_notes;
DELETE FROM service_cases;
DELETE FROM app_users;
ALTER SEQUENCE app_users_id_seq RESTART WITH 100;
ALTER SEQUENCE service_cases_id_seq RESTART WITH 100;
ALTER SEQUENCE case_notes_id_seq RESTART WITH 100;
ALTER SEQUENCE case_documents_id_seq RESTART WITH 100;
ALTER SEQUENCE audit_events_id_seq RESTART WITH 100;
ALTER SEQUENCE case_notices_id_seq RESTART WITH 100;

INSERT INTO app_users(id,username,password_hash,display_name,role,department,enabled) VALUES
(1,'admin','test-only-hash','Administrator','ADMIN','Operations',TRUE),
(2,'worker','test-only-hash','Worker','WORKER','Operations',TRUE),
(3,'supervisor','test-only-hash','Supervisor','SUPERVISOR','Operations',TRUE),
(4,'auditor','test-only-hash','Auditor','AUDITOR','Operations',TRUE),
(5,'disabled','test-only-hash','Disabled worker','WORKER','Operations',FALSE);
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,due_at,updated_at,archived,overdue_notice_sent) VALUES
(1,'CASE-000001','Access request','Access description','ACCESS','HIGH','NEW','worker','worker','Operations','2000-01-01 12:00:00','2000-01-01 12:00:00',FALSE,FALSE),
(2,'CASE-000002','General request','General description','GENERAL','MEDIUM','IN_PROGRESS','worker','worker','Operations','2999-01-01 12:00:00','2000-01-02 12:00:00',FALSE,FALSE),
(3,'CASE-000003','Closed request','Closed description','GENERAL','LOW','CLOSED','worker','worker','Operations','2000-01-01 12:00:00','2000-01-03 12:00:00',FALSE,FALSE),
(4,'CASE-000004','Archived access','Archived description','ACCESS','HIGH','NEW','worker','worker','Operations','2000-01-01 12:00:00','2000-01-04 12:00:00',TRUE,FALSE),
(5,'CASE-000005','Notified request','Notified description','GENERAL','CRITICAL','TRIAGED','supervisor','supervisor','Operations','2000-01-01 12:00:00','2000-01-05 12:00:00',FALSE,TRUE);
INSERT INTO case_notes(id,case_id,author,body,created_at) VALUES
(10,1,'worker','First note','2000-01-01 12:00:00'),
(11,1,'supervisor','Second note','2000-01-02 12:00:00'),
(12,2,'worker','Other case note','2000-01-03 12:00:00');
INSERT INTO case_documents(id,case_id,original_name,content_type,content_size,payload,uploaded_by,created_at) VALUES
(20,1,'seed.txt','text/plain',3,X'010203','worker','2000-01-01 12:00:00'),
(21,2,'other.txt','text/plain',2,X'0405','worker','2000-01-02 12:00:00');
INSERT INTO audit_events(id,entity_type,entity_id,action_name,actor,details,created_at) VALUES
(30,'CASE',1,'CREATE','admin','Case created','2000-01-01 12:00:00'),
(31,'CASE',1,'UPDATE','worker','Case updated','2000-01-02 12:00:00');
INSERT INTO case_notices(id,recipient,case_id,subject,message,is_read,sent_at,created_at) VALUES
(40,'worker',1,'First','First message',FALSE,NULL,'2000-01-01 12:00:00'),
(41,'admin',1,'Sent','Sent message',FALSE,'2000-01-02 12:00:00','2000-01-02 12:00:00'),
(42,'worker',1,'Read','Read message',TRUE,NULL,'2000-01-03 12:00:00'),
(43,'auditor',2,'Other recipient','Other message',FALSE,NULL,'2000-01-04 12:00:00');

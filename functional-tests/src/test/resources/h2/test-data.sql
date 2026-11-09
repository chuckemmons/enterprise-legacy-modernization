-- Loaded before every functional test; all writes belong to the private H2 in-memory database.
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
(1,'admin','PBKDF2_SHA256$150000$ZnVuY3Rpb25hbC10ZXN0cw==$NdOnTEFdVrc1FRaKgsheJrJjdbOw6XC8QbjNPL2XkHs=','Administrator','ADMIN','Operations',TRUE),
(2,'worker','PBKDF2_SHA256$150000$ZnVuY3Rpb25hbC10ZXN0cw==$NdOnTEFdVrc1FRaKgsheJrJjdbOw6XC8QbjNPL2XkHs=','Worker','WORKER','Operations',TRUE),
(3,'supervisor','PBKDF2_SHA256$150000$ZnVuY3Rpb25hbC10ZXN0cw==$NdOnTEFdVrc1FRaKgsheJrJjdbOw6XC8QbjNPL2XkHs=','Supervisor','SUPERVISOR','Operations',TRUE),
(4,'auditor','PBKDF2_SHA256$150000$ZnVuY3Rpb25hbC10ZXN0cw==$NdOnTEFdVrc1FRaKgsheJrJjdbOw6XC8QbjNPL2XkHs=','Auditor','AUDITOR','Operations',TRUE),
(5,'disabled','PBKDF2_SHA256$150000$ZnVuY3Rpb25hbC10ZXN0cw==$NdOnTEFdVrc1FRaKgsheJrJjdbOw6XC8QbjNPL2XkHs=','Disabled worker','WORKER','Operations',FALSE);
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

-- Enough distinct cases to exercise both result pages; IDs stay below the application sequences.
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (6,'CASE-000006','Pagination case 06','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (7,'CASE-000007','Pagination case 07','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (8,'CASE-000008','Pagination case 08','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (9,'CASE-000009','Pagination case 09','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (10,'CASE-000010','Pagination case 10','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (11,'CASE-000011','Pagination case 11','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (12,'CASE-000012','Pagination case 12','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (13,'CASE-000013','Pagination case 13','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (14,'CASE-000014','Pagination case 14','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (15,'CASE-000015','Pagination case 15','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (16,'CASE-000016','Pagination case 16','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (17,'CASE-000017','Pagination case 17','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (18,'CASE-000018','Pagination case 18','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (19,'CASE-000019','Pagination case 19','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (20,'CASE-000020','Pagination case 20','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (21,'CASE-000021','Pagination case 21','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (22,'CASE-000022','Pagination case 22','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (23,'CASE-000023','Pagination case 23','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (24,'CASE-000024','Pagination case 24','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (25,'CASE-000025','Pagination case 25','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (26,'CASE-000026','Pagination case 26','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (27,'CASE-000027','Pagination case 27','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (28,'CASE-000028','Pagination case 28','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (29,'CASE-000029','Pagination case 29','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (30,'CASE-000030','Pagination case 30','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (31,'CASE-000031','Pagination case 31','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (32,'CASE-000032','Pagination case 32','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (33,'CASE-000033','Pagination case 33','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (34,'CASE-000034','Pagination case 34','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
INSERT INTO service_cases(id,case_number,title,description,category,priority,status,created_by,assigned_to,department,updated_at) VALUES (35,'CASE-000035','Pagination case 35','Seeded functional case','GENERAL','LOW','NEW','worker','worker','Operations','2000-02-01 12:00:00');
UPDATE service_cases SET title='=1+1' WHERE id=35;

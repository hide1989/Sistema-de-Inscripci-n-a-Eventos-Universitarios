INSERT INTO events (id, name, max_capacity, available_spots, event_date, version) VALUES ('aaaaaaaa-0001-0001-0001-000000000001', 'Taller de Programación Concurrente', 50, 50, DATEADD('DAY', 7, NOW()), 0);
INSERT INTO events (id, name, max_capacity, available_spots, event_date, version) VALUES ('aaaaaaaa-0002-0002-0002-000000000002', 'Charla: Arquitectura Hexagonal en la Práctica', 10, 10, DATEADD('DAY', 14, NOW()), 0);
INSERT INTO participants (id, name, email) VALUES ('bbbbbbbb-0001-0001-0001-000000000001', 'Ana García', 'ana.garcia@pascualbravo.edu.co');
INSERT INTO participants (id, name, email) VALUES ('bbbbbbbb-0002-0002-0002-000000000002', 'Carlos López', 'carlos.lopez@pascualbravo.edu.co');
INSERT INTO participants (id, name, email) VALUES ('bbbbbbbb-0003-0003-0003-000000000003', 'María Torres', 'maria.torres@pascualbravo.edu.co');

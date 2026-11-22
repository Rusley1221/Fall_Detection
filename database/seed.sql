-- Data demo: Syauqi Akifur Rahman, NIM 21120124130097
-- Seki (082176119857, anak) sengaja menjadi kontak utama & urutan pertama untuk uji SMS.
INSERT INTO users (id, full_name, nim, phone)
VALUES ('11111111-1111-1111-1111-111111111111', 'Syauqi Akifur Rahman', '21120124130097', '082176119857')
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_settings (user_id, impact_threshold, inactivity_seconds, countdown_seconds)
VALUES ('11111111-1111-1111-1111-111111111111', 25.0, 3, 20)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO emergency_contacts (id, user_id, name, phone, relationship, is_primary, created_at)
VALUES
 ('c0000000-0000-4000-8000-000000000001','11111111-1111-1111-1111-111111111111','Seki','082176119857','anak',true, now() - interval '9 days'),
 ('c0000000-0000-4000-8000-000000000002','11111111-1111-1111-1111-111111111111','Bapak (decoy)','081234567801','ayah',false, now() - interval '8 days'),
 ('c0000000-0000-4000-8000-000000000003','11111111-1111-1111-1111-111111111111','Ibu (decoy)','081234567802','ibu',false, now() - interval '7 days'),
 ('c0000000-0000-4000-8000-000000000004','11111111-1111-1111-1111-111111111111','Tetangga (decoy)','081234567803','tetangga',false, now() - interval '6 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO fall_events (id, user_id, detected_at, latitude, longitude, location_accuracy,
                         peak_acceleration, inactivity_seconds, status, notes)
VALUES
 ('f0000000-0000-4000-8000-000000000001','11111111-1111-1111-1111-111111111111', now() - interval '10 minutes', -6.993200, 110.420300, 12.5, 28.5, 4.0, 'confirmed', 'Terdeteksi di depan rumah'),
 ('f0000000-0000-4000-8000-000000000002','11111111-1111-1111-1111-111111111111', now() - interval '3 hours', -6.995100, 110.418800, 20.0, 31.2, 5.1, 'resolved', 'Sudah dibantu tetangga'),
 ('f0000000-0000-4000-8000-000000000003','11111111-1111-1111-1111-111111111111', now() - interval '7 hours', NULL, NULL, NULL, 26.4, 3.4, 'confirmed', 'GPS tidak tersedia'),
 ('f0000000-0000-4000-8000-000000000004','11111111-1111-1111-1111-111111111111', now() - interval '15 hours', -7.001200, 110.431100, 9.0, 27.1, 3.8, 'cancelled', 'Peringatan palsu'),
 ('f0000000-0000-4000-8000-000000000005','11111111-1111-1111-1111-111111111111', now() - interval '1 day', -6.990400, 110.424900, 15.5, 29.8, 4.6, 'resolved', NULL),
 ('f0000000-0000-4000-8000-000000000006','11111111-1111-1111-1111-111111111111', now() - interval '2 days', -6.988700, 110.416500, 30.0, 25.6, 3.1, 'confirmed', NULL),
 ('f0000000-0000-4000-8000-000000000007','11111111-1111-1111-1111-111111111111', now() - interval '3 days', -6.996800, 110.427700, 11.0, 33.4, 6.2, 'resolved', 'Dibawa ke klinik'),
 ('f0000000-0000-4000-8000-000000000008','11111111-1111-1111-1111-111111111111', now() - interval '5 days', -6.991100, 110.422200, 18.0, 24.9, 3.0, 'cancelled', 'Telepon jatuh dari meja')
ON CONFLICT (id) DO NOTHING;

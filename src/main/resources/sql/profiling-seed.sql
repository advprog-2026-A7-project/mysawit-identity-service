-- ============================================================
-- profiling-seed.sql
-- ============================================================
-- Tujuan: menyiapkan data realistis untuk load-testing
--         endpoint GET /api/admin/users.
--
-- Komposisi:
--   - 5 mandors (atasan)
--   - 100 buruhs (semua ditugaskan ke salah satu mandor)
--   - 10 supirs
--
-- Kenapa komposisi ini? Endpoint list akan return ~115 user.
-- Setiap Buruh punya FK ke Mandor — pola yang paling mungkin
-- memicu masalah performance JPA klasik. Kalau seed-nya cuma
-- 5 user, masalahnya tidak akan kelihatan.
--
-- Cara jalankan (pilih salah satu):
--
--   A) Dari psql langsung ke Supabase (paling cepat):
--      psql "$DB_URL" -f src/main/resources/sql/profiling-seed.sql
--
--   B) Lewat Supabase Studio:
--      Buka SQL Editor → paste isi file ini → Run.
--
-- Untuk reset/ulang:
--   DELETE FROM buruhs;
--   DELETE FROM mandors;
--   DELETE FROM supirs;
--   DELETE FROM users WHERE email LIKE 'profiling-%@example.com';
-- ============================================================

DO $$
DECLARE
    mandor_ids UUID[] := ARRAY[
        gen_random_uuid(), gen_random_uuid(), gen_random_uuid(),
        gen_random_uuid(), gen_random_uuid()
    ];
    new_id UUID;
    i INT;
BEGIN
    ------------------------------------------------------------
    -- 1) Insert 5 mandors
    ------------------------------------------------------------
    FOR i IN 1..5 LOOP
        new_id := mandor_ids[i];

        INSERT INTO users (
            id, username, email, name, password, role,
            account_non_expired, account_non_locked,
            credentials_non_expired, enabled,
            created_at, updated_at
        ) VALUES (
            new_id::text,
            'profiling-mandor-' || i,
            'profiling-mandor-' || i || '@example.com',
            'Mandor Profiling ' || i,
            -- BCrypt hash of "password123" (only for local testing!)
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'MANDOR',
            TRUE, TRUE, TRUE, TRUE,
            NOW(), NOW()
        );

        INSERT INTO mandors (id, certification_number)
        VALUES (new_id::text, 'CERT-PROF-' || LPAD(i::text, 4, '0'));
    END LOOP;

    ------------------------------------------------------------
    -- 2) Insert 100 buruhs, distribute round-robin across mandors
    ------------------------------------------------------------
    FOR i IN 1..100 LOOP
        new_id := gen_random_uuid();

        INSERT INTO users (
            id, username, email, name, password, role,
            account_non_expired, account_non_locked,
            credentials_non_expired, enabled,
            created_at, updated_at
        ) VALUES (
            new_id::text,
            'profiling-buruh-' || i,
            'profiling-buruh-' || i || '@example.com',
            'Buruh Profiling ' || i,
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'BURUH',
            TRUE, TRUE, TRUE, TRUE,
            NOW(), NOW()
        );

        INSERT INTO buruhs (id, mandor_id)
        VALUES (
            new_id::text,
            -- round-robin: buruh i -> mandor (i mod 5)+1
            mandor_ids[((i - 1) % 5) + 1]::text
        );
    END LOOP;

    ------------------------------------------------------------
    -- 3) Insert 10 supirs
    ------------------------------------------------------------
    FOR i IN 1..10 LOOP
        new_id := gen_random_uuid();

        INSERT INTO users (
            id, username, email, name, password, role,
            account_non_expired, account_non_locked,
            credentials_non_expired, enabled,
            created_at, updated_at
        ) VALUES (
            new_id::text,
            'profiling-supir-' || i,
            'profiling-supir-' || i || '@example.com',
            'Supir Profiling ' || i,
            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
            'SUPIR',
            TRUE, TRUE, TRUE, TRUE,
            NOW(), NOW()
        );

        INSERT INTO supirs (id, kebun_id)
        VALUES (new_id::text, 'KEBUN-' || ((i % 3) + 1));
    END LOOP;

    RAISE NOTICE 'Seed selesai: 5 mandor, 100 buruh, 10 supir';
END $$;

-- Verifikasi cepat:
\

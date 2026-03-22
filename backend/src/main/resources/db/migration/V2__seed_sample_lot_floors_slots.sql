-- V2__seed_sample_lot_floors_slots.sql
-- Add 3 floors with parking slots to the seeded "ParkSmart Downtown" lot.

DO $$
DECLARE
    v_lot_id      UUID;
    v_floor1_id   UUID;
    v_floor2_id   UUID;
    v_floor3_id   UUID;
    i             INT;
BEGIN
    -- Resolve the lot created in V1
    SELECT id INTO v_lot_id FROM parking_lots WHERE name = 'ParkSmart Downtown' LIMIT 1;
    IF v_lot_id IS NULL THEN RETURN; END IF;  -- idempotent guard

    -- ── FLOOR 1: Ground Floor ──────────────────────────────────────────────────
    INSERT INTO parking_floors (lot_id, floor_number, floor_name)
    VALUES (v_lot_id, 1, 'Ground Floor')
    RETURNING id INTO v_floor1_id;

    -- 10 SMALL (motorcycle) slots  G-S01 … G-S10
    FOR i IN 1..10 LOOP
        INSERT INTO parking_slots (floor_id, slot_number, slot_type, status, has_ev_charger)
        VALUES (v_floor1_id, 'G-S' || LPAD(i::TEXT, 2, '0'), 'SMALL', 'AVAILABLE', FALSE);
    END LOOP;

    -- 15 MEDIUM (car/SUV) slots  G-M01 … G-M15
    FOR i IN 1..15 LOOP
        INSERT INTO parking_slots (floor_id, slot_number, slot_type, status, has_ev_charger)
        VALUES (v_floor1_id, 'G-M' || LPAD(i::TEXT, 2, '0'), 'MEDIUM', 'AVAILABLE', FALSE);
    END LOOP;

    -- 4 EV slots  G-E01 … G-E04
    FOR i IN 1..4 LOOP
        INSERT INTO parking_slots (floor_id, slot_number, slot_type, status, has_ev_charger)
        VALUES (v_floor1_id, 'G-E' || LPAD(i::TEXT, 2, '0'), 'EV', 'AVAILABLE', TRUE);
    END LOOP;

    -- 2 OVERSIZED (bus)  G-O01, G-O02
    FOR i IN 1..2 LOOP
        INSERT INTO parking_slots (floor_id, slot_number, slot_type, status, has_ev_charger)
        VALUES (v_floor1_id, 'G-O' || LPAD(i::TEXT, 2, '0'), 'OVERSIZED', 'AVAILABLE', FALSE);
    END LOOP;

    -- ── FLOOR 2: Level 1 ──────────────────────────────────────────────────────
    INSERT INTO parking_floors (lot_id, floor_number, floor_name)
    VALUES (v_lot_id, 2, 'Level 1')
    RETURNING id INTO v_floor2_id;

    -- 20 MEDIUM slots  L1-M01 … L1-M20
    FOR i IN 1..20 LOOP
        INSERT INTO parking_slots (floor_id, slot_number, slot_type, status, has_ev_charger)
        VALUES (v_floor2_id, 'L1-M' || LPAD(i::TEXT, 2, '0'), 'MEDIUM', 'AVAILABLE', FALSE);
    END LOOP;

    -- 8 LARGE (truck) slots  L1-L01 … L1-L08
    FOR i IN 1..8 LOOP
        INSERT INTO parking_slots (floor_id, slot_number, slot_type, status, has_ev_charger)
        VALUES (v_floor2_id, 'L1-L' || LPAD(i::TEXT, 2, '0'), 'LARGE', 'AVAILABLE', FALSE);
    END LOOP;

    -- 6 EV slots  L1-E01 … L1-E06
    FOR i IN 1..6 LOOP
        INSERT INTO parking_slots (floor_id, slot_number, slot_type, status, has_ev_charger)
        VALUES (v_floor2_id, 'L1-E' || LPAD(i::TEXT, 2, '0'), 'EV', 'AVAILABLE', TRUE);
    END LOOP;

    -- ── FLOOR 3: Level 2 ──────────────────────────────────────────────────────
    INSERT INTO parking_floors (lot_id, floor_number, floor_name)
    VALUES (v_lot_id, 3, 'Level 2')
    RETURNING id INTO v_floor3_id;

    -- 15 MEDIUM slots  L2-M01 … L2-M15
    FOR i IN 1..15 LOOP
        INSERT INTO parking_slots (floor_id, slot_number, slot_type, status, has_ev_charger)
        VALUES (v_floor3_id, 'L2-M' || LPAD(i::TEXT, 2, '0'), 'MEDIUM', 'AVAILABLE', FALSE);
    END LOOP;

    -- 10 LARGE slots  L2-L01 … L2-L10
    FOR i IN 1..10 LOOP
        INSERT INTO parking_slots (floor_id, slot_number, slot_type, status, has_ev_charger)
        VALUES (v_floor3_id, 'L2-L' || LPAD(i::TEXT, 2, '0'), 'LARGE', 'AVAILABLE', FALSE);
    END LOOP;

    -- 5 SMALL slots  L2-S01 … L2-S05
    FOR i IN 1..5 LOOP
        INSERT INTO parking_slots (floor_id, slot_number, slot_type, status, has_ev_charger)
        VALUES (v_floor3_id, 'L2-S' || LPAD(i::TEXT, 2, '0'), 'SMALL', 'AVAILABLE', FALSE);
    END LOOP;

END $$;

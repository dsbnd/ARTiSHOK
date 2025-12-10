
-- 1. Измените тип колонки status с ENUM на VARCHAR
ALTER TABLE gallery ALTER COLUMN status TYPE VARCHAR(20)
USING status::text;

-- 2. Установите значение по умолчанию
ALTER TABLE gallery ALTER COLUMN status SET DEFAULT 'PENDING';

-- 3. Добавьте CHECK constraint для валидации значений
ALTER TABLE gallery ADD CONSTRAINT gallery_status_check
CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED'));



-- Измените тип колонки status в таблице exhibition_event
ALTER TABLE exhibition_event
ALTER COLUMN status TYPE VARCHAR(20)
USING status::text;

-- Установите DEFAULT
ALTER TABLE exhibition_event
ALTER COLUMN status SET DEFAULT 'DRAFT';

-- Добавьте CHECK constraint
ALTER TABLE exhibition_event
ADD CONSTRAINT check_exhibition_status
CHECK (status IN ('DRAFT', 'ACTIVE', 'FINISHED', 'CANCELLED'));
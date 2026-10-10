-- ## 多个序列通用记录表
CREATE TABLE IF NOT EXISTS sequence_reset_log (
    id SERIAL PRIMARY KEY,
    sequence_name TEXT NOT NULL,
    last_reset_date DATE NOT NULL
);

-- ## 普通序列函数
CREATE OR REPLACE FUNCTION get_general_sequence(
    seq_name TEXT DEFAULT 'sno_general',  -- 默认序列名
    start_val INTEGER DEFAULT 1,          -- 默认起始值
    increment_by INTEGER DEFAULT 1        -- 递增值
) RETURNS BIGINT AS $func$
DECLARE
    current_val BIGINT;
    seq_exists BOOLEAN;
BEGIN
    -- 检查序列是否存在
    SELECT EXISTS (
        SELECT 1 FROM pg_class
        WHERE relname = seq_name AND relkind = 'S'
    ) INTO seq_exists;

    -- 如果序列不存在，创建它
    IF NOT seq_exists THEN
        EXECUTE format('
            CREATE SEQUENCE %I
            START WITH %s
            INCREMENT BY %s
            NO MINVALUE
            NO MAXVALUE
            CACHE 1
        ', seq_name, start_val, increment_by);
    END IF;

    -- 获取下一个值
    EXECUTE format('SELECT NEXTVAL(''%I'')', seq_name) INTO current_val;
    RETURN current_val;
END;
$func$ LANGUAGE plpgsql;

-- ## 创建序列读取函数
CREATE OR REPLACE FUNCTION get_yearly_sequence(
    seq_name TEXT DEFAULT 'sno_yearly',     -- 默认序列名
    start_val INTEGER DEFAULT 1             -- 默认起始值
) RETURNS INTEGER AS $func$
DECLARE
    current_val INTEGER;
    last_year INTEGER;
    seq_exists BOOLEAN;
BEGIN
    -- 检查序列是否存在
    SELECT EXISTS (
        SELECT 1 FROM pg_class
        WHERE relname = seq_name AND relkind = 'S'
    ) INTO seq_exists;

    -- 如果序列不存在，创建它
    IF NOT seq_exists THEN
            EXECUTE format('CREATE SEQUENCE %I START WITH %s', seq_name, start_val);
    END IF;

    -- 检查序列的重置记录
    IF NOT EXISTS (
        SELECT 1 FROM sequence_reset_log
        WHERE sequence_name = seq_name
    ) THEN
        -- 插入新记录
        INSERT INTO sequence_reset_log (sequence_name, last_reset_date)
        VALUES (seq_name, CURRENT_DATE);
    ELSE
        -- 获取最后重置年份
        SELECT EXTRACT(YEAR FROM last_reset_date) INTO last_year
        FROM sequence_reset_log
        WHERE sequence_name = seq_name;

        -- 如果年份不是当前年份，重置序列
        IF last_year < EXTRACT(YEAR FROM CURRENT_DATE) THEN
            EXECUTE format('ALTER SEQUENCE %I RESTART WITH %s', seq_name, start_val);

            -- 更新记录
            UPDATE sequence_reset_log
            SET last_reset_date = CURRENT_DATE
            WHERE sequence_name = seq_name;
            END IF;
        END IF;

        -- 获取下一个值（使用动态SQL避免SQL注入）
        EXECUTE format('SELECT NEXTVAL(''%I'')', seq_name) INTO current_val;
        RETURN current_val;
END;
$func$ LANGUAGE plpgsql;

-- ## 创建序列读取函数
CREATE OR REPLACE FUNCTION get_daily_sequence(
    seq_name TEXT DEFAULT 'sno_daily',  -- 默认序列名
    start_val INTEGER DEFAULT 1         -- 默认起始值
) RETURNS INTEGER AS $func$
DECLARE
    current_val INTEGER;
    last_date DATE;
    seq_exists BOOLEAN;
BEGIN
    -- 检查序列是否存在
    SELECT EXISTS (
        SELECT 1 FROM pg_class
        WHERE relname = seq_name AND relkind = 'S'
    ) INTO seq_exists;

    -- 如果序列不存在，创建它
    IF NOT seq_exists THEN
        EXECUTE format('CREATE SEQUENCE %I START WITH %s', seq_name, start_val);
    END IF;

    -- 检查序列的重置记录
    IF NOT EXISTS (
        SELECT 1 FROM sequence_reset_log
        WHERE sequence_name = seq_name
    ) THEN
        -- 插入新记录
        INSERT INTO sequence_reset_log (sequence_name, last_reset_date)
        VALUES (seq_name, CURRENT_DATE);
    ELSE
        -- 获取最后重置日期
        SELECT last_reset_date INTO last_date
        FROM sequence_reset_log
        WHERE sequence_name = seq_name;

        -- 如果日期不是今天，重置序列
        IF last_date < CURRENT_DATE THEN
            EXECUTE format('ALTER SEQUENCE %I RESTART WITH %s', seq_name, start_val);

            -- 更新记录
            UPDATE sequence_reset_log
            SET last_reset_date = CURRENT_DATE
            WHERE sequence_name = seq_name;
            END IF;
        END IF;

        -- 获取下一个值（使用动态SQL避免SQL注入）
        EXECUTE format('SELECT NEXTVAL(''%I'')', seq_name) INTO current_val;
        RETURN current_val;
END;
$func$ LANGUAGE plpgsql;
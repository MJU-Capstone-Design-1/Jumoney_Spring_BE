ALTER TABLE stock_candles
    DROP CONSTRAINT chk_stock_candle_interval_type;

ALTER TABLE stock_candles
    ADD CONSTRAINT chk_stock_candle_interval_type
        CHECK (interval_type IN ('MINUTE', 'THIRTY_MINUTE', 'DAY', 'WEEK', 'MONTH', 'YEAR'));

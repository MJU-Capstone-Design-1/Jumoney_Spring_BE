ALTER TABLE stock_indicators
    ADD COLUMN sales_growth_rate NUMERIC(19, 4);

ALTER TABLE master_choice_backtest_financials
    ADD COLUMN sales_growth_rate NUMERIC(19, 4);

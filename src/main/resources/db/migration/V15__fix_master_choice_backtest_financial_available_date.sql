UPDATE master_choice_backtest_financials
SET available_date = (
    date_trunc('month', to_date(settlement_year_month, 'YYYYMM'))::date
    + INTERVAL '1 month - 1 day'
    + CASE
        WHEN substring(settlement_year_month FROM 5 FOR 2) IN ('03', '06', '09')
            THEN INTERVAL '45 days'
        ELSE INTERVAL '90 days'
    END
)::date
WHERE settlement_year_month ~ '^[0-9]{6}$'
  AND substring(settlement_year_month FROM 5 FOR 2) BETWEEN '01' AND '12';

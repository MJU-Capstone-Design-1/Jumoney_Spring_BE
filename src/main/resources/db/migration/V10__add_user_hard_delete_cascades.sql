ALTER TABLE recommendations
    DROP CONSTRAINT fk3c9w1lipqdutm65a9inevwfp0,
    ADD CONSTRAINT fk3c9w1lipqdutm65a9inevwfp0
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE stock_term_scraps
    DROP CONSTRAINT fklwf3acaqrqeahtpl23ekh4rax,
    ADD CONSTRAINT fklwf3acaqrqeahtpl23ekh4rax
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE stock_term_learnings
    DROP CONSTRAINT fktdx31tae4l37jhib1qi5uen45,
    ADD CONSTRAINT fktdx31tae4l37jhib1qi5uen45
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE hojumoney_recommendations
    DROP CONSTRAINT fk7rp4o86chg68bbtvrw17m8o8y,
    ADD CONSTRAINT fk7rp4o86chg68bbtvrw17m8o8y
        FOREIGN KEY (recommendation_id) REFERENCES recommendations(recommendation_id) ON DELETE CASCADE;

ALTER TABLE recommendation_stocks
    DROP CONSTRAINT fkadub1p3h3a04gxui7yt6rc3qh,
    ADD CONSTRAINT fkadub1p3h3a04gxui7yt6rc3qh
        FOREIGN KEY (recommendation_id) REFERENCES recommendations(recommendation_id) ON DELETE CASCADE;

ALTER TABLE master_recommendations
    DROP CONSTRAINT fkqq1y76d3b0yg4lr180hkwn3d,
    ADD CONSTRAINT fkqq1y76d3b0yg4lr180hkwn3d
        FOREIGN KEY (recommendation_id) REFERENCES recommendations(recommendation_id) ON DELETE CASCADE;

ALTER TABLE recommendation_stock_tags
    DROP CONSTRAINT fkp8rrqtvn5pdqi8lw6sdivdn0v,
    ADD CONSTRAINT fkp8rrqtvn5pdqi8lw6sdivdn0v
        FOREIGN KEY (recommendation_stock_id) REFERENCES recommendation_stocks(recommendation_stock_id) ON DELETE CASCADE;

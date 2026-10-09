DROP TABLE IF EXISTS base_test_data;
DROP SEQUENCE IF EXISTS base_test_seq;

/* base_test_data */
CREATE SEQUENCE IF NOT EXISTS base_test_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS base_test_data 
( 
    base_test_pk INTEGER DEFAULT nextval('base_test_seq') NOT NULL PRIMARY KEY,
    ss_label VARCHAR(50),
    ss_list INTEGER ARRAY, /* ARRAY is typed for 2.x+, arbitrary range 2-8 */
    ss_text_field VARCHAR(100)
);

MERGE INTO base_test_data VALUES (1,'This is Label 1',ARRAY[1,2,3],'This is TextField 1') ;
MERGE INTO base_test_data VALUES (2,'This is Label 2',ARRAY[3,4,5],'This is TextField 2') ;

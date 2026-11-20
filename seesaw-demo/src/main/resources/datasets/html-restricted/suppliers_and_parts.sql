/*******************************************************************************
 * Copyright (C) 2003-2021, Prasanth R. Pasala, Brian E. Pangburn, & The Pangburn Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contributors:
 *   Prasanth R. Pasala
 *   Brian E. Pangburn
 *   Diego Gil
 *   Man "Bee" Vo
 *   Ernie R. Rael
 ******************************************************************************/
/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/

/* Dataset html-restricted.
   html-default plus html that no built-in Rules allows: img, a background,
   style, a stylesheet import, a link, form fields, title, object, and
   nesting deeper than 32. With a checker these are shown literally, and
   logged. Refused rows are between accepted ones.
   WARNING with --rules=PASS: the img, background and @import rows fetch
   URLs (the project's own files on GitHub, and host.invalid, which never
   resolves). Label 10 (frameset) makes setText throw, and label 11 (600
   nested tables) overflows the stack; they are at the end so they are
   reached only on purpose.
 */

/* This SQL script is used to generate the sample suppliers_and_parts database
   for the SwingSet sample programs */

/* housekeeping */
DROP TABLE IF EXISTS supplier_part_data;
DROP SEQUENCE IF EXISTS supplier_part_data_seq;
DROP TABLE IF EXISTS supplier_data;
DROP SEQUENCE IF EXISTS supplier_data_seq;
DROP TABLE IF EXISTS part_data;
DROP SEQUENCE IF EXISTS part_data_seq;

/* supplier_data */
CREATE SEQUENCE IF NOT EXISTS supplier_data_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS supplier_data 
( 
    supplier_id INTEGER DEFAULT nextval('supplier_data_seq') NOT NULL PRIMARY KEY,
    supplier_name VARCHAR(2000), 
    status SMALLINT CONSTRAINT NN_supplier_data_status NOT NULL,
    city VARCHAR(2000),
    CONSTRAINT CHK_supplier_data_status CHECK (status <= 999 and status <> 321)
);
    // status SMALLINT NOT NULL CHECK (status <= 999 and status <> 321), 

MERGE INTO supplier_data VALUES (2, '<html><b>Jones</b>', 10, 'Paris');
MERGE INTO supplier_data VALUES (5, '<html><i>Adams</i> &amp; Sons', 30, '<html><i>Athens</i>');
MERGE INTO supplier_data VALUES (4,'Clark', 20, 'London');
MERGE INTO supplier_data VALUES (6, '<html><object classid="javax.swing.JButton"></object>Anurak', 73, 'Bankok');
MERGE INTO supplier_data VALUES (3, '<html><div align="right">Blake</div>', 30, 'Paris');
MERGE INTO supplier_data VALUES (1, '<HTML><u>Smith</u>', 20, 'London');
MERGE INTO supplier_data VALUES (8, ' <html><b>Gupta</b>', 71, 'Mumbai');
MERGE INTO supplier_data VALUES (7, '<html lang=uk><b>Kozar</b>', 79, 'Kyiv');

/* SELECT * from supplier_data; */


/* part_data */
CREATE SEQUENCE IF NOT EXISTS part_data_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS part_data 
( 
    part_id INTEGER DEFAULT nextval('part_data_seq') NOT NULL PRIMARY KEY,
    part_name VARCHAR(2000), 
    color_code SMALLINT, 
    weight SMALLINT, 
    city VARCHAR(2000)
);

MERGE INTO part_data VALUES (6, '<html>Cog<br><small>sprocket</small>', 0, 19, 'London');
MERGE INTO part_data VALUES (5, '<html><img src="https://raw.githubusercontent.com/visdb-dev/seesaw/main/seesaw-demo/src/main/resources/sample_image_1.png" width="16" height="16"> Cam', 2, 12, '<html><i>Paris</i>');
MERGE INTO part_data VALUES (4, '<html>Screw <small>M4</small>', 0, 14, 'London');
MERGE INTO part_data VALUES (3, '<html><img src="http://host.invalid/beacon.png"> Screw', 2, 17, '<html><span color="blue">Rome</span>');
MERGE INTO part_data VALUES (2, '<html><big>B</big>olt<sub>2</sub>', 1, 17, 'Paris');
MERGE INTO part_data VALUES (1, 'Nut', 0, 12, 'London');

/* SELECT * FROM part_data; */

/* supplier_part_data */
CREATE SEQUENCE IF NOT EXISTS supplier_part_data_seq START WITH 1000;
CREATE TABLE IF NOT EXISTS supplier_part_data 
( 
    supplier_part_id INTEGER DEFAULT nextval('supplier_part_data_seq') NOT NULL PRIMARY KEY,
    supplier_id INT, 
    part_id INT, 
    quantity SMALLINT, 
    ship_date DATE,
    FOREIGN KEY (supplier_id) REFERENCES supplier_data(supplier_id), 
    FOREIGN KEY (part_id) REFERENCES part_data(part_id)
);

MERGE INTO supplier_part_data VALUES (8, 3, 2, 700, '2003-10-15');
MERGE INTO supplier_part_data VALUES (7, 2, 1, 400, '2004-09-15');
MERGE INTO supplier_part_data VALUES (4, 4, 6, 700, '2004-03-15');
MERGE INTO supplier_part_data VALUES (5, 5, 5, 100, '2004-03-25');
MERGE INTO supplier_part_data VALUES (1, 1, 4, 100, '2003-10-20');
MERGE INTO supplier_part_data VALUES (3, 3, 3, 400, '2003-01-05');
MERGE INTO supplier_part_data VALUES (2, 2, 2, 200, '2003-01-01');
MERGE INTO supplier_part_data VALUES (6, 1, 1, 700, '2003-12-12');
MERGE INTO supplier_part_data VALUES (9, 4, 3, 700, '2003-12-12');
MERGE INTO supplier_part_data VALUES (12, 2, 5, 400, '2003-12-12');
MERGE INTO supplier_part_data VALUES (11, 1, 4, 300, '2003-12-12');
MERGE INTO supplier_part_data VALUES (10, 5, 2, 200, '2003-12-12');

/* SELECT * FROM supplier_part_data; */

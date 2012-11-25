CREATE TABLE countries (_id INTEGER PRIMARY KEY, country_code TEXT NOT NULL, name TEXT, language TEXT NOT NULL);
INSERT INTO countries VALUES(1, 'SI', 'Slovenija', 'sl-si');
INSERT INTO countries VALUES(2, 'CZ', 'Češka', 'sl-si');
INSERT INTO countries VALUES(3, 'DK', 'Danska', 'sl-si');
INSERT INTO countries VALUES(4, 'DE', 'Nemčija', 'sl-si');
INSERT INTO countries VALUES(5, 'EE', 'Estonija', 'sl-si');
INSERT INTO countries VALUES(6, 'ES', 'Španija', 'sl-si');
INSERT INTO countries VALUES(7, 'FI', 'Finska', 'sl-si');
INSERT INTO countries VALUES(8, 'FR', 'Francija', 'sl-si');
INSERT INTO countries VALUES(9, 'GR', 'Grčija', 'sl-si');
INSERT INTO countries VALUES(10, 'HR', 'Hrvaška', 'sl-si');
INSERT INTO countries VALUES(11, 'HU', 'Madžarska', 'sl-si');
INSERT INTO countries VALUES(12, 'IT', 'Italija', 'sl-si');
INSERT INTO countries VALUES(13, 'LT', 'Litva', 'sl-si');
INSERT INTO countries VALUES(14, 'LV', 'Latvija', 'sl-si');
INSERT INTO countries VALUES(15, 'ME', 'Črna gora', 'sl-si');
INSERT INTO countries VALUES(16, 'MK', 'Makedonija', 'sl-si');
INSERT INTO countries VALUES(17, 'NL', 'Nizozemska', 'sl-si');
INSERT INTO countries VALUES(18, 'PL', 'Poljska', 'sl-si');
INSERT INTO countries VALUES(19, 'PT', 'Portugalska', 'sl-si');
INSERT INTO countries VALUES(20, 'RO', 'Romunija', 'sl-si');
INSERT INTO countries VALUES(21, 'RS', 'Srbija', 'sl-si');
INSERT INTO countries VALUES(22, 'SE', 'Švedska', 'sl-si');
INSERT INTO countries VALUES(23, 'SK', 'Slovaška', 'sl-si');
INSERT INTO countries VALUES(24, 'AT', 'Avstrija', 'sl-si');
INSERT INTO countries VALUES(25, 'BA', 'Bosna in Hercegovina', 'sl-si');
INSERT INTO countries VALUES(26, 'CH', 'Švica', 'sl-si');
INSERT INTO countries VALUES(27, 'BE', 'Belgija', 'sl-si');
INSERT INTO countries VALUES(28, 'BG', 'Bolgarija', 'sl-si');
CREATE INDEX country_code_index ON countries(country_code ASC);
CREATE INDEX country_language_index ON countries(language ASC);

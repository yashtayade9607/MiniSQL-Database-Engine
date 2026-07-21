-- Example SQL script for NovaDB

-- Create and use database
CREATE DATABASE testdb;
USE testdb;

-- Create table
CREATE TABLE employees (
    id INT PRIMARY KEY,
    name STRING NOT NULL,
    department STRING,
    salary DOUBLE,
    is_active BOOLEAN DEFAULT true
);

-- Insert data
INSERT INTO employees (id, name, department, salary) VALUES (1, 'Alice Smith', 'Engineering', 120000.50);
INSERT INTO employees (id, name, department, salary) VALUES (2, 'Bob Jones', 'HR', 75000.00);
INSERT INTO employees (id, name, department, salary) VALUES (3, 'Charlie Brown', 'Engineering', 110000.00);
INSERT INTO employees (id, name, department, salary) VALUES (4, 'Diana Prince', 'Management', 150000.00);

-- Query data
SELECT * FROM employees;
SELECT name, salary FROM employees WHERE department = 'Engineering';
SELECT * FROM employees WHERE salary >= 100000 ORDER BY salary DESC LIMIT 2;

-- Update data
BEGIN;
UPDATE employees SET salary = 125000.00 WHERE id = 1;
UPDATE employees SET is_active = false WHERE id = 2;
COMMIT;

-- Verify update
SELECT * FROM employees;

-- Delete data
DELETE FROM employees WHERE id = 2;

-- Verify deletion
SELECT * FROM employees;

-- Show tables
SHOW TABLES;

-- Describe table
DESCRIBE employees;

-- Semgrep check_id and metadata can exceed the original column widths

ALTER TABLE findings ALTER COLUMN rule_id TYPE VARCHAR(255);
ALTER TABLE findings ALTER COLUMN owasp_code TYPE VARCHAR(50);
ALTER TABLE findings ALTER COLUMN cwe TYPE VARCHAR(50);

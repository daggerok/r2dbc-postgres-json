-- see: https://github.com/pgjdbc/pgjdbc/issues/265#issuecomment-842459078
-- see: https://github.com/pgjdbc/pgjdbc/issues/265#issuecomment-428336719

DO
$$
BEGIN
CREATE CAST (VARCHAR AS JSON) WITH INOUT AS IMPLICIT;
EXCEPTION
        WHEN duplicate_object THEN null;
END $$;

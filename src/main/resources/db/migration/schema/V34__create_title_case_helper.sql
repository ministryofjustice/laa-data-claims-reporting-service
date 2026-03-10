CREATE OR REPLACE FUNCTION claims.convert_string_to_title_case(input_string text)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
	-- Also remove underscores and replace with spaces
	SELECT INITCAP(REGEXP_REPLACE(TRIM(input_string), '_+', ' ', 'g'))
$$;
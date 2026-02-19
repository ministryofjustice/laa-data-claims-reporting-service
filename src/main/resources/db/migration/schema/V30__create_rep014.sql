DROP MATERIALIZED VIEW IF EXISTS mvw_report_014;

CREATE MATERIALIZED VIEW mvw_report_014 AS
WITH submission_periods AS (
	SELECT
		s.id AS submission_id,
		s.office_account_number,
		s.submission_period,
		s.area_of_law,
		s.status AS submission_status,
		CASE
			WHEN s.submission_period ~ '^\d{4}-\d{2}-\d{2}$'
				THEN TO_DATE(s.submission_period, 'YYYY-MM-DD')
			WHEN s.submission_period ~ '^\d{4}-\d{2}$'
				THEN TO_DATE(s.submission_period || '-01', 'YYYY-MM-DD')
			WHEN s.submission_period ~ '^[A-Za-z]{3}-\d{4}$'
				THEN TO_DATE(UPPER(s.submission_period), 'MON-YYYY')
			ELSE NULL
		END AS submission_period_date
	FROM claims.submission AS s
), assessment_lines AS (
	SELECT
		a.id as assessment_id,
		a.claim_id,
		c.submission_id,
		c.unique_file_number,
		a.created_on AS assessment_created_on,
		-- try and get the last value, but if first assessment (so LAG==NULL) get the last calc amount
		-- numeric as need to do some maths with it
		LAG(a.allowed_total_incl_vat, 1, COALESCE(calc_latest.total_amount::numeric, 0)) OVER (
			PARTITION BY c.id
			ORDER BY a.created_on ASC NULLS LAST, a.id
		) AS before_value,
		a.allowed_total_incl_vat AS after_value,
		c.status AS claim_status
    FROM claims.assessment AS a
    JOIN claims.claim as c
		ON c.id = a.claim_id
	 -- LATERAL join for latest calculated fee detail (performance optimization)
    LEFT JOIN LATERAL (
    	SELECT *
    	FROM claims.calculated_fee_detail cfd_inner
    	WHERE cfd_inner.claim_id = c.id
        ORDER BY COALESCE(cfd_inner.updated_on, cfd_inner.created_on) DESC NULLS LAST
    	LIMIT 1
    ) calc_latest ON TRUE
)
SELECT
	COALESCE(sp.office_account_number, '')										AS "Office Account Number",
	COALESCE(TO_CHAR(sp.submission_period_date, 'MON-YYYY'), '')				AS "Submission Period",
	COALESCE(sp.area_of_law, '')                                    			AS "Area of Law",
	COALESCE(cl.client_forename, '') 											AS "Client Forename",
	COALESCE(cl.client_surname, '') 											AS "Client Surname",
	COALESCE(cl.unique_client_number, '') 										AS "Unique Client Number",
	COALESCE(al.unique_file_number, '') 										AS "Unique File Number",
	COALESCE(TO_CHAR(al.assessment_created_on, 'DD/MM/YYYY HH24:MI:SS'), '') 	AS "Amendment Date",
	COALESCE(ROUND(al.before_value, 2)::text, '') 								AS "Value before Amendment",
	COALESCE(ROUND(al.after_value, 2)::text, '') 								AS "Value after Amendment",
	COALESCE(ROUND(al.after_value - al.before_value, 2)::text, '') 				AS "Difference",
	'Escape Fee Case Assessment' 												AS "Reason of Amendment",
	COALESCE(sp.submission_id::text, '') 										AS "Submission ID",
    COALESCE(al.claim_id::text, '') 											AS "Claim ID",
    COALESCE(al.assessment_id::text, '') 										AS "Assessment ID"
FROM assessment_lines as al
JOIN submission_periods AS sp
	ON sp.submission_id = al.submission_id
LEFT JOIN claims.client AS cl
	ON cl.claim_id = al.claim_id

WHERE
	sp.submission_status = 'VALIDATION_SUCCEEDED' AND
	al.claim_status = 'VALID'
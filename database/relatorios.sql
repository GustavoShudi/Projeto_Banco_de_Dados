-- RELATÓRIO 1: Média de notas por avaliação

WITH notas_alunos AS (
    SELECT
        re.id_avaliacao,
        re.email_aluno,
        SUM(r.nota) AS nota_total
    FROM realiza re
    LEFT JOIN responde r
        ON r.id_avaliacao = re.id_avaliacao
       AND r.email_usuario = re.email_aluno
    GROUP BY re.id_avaliacao, re.email_aluno
)
SELECT
    a.id_avaliacao,
    a.titulo,
    m.nome AS materia,
    COUNT(na.nota_total) AS quantidade_alunos,
    ROUND(AVG(na.nota_total), 2) AS media_notas
FROM avaliacao a
JOIN materia m
    ON m.sigla = a.sigla_materia
LEFT JOIN notas_alunos na
    ON na.id_avaliacao = a.id_avaliacao
GROUP BY
    a.id_avaliacao,
    a.titulo,
    m.nome
ORDER BY a.data_avaliacao DESC;

-- RELATÓRIO 2: Aluno versus média
WITH notas_alunos AS (
    SELECT
        re.id_avaliacao,
        re.email_aluno,
        SUM(r.nota) AS nota_total
    FROM realiza re
    LEFT JOIN responde r
        ON r.id_avaliacao = re.id_avaliacao
       AND r.email_usuario = re.email_aluno
    GROUP BY re.id_avaliacao, re.email_aluno
)
SELECT
	a.id_avaliacao,
	a.titulo,
	aluno.nome,
	propria.nota_total,
	(
		SELECT ROUND(AVG(outros.nota_total), 2)
		FROM notas_alunos AS outros
		WHERE outros.id_avaliacao = propria.id_avaliacao
		AND outros.email_aluno <> propria.email_aluno
	) AS media_demais
FROM notas_alunos propria
JOIN avaliacao a
	ON a.id_avaliacao = propria.id_avaliacao
JOIN usuario aluno
	ON aluno.email = propria.email_aluno
WHERE propria.email_aluno = ?
ORDER BY a.data_avaliacao;
	
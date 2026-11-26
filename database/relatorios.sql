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

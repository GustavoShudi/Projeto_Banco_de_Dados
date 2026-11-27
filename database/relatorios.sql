
-- =====================================================
-- SISTEMA DE AVALIACOES
-- RELATORIOS SQL
-- PostgreSQL
-- =====================================================


-- =====================================================
-- RELATORIO 1
-- MEDIA DE CADA AVALIACAO
--
-- Recursos:
-- JOIN
-- LEFT JOIN
-- SUM
-- AVG
-- COUNT
-- GROUP BY
-- HAVING
-- ORDER BY
--
-- Considera somente alunos que responderam todas
-- as questoes e tiveram todas as discursivas corrigidas.
-- =====================================================

WITH notas AS (

    SELECT
        re.id_avaliacao,
        re.email_aluno,

        SUM(r.nota) AS nota_total,

        COUNT(aq.id_questao) AS total_questoes,

        COUNT(r.id_questao) AS respondidas,

        COUNT(*) FILTER (
            WHERE qs.id_questao IS NOT NULL
              AND r.nota IS NULL
        ) AS pendentes

    FROM realiza re

             JOIN avaliacao_questao aq
                  ON aq.id_avaliacao = re.id_avaliacao

             LEFT JOIN responde r
                       ON r.id_avaliacao = aq.id_avaliacao
                           AND r.id_questao = aq.id_questao
                           AND r.email_usuario = re.email_aluno

             LEFT JOIN questao_subjetiva qs
                       ON qs.id_questao = aq.id_questao

    GROUP BY
        re.id_avaliacao,
        re.email_aluno

),

     concluidas AS (

         SELECT *
         FROM notas

         WHERE total_questoes = respondidas
           AND pendentes = 0
     )

SELECT
    a.id_avaliacao,
    a.titulo,
    a.data_avaliacao,

    ROUND(AVG(c.nota_total), 2) AS media,

    COUNT(c.email_aluno) AS alunos_concluintes,

    (
        SELECT COUNT(*)
        FROM realiza re
        WHERE re.id_avaliacao = a.id_avaliacao
    ) AS alunos_inscritos

FROM avaliacao a

         LEFT JOIN concluidas c
                   ON c.id_avaliacao = a.id_avaliacao

GROUP BY
    a.id_avaliacao,
    a.titulo,
    a.data_avaliacao

ORDER BY
    a.data_avaliacao,
    a.id_avaliacao;


-- =====================================================
-- RELATORIO 2
-- DESEMPENHO DO ALUNO VERSUS MEDIA DOS DEMAIS
--
-- Recursos:
-- JOIN
-- Subconsulta correlacionada
-- AVG
-- GROUP BY
-- ORDER BY
--
-- Parametro: email do aluno.
-- =====================================================

WITH notas AS (

    SELECT
        re.id_avaliacao,
        re.email_aluno,

        SUM(r.nota) AS nota_total,

        COUNT(aq.id_questao) AS total_questoes,

        COUNT(r.id_questao) AS respondidas,

        COUNT(*) FILTER (
            WHERE qs.id_questao IS NOT NULL
              AND r.nota IS NULL
        ) AS pendentes

    FROM realiza re

             JOIN avaliacao_questao aq
                  ON aq.id_avaliacao = re.id_avaliacao

             LEFT JOIN responde r
                       ON r.id_avaliacao = aq.id_avaliacao
                           AND r.id_questao = aq.id_questao
                           AND r.email_usuario = re.email_aluno

             LEFT JOIN questao_subjetiva qs
                       ON qs.id_questao = aq.id_questao

    GROUP BY
        re.id_avaliacao,
        re.email_aluno

),

     concluidas AS (

         SELECT *
         FROM notas

         WHERE total_questoes = respondidas
           AND pendentes = 0
     )

SELECT
    a.id_avaliacao,
    a.titulo,

    aluno.nota_total AS nota_aluno,

    (
        SELECT ROUND(AVG(outros.nota_total), 2)

        FROM concluidas outros

        WHERE outros.id_avaliacao = a.id_avaliacao
          AND outros.email_aluno <> aluno.email_aluno

    ) AS media_demais_alunos

FROM concluidas aluno

         JOIN avaliacao a
              ON a.id_avaliacao = aluno.id_avaliacao

WHERE aluno.email_aluno = :email

ORDER BY a.data_avaliacao;


-- =====================================================
-- RELATORIO 3
-- EVOLUCAO DAS NOTAS DO ALUNO
--
-- Recursos:
-- JOIN
-- SUM
-- COUNT
-- FILTER
-- WHERE
-- BETWEEN
-- ORDER BY
--
-- Parametros:
-- :email
-- :de
-- :ate
-- =====================================================

WITH notas AS (

    SELECT
        re.id_avaliacao,
        re.email_aluno,

        SUM(r.nota) AS nota_total,

        COUNT(aq.id_questao) AS total_questoes,

        COUNT(r.id_questao) AS respondidas,

        COUNT(*) FILTER (
            WHERE qs.id_questao IS NOT NULL
              AND r.nota IS NULL
        ) AS pendentes

    FROM realiza re

             JOIN avaliacao_questao aq
                  ON aq.id_avaliacao = re.id_avaliacao

             LEFT JOIN responde r
                       ON r.id_avaliacao = aq.id_avaliacao
                           AND r.id_questao = aq.id_questao
                           AND r.email_usuario = re.email_aluno

             LEFT JOIN questao_subjetiva qs
                       ON qs.id_questao = aq.id_questao

    GROUP BY
        re.id_avaliacao,
        re.email_aluno

)

SELECT
    a.id_avaliacao,
    a.titulo,
    a.data_avaliacao,

    ROUND(n.nota_total, 2) AS nota

FROM notas n

         JOIN avaliacao a
              ON a.id_avaliacao = n.id_avaliacao

WHERE n.email_aluno = :email

  AND n.total_questoes = n.respondidas
  AND n.pendentes = 0

  AND a.data_avaliacao BETWEEN :de AND :ate

ORDER BY
    a.data_avaliacao,
    a.id_avaliacao;


-- =====================================================
-- RELATORIO 4
-- QUESTOES COM MAIOR PERCENTUAL DE ERROS
--
-- Recursos:
-- JOIN
-- LEFT JOIN
-- COUNT
-- FILTER
-- GROUP BY
-- ORDER BY
-- FETCH FIRST
-- WHERE
-- LIKE
--
-- A consulta considera:
-- Objetivas: alternativa incorreta.
-- Subjetivas: nota inferior ao valor da questão.
--
-- A versão do DAO aceita os tipos e a busca
-- como parametros dinamicos.
-- =====================================================

SELECT
    q.id_questao,
    q.enunciado,
    q.tipo,

    COUNT(*) AS total_respostas,

    COUNT(*) FILTER (
        WHERE
            (
                q.tipo = 'OBJETIVA'
                AND alt.correta = FALSE
            )
            OR
            (
                q.tipo = 'SUBJETIVA'
                AND r.nota < aq.valor_questao
            )
    ) AS respostas_erradas,

    ROUND(
            100.0 *

            COUNT(*) FILTER (
            WHERE
                (
                    q.tipo = 'OBJETIVA'
                    AND alt.correta = FALSE
                )
                OR
                (
                    q.tipo = 'SUBJETIVA'
                    AND r.nota < aq.valor_questao
                )
        )

        / NULLIF(COUNT(*), 0),

            2
    ) AS percentual_erros

FROM responde r

         JOIN questao q
              ON q.id_questao = r.id_questao

         JOIN avaliacao_questao aq
              ON aq.id_avaliacao = r.id_avaliacao
                  AND aq.id_questao = r.id_questao

         LEFT JOIN alternativa alt
                   ON alt.id_questao_objetiva = r.id_questao
                       AND alt.id_alternativa = r.id_alternativa_escolhida

WHERE q.tipo IN ('OBJETIVA', 'SUBJETIVA')

  AND q.enunciado ILIKE '%%'

  AND (
      (
          q.tipo = 'OBJETIVA'
          AND alt.correta IS NOT NULL
      )
      OR
      (
          q.tipo = 'SUBJETIVA'
          AND r.nota IS NOT NULL
      )
  )

GROUP BY
    q.id_questao,
    q.enunciado,
    q.tipo

ORDER BY
    percentual_erros DESC,
    total_respostas DESC

    FETCH FIRST 10 ROWS ONLY;


-- =====================================================
-- RELATORIO 5
-- ALUNOS QUE AINDA NAO RESPONDERAM
--
-- Recursos:
-- LEFT JOIN
-- WHERE
-- ORDER BY
--
-- Identifica alunos designados que ainda nao
-- possuem nenhuma resposta registrada.
-- =====================================================

SELECT
    a.id_avaliacao,
    a.titulo,
    a.data_avaliacao,

    u.email,
    u.nome

FROM realiza re

         JOIN avaliacao a
              ON a.id_avaliacao = re.id_avaliacao

         JOIN usuario u
              ON u.email = re.email_aluno

         LEFT JOIN responde r
                   ON r.id_avaliacao = re.id_avaliacao
                       AND r.email_usuario = re.email_aluno

WHERE r.id_questao IS NULL

ORDER BY
    a.data_avaliacao,
    a.titulo,
    u.nome;

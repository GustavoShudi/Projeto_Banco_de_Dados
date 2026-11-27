
package br.uel.avaliacoes.dao;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class RelatorioDAO {

    private final DataSource ds;

    public RelatorioDAO(DataSource ds) {
        this.ds = ds;
    }

    // ==========================================
    // RELATORIO 1
    // MEDIA POR AVALIACAO
    // ==========================================

    public List<Map<String, Object>> mediaPorAvaliacao() {

        return Db.consulta(
                ds,
                """
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

                    ROUND(
                        AVG(c.nota_total),
                        2
                    ) AS media,

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
                    a.id_avaliacao
                """
        );
    }

    // ==========================================
    // RELATORIO 2
    // ALUNO VERSUS MEDIA DA TURMA
    // ==========================================

    public List<Map<String, Object>> alunoVersusMedia(
            String email
    ) {

        return Db.consulta(
                ds,
                """
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

                    ROUND(
                        (
                            SELECT AVG(outros.nota_total)

                            FROM concluidas outros

                            WHERE outros.id_avaliacao = a.id_avaliacao
                              AND outros.email_aluno <> aluno.email_aluno
                        ),
                        2
                    ) AS media_demais_alunos

                FROM concluidas aluno

                JOIN avaliacao a
                    ON a.id_avaliacao = aluno.id_avaliacao

                WHERE aluno.email_aluno = ?

                ORDER BY a.data_avaliacao
                """,
                email
        );
    }

    // ==========================================
    // RELATORIO 3
    // EVOLUCAO DO ALUNO
    // ==========================================

    public List<Map<String, Object>> evolucao(
            String email,
            LocalDate de,
            LocalDate ate
    ) {

        return Db.consulta(
                ds,
                """
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

                WHERE n.email_aluno = ?
                  AND n.total_questoes = n.respondidas
                  AND n.pendentes = 0
                  AND a.data_avaliacao BETWEEN ? AND ?

                ORDER BY
                    a.data_avaliacao,
                    a.id_avaliacao
                """,
                email,
                de,
                ate
        );
    }

    // ==========================================
    // RELATORIO 4
    // QUESTOES MAIS ERRADAS
    // ==========================================

    public List<Map<String, Object>> questoesMaisErradas(
            List<String> tipos,
            String busca
    ) {

        if (tipos == null || tipos.isEmpty()) {

            throw new IllegalArgumentException(
                    "Selecione pelo menos um tipo de questão."
            );
        }

        List<String> permitidos =
                tipos.stream()
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .filter(t ->
                                t.equals("OBJETIVA")
                                        || t.equals("SUBJETIVA")
                        )
                        .distinct()
                        .toList();

        if (permitidos.isEmpty()) {

            throw new IllegalArgumentException(
                    "Tipos de questão inválidos."
            );
        }

        String marcadores = String.join(
                ",",
                java.util.Collections.nCopies(
                        permitidos.size(),
                        "?"
                )
        );

        String sql = """
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

                WHERE q.tipo IN (
                    %s
                )

                  AND q.enunciado ILIKE ?

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

                FETCH FIRST 10 ROWS ONLY
                """.formatted(marcadores);

        List<Object> parametros = new ArrayList<>();

        parametros.addAll(permitidos);

        parametros.add(
                "%" + (busca == null ? "" : busca.trim()) + "%"
        );

        return Db.consulta(
                ds,
                sql,
                parametros.toArray()
        );
    }

    // ==========================================
    // RELATORIO 5
    // QUEM NAO RESPONDEU
    // ==========================================

    public List<Map<String, Object>> quemNaoRespondeu() {

        return Db.consulta(
                ds,
                """
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
                    u.nome
                """
        );
    }
}

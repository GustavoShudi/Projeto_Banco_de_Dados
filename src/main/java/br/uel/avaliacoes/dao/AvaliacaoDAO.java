package br.uel.avaliacoes.dao;

import br.uel.avaliacoes.model.Model.Avaliacao;
import br.uel.avaliacoes.model.Model.Avaliacao.Item;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AvaliacaoDAO {

    private final DataSource ds;

    public AvaliacaoDAO(DataSource ds) {
        this.ds = ds;
    }

    // ==========================================
    // CRIAR AVALIACAO
    // ==========================================

    public int inserir(Avaliacao a) {

        if (a == null
                || a.titulo() == null
                || a.titulo().isBlank()
                || a.descricao() == null
                || a.data() == null
                || a.inicio() == null
                || a.fim() == null
                || a.emailProfessor() == null
                || a.siglaMateria() == null) {

            throw new IllegalArgumentException(
                    "Preencha todos os dados da avaliação."
            );
        }

        if (!a.fim().isAfter(a.inicio())) {

            throw new IllegalArgumentException(
                    "O horário final deve ser posterior ao inicial."
            );
        }

        if (a.itens() == null || a.itens().isEmpty()) {

            throw new IllegalArgumentException(
                    "Selecione pelo menos uma questão."
            );
        }

        if (a.alunos() == null || a.alunos().isEmpty()) {

            throw new IllegalArgumentException(
                    "Selecione pelo menos um aluno."
            );
        }

        return Db.transacao(ds, con -> {

            int id;

            // Criar avaliação

            try (
                    PreparedStatement ps = con.prepareStatement(
                            """
                            INSERT INTO avaliacao (
                                titulo,
                                descricao,
                                data_avaliacao,
                                horario_inicio,
                                horario_fim,
                                email_professor,
                                sigla_materia
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            RETURNING id_avaliacao
                            """
                    )
            ) {

                ps.setString(1, a.titulo());
                ps.setString(2, a.descricao());
                ps.setObject(3, a.data());
                ps.setObject(4, a.inicio());
                ps.setObject(5, a.fim());
                ps.setString(6, a.emailProfessor());
                ps.setString(7, a.siglaMateria());

                try (ResultSet rs = ps.executeQuery()) {

                    rs.next();

                    id = rs.getInt("id_avaliacao");
                }
            }

            // Associar questões e seus valores

            try (
                    PreparedStatement ps = con.prepareStatement(
                            """
                            INSERT INTO avaliacao_questao (
                                id_avaliacao,
                                id_questao,
                                valor_questao
                            )
                            VALUES (?, ?, ?)
                            """
                    )
            ) {

                for (Item item : a.itens()) {

                    if (item.valor() == null
                            || item.valor().compareTo(BigDecimal.ZERO) <= 0) {

                        throw new IllegalArgumentException(
                                "O valor de cada questão deve ser maior que zero."
                        );
                    }

                    ps.setInt(1, id);
                    ps.setInt(2, item.idQuestao());
                    ps.setBigDecimal(3, item.valor());

                    ps.addBatch();
                }

                ps.executeBatch();
            }

            // Associar alunos à avaliação

            try (
                    PreparedStatement ps = con.prepareStatement(
                            """
                            INSERT INTO realiza (
                                id_avaliacao,
                                email_aluno
                            )
                            VALUES (?, ?)
                            """
                    )
            ) {

                for (String email : a.alunos()) {

                    ps.setInt(1, id);
                    ps.setString(2, email);

                    ps.addBatch();
                }

                ps.executeBatch();
            }

            return id;
        });
    }

    // ==========================================
    // LISTAR AVALIACOES
    // ==========================================

    public List<Map<String, Object>> listar() {

        return listar(null);
    }

    /*
     * Se o e-mail do professor for informado,
     * lista somente as avaliações criadas por ele.
     * Sem e-mail, lista todas.
     */

    public List<Map<String, Object>> listar(
            String emailProfessor
    ) {

        boolean filtrar =
                emailProfessor != null
                        && !emailProfessor.isBlank();

        String filtro =
                filtrar
                        ? "WHERE a.email_professor = ?"
                        : "";

        String sql =
                """
                SELECT
                    a.id_avaliacao,
                    a.titulo,
                    a.descricao,
                    a.data_avaliacao,
                    a.horario_inicio,
                    a.horario_fim,
                    a.sigla_materia,
                    m.nome AS materia,
                    u.nome AS professor,

                    (
                        SELECT COUNT(*)
                        FROM avaliacao_questao aq
                        WHERE aq.id_avaliacao = a.id_avaliacao
                    ) AS questoes,

                    (
                        SELECT COUNT(*)
                        FROM realiza re
                        WHERE re.id_avaliacao = a.id_avaliacao
                    ) AS alunos

                FROM avaliacao a

                JOIN materia m
                    ON m.sigla = a.sigla_materia

                JOIN usuario u
                    ON u.email = a.email_professor

                %s

                ORDER BY
                    a.data_avaliacao DESC,
                    a.id_avaliacao DESC
                """.formatted(filtro);

        return Db.consulta(
                ds,
                sql,
                filtrar
                        ? new Object[]{emailProfessor.trim()}
                        : new Object[0]
        );
    }

    // ==========================================
    // AVALIACOES DO ALUNO
    // ==========================================

    public List<Map<String, Object>> listarDoAluno(
            String email
    ) {

        return Db.consulta(
                ds,
                """
                SELECT
                    a.id_avaliacao,
                    a.titulo,
                    a.descricao,
                    a.data_avaliacao,
                    m.nome AS materia,
                    re.nota_total,

                    (
                        SELECT COALESCE(SUM(aq.valor_questao), 0)
                        FROM avaliacao_questao aq
                        WHERE aq.id_avaliacao = a.id_avaliacao
                    ) AS valor_total,

                    (
                        SELECT COUNT(*)
                        FROM responde r
                        WHERE r.id_avaliacao = a.id_avaliacao
                          AND r.email_usuario = re.email_aluno
                    ) AS respondidas,

                    EXISTS (
                        SELECT 1
                        FROM avaliacao_questao aq
                        JOIN questao_subjetiva qs
                            ON qs.id_questao = aq.id_questao
                        LEFT JOIN responde r
                            ON r.id_avaliacao = aq.id_avaliacao
                           AND r.id_questao = aq.id_questao
                           AND r.email_usuario = re.email_aluno
                        WHERE aq.id_avaliacao = a.id_avaliacao
                          AND r.id_questao IS NOT NULL
                          AND r.nota IS NULL
                    ) AS aguardando_correcao

                FROM realiza re

                JOIN avaliacao a
                    ON a.id_avaliacao = re.id_avaliacao

                JOIN materia m
                    ON m.sigla = a.sigla_materia

                WHERE re.email_aluno = ?

                ORDER BY a.data_avaliacao
                """,
                email
        );
    }

    // ==========================================
    // QUESTOES DA PROVA
    // ==========================================

    public List<Map<String, Object>> prova(
            int idAvaliacao
    ) {

        List<Map<String, Object>> questoes =
                Db.consulta(
                        ds,
                        """
                        SELECT
                            q.id_questao AS id,
                            q.tipo,
                            q.enunciado,
                            aq.valor_questao AS valor

                        FROM avaliacao_questao aq

                        JOIN questao q
                            ON q.id_questao = aq.id_questao

                        WHERE aq.id_avaliacao = ?

                        ORDER BY q.id_questao
                        """,
                        idAvaliacao
                );

        for (Map<String, Object> q : questoes) {

            if ("OBJETIVA".equals(q.get("tipo"))) {

                q.put(
                        "opcoes",
                        Db.consulta(
                                ds,
                                """
                                SELECT
                                    id_alternativa AS id,
                                    texto
                                FROM alternativa
                                WHERE id_questao_objetiva = ?
                                ORDER BY id_alternativa
                                """,
                                q.get("id")
                        )
                );

            } else {

                q.put("opcoes", List.of());
            }
        }

        return questoes;
    }

    // ==========================================
    // SALVAR RESPOSTAS DO ALUNO
    // ==========================================

    public void salvarRespostas(
            String email,
            int idAvaliacao,
            Map<String, String> respostas
    ) {

        if (email == null || email.isBlank()) {

            throw new IllegalArgumentException(
                    "Aluno não informado."
            );
        }

        if (respostas == null) {

            throw new IllegalArgumentException(
                    "Nenhuma resposta recebida."
            );
        }

        Db.transacao(ds, con -> {

            // Verificar se o aluno foi associado à avaliação

            try (
                    PreparedStatement ps = con.prepareStatement(
                            """
                            SELECT 1
                            FROM realiza
                            WHERE id_avaliacao = ?
                              AND email_aluno = ?
                            """
                    )
            ) {

                ps.setInt(1, idAvaliacao);
                ps.setString(2, email);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {

                        throw new IllegalArgumentException(
                                "Você não está autorizado a responder esta avaliação."
                        );
                    }
                }
            }

            // Buscar questões da avaliação

            List<Map<String, Object>> questoes =
                    Db.consulta(
                            ds,
                            """
                            SELECT
                                q.id_questao,
                                q.tipo,
                                aq.valor_questao
                            FROM avaliacao_questao aq
                            JOIN questao q
                                ON q.id_questao = aq.id_questao
                            WHERE aq.id_avaliacao = ?
                            ORDER BY q.id_questao
                            """,
                            idAvaliacao
                    );

            if (questoes.isEmpty()) {

                throw new IllegalArgumentException(
                        "Avaliação sem questões."
                );
            }

            // Preparar gravação

            try (
                    PreparedStatement ps = con.prepareStatement(
                            """
                            INSERT INTO responde (
                                id_avaliacao,
                                email_usuario,
                                id_questao,
                                resposta_subjetiva,
                                id_alternativa_escolhida,
                                nota
                            )
                            VALUES (?, ?, ?, ?, ?, ?)

                            ON CONFLICT (
                                id_avaliacao,
                                email_usuario,
                                id_questao
                            )
                            DO UPDATE SET
                                resposta_subjetiva = EXCLUDED.resposta_subjetiva,
                                id_alternativa_escolhida = EXCLUDED.id_alternativa_escolhida,
                                nota = EXCLUDED.nota
                            """
                    )
            ) {

                for (Map<String, Object> q : questoes) {

                    int idQuestao =
                            Db.inteiro(q.get("id_questao"));

                    String tipo =
                            (String) q.get("tipo");

                    BigDecimal valor =
                            (BigDecimal) q.get("valor_questao");

                    String resposta =
                            respostas.getOrDefault(
                                    String.valueOf(idQuestao),
                                    ""
                            );

                    String respostaSubjetiva = null;

                    Integer idAlternativa = null;

                    BigDecimal nota = null;

                    if ("OBJETIVA".equals(tipo)) {

                        nota = BigDecimal.ZERO;

                        if (!resposta.isBlank()) {

                            try {

                                idAlternativa =
                                        Integer.parseInt(resposta);

                            } catch (NumberFormatException e) {

                                throw new IllegalArgumentException(
                                        "Alternativa inválida na questão "
                                                + idQuestao + "."
                                );
                            }

                            // Verificar se a alternativa pertence à questão

                            boolean correta = false;

                            try (
                                    PreparedStatement psAlt =
                                            con.prepareStatement(
                                                    """
                                                    SELECT correta
                                                    FROM alternativa
                                                    WHERE id_questao_objetiva = ?
                                                      AND id_alternativa = ?
                                                    """
                                            )
                            ) {

                                psAlt.setInt(1, idQuestao);
                                psAlt.setInt(2, idAlternativa);

                                try (ResultSet rs = psAlt.executeQuery()) {

                                    if (!rs.next()) {

                                        throw new IllegalArgumentException(
                                                "Alternativa não pertence à questão."
                                        );
                                    }

                                    correta = rs.getBoolean("correta");
                                }
                            }

                            if (correta) {

                                nota = valor;
                            }
                        }

                    } else {

                        respostaSubjetiva = resposta;

                        // A nota será atribuída pelo professor.
                        nota = null;
                    }

                    ps.setInt(1, idAvaliacao);
                    ps.setString(2, email);
                    ps.setInt(3, idQuestao);

                    ps.setString(4, respostaSubjetiva);

                    if (idAlternativa == null) {

                        ps.setNull(
                                5,
                                java.sql.Types.BIGINT
                        );

                    } else {

                        ps.setInt(5, idAlternativa);
                    }

                    if (nota == null) {

                        ps.setNull(
                                6,
                                java.sql.Types.NUMERIC
                        );

                    } else {

                        ps.setBigDecimal(6, nota);
                    }

                    ps.addBatch();
                }

                ps.executeBatch();
            }

            recalcularNotaTotal(
                    con,
                    idAvaliacao,
                    email
            );

            return null;
        });
    }

    // ==========================================
    // CALCULAR NOTA TOTAL
    // ==========================================

    private void recalcularNotaTotal(
            Connection con,
            int idAvaliacao,
            String email
    ) throws java.sql.SQLException {

        /*
         * A nota total só é atualizada quando
         * todas as questões discursivas foram corrigidas.
         */

        String sql =
                """
                UPDATE realiza re

                SET nota_total = (
                    SELECT COALESCE(SUM(r.nota), 0)
                    FROM responde r
                    WHERE r.id_avaliacao = re.id_avaliacao
                      AND r.email_usuario = re.email_aluno
                )

                WHERE re.id_avaliacao = ?
                  AND re.email_aluno = ?

                  AND NOT EXISTS (
                      SELECT 1
                      FROM avaliacao_questao aq

                      JOIN questao_subjetiva qs
                          ON qs.id_questao = aq.id_questao

                      LEFT JOIN responde r
                          ON r.id_avaliacao = aq.id_avaliacao
                         AND r.id_questao = aq.id_questao
                         AND r.email_usuario = re.email_aluno

                      WHERE aq.id_avaliacao = re.id_avaliacao
                        AND r.nota IS NULL
                  )
                """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idAvaliacao);
            ps.setString(2, email);

            ps.executeUpdate();
        }
    }

    // ==========================================
    // RESULTADO DO ALUNO
    // ==========================================

    public Map<String, Object> resultado(
            String email,
            int idAvaliacao
    ) {

        List<Map<String, Object>> total =
                Db.consulta(
                        ds,
                        """
                        SELECT
                            re.nota_total,

                            (
                                SELECT COALESCE(SUM(aq.valor_questao), 0)
                                FROM avaliacao_questao aq
                                WHERE aq.id_avaliacao = re.id_avaliacao
                            ) AS valor_total,

                            EXISTS (
                                SELECT 1
                                FROM avaliacao_questao aq
                                JOIN questao_subjetiva qs
                                    ON qs.id_questao = aq.id_questao
                                LEFT JOIN responde r
                                    ON r.id_avaliacao = aq.id_avaliacao
                                   AND r.id_questao = aq.id_questao
                                   AND r.email_usuario = re.email_aluno
                                WHERE aq.id_avaliacao = re.id_avaliacao
                                  AND r.id_questao IS NOT NULL
                                  AND r.nota IS NULL
                            ) AS aguardando_correcao

                        FROM realiza re

                        WHERE re.email_aluno = ?
                          AND re.id_avaliacao = ?
                        """,
                        email,
                        idAvaliacao
                );

        if (total.isEmpty()) {

            throw new IllegalArgumentException(
                    "Avaliação não designada a este aluno."
            );
        }

        Map<String, Object> resposta =
                new LinkedHashMap<>();

        resposta.put(
                "itens",
                Db.consulta(
                        ds,
                        """
                        SELECT
                            q.id_questao,
                            q.enunciado,
                            q.tipo,
                            aq.valor_questao AS valor,
                            r.resposta_subjetiva AS resposta_aluno,
                            alt.texto AS alternativa_escolhida,
                            r.nota AS nota_obtida

                        FROM avaliacao_questao aq

                        JOIN questao q
                            ON q.id_questao = aq.id_questao

                        LEFT JOIN responde r
                            ON r.id_avaliacao = aq.id_avaliacao
                           AND r.id_questao = aq.id_questao
                           AND r.email_usuario = ?

                        LEFT JOIN alternativa alt
                            ON alt.id_questao_objetiva = r.id_questao
                           AND alt.id_alternativa = r.id_alternativa_escolhida

                        WHERE aq.id_avaliacao = ?

                        ORDER BY q.id_questao
                        """,
                        email,
                        idAvaliacao
                )
        );

        resposta.put(
                "total",
                total.get(0)
        );

        return resposta;
    }

    // ==========================================
    // QUESTOES PENDENTES DE CORRECAO
    // ==========================================

    public List<Map<String, Object>> pendentesDeCorrecao() {

        return pendentesDeCorrecao(null);
    }

    /*
     * Se o e-mail do professor for informado,
     * lista somente as pendências das avaliações dele.
     * Sem e-mail, lista todas.
     */

    public List<Map<String, Object>> pendentesDeCorrecao(
            String emailProfessor
    ) {

        boolean filtrar =
                emailProfessor != null
                        && !emailProfessor.isBlank();

        String filtro =
                filtrar
                        ? "AND a.email_professor = ?"
                        : "";

        String sql =
                """
                SELECT
                    r.email_usuario AS email_aluno,
                    u.nome,
                    r.id_avaliacao,
                    a.titulo,
                    r.id_questao,
                    q.enunciado,
                    qs.expectativa,
                    aq.valor_questao AS valor,
                    r.resposta_subjetiva AS resposta_aluno

                FROM responde r

                JOIN usuario u
                    ON u.email = r.email_usuario

                JOIN avaliacao a
                    ON a.id_avaliacao = r.id_avaliacao

                JOIN questao q
                    ON q.id_questao = r.id_questao

                JOIN questao_subjetiva qs
                    ON qs.id_questao = r.id_questao

                JOIN avaliacao_questao aq
                    ON aq.id_avaliacao = r.id_avaliacao
                   AND aq.id_questao = r.id_questao

                WHERE r.nota IS NULL

                %s

                ORDER BY
                    a.data_avaliacao,
                    u.nome,
                    r.id_questao
                """.formatted(filtro);

        return Db.consulta(
                ds,
                sql,
                filtrar
                        ? new Object[]{emailProfessor.trim()}
                        : new Object[0]
        );
    }

    // ==========================================
    // CORRIGIR QUESTAO SUBJETIVA
    // ==========================================

    public void corrigir(
            String email,
            int idAvaliacao,
            int idQuestao,
            BigDecimal nota
    ) {

        if (nota == null
                || nota.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Informe uma nota válida."
            );
        }

        Db.transacao(ds, con -> {

            BigDecimal valorMaximo;

            // Buscar valor e verificar questão

            try (
                    PreparedStatement ps = con.prepareStatement(
                            """
                            SELECT
                                aq.valor_questao

                            FROM avaliacao_questao aq

                            JOIN questao_subjetiva qs
                                ON qs.id_questao = aq.id_questao

                            WHERE aq.id_avaliacao = ?
                              AND aq.id_questao = ?
                            """
                    )
            ) {

                ps.setInt(1, idAvaliacao);
                ps.setInt(2, idQuestao);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {

                        throw new IllegalArgumentException(
                                "Questão discursiva não encontrada."
                        );
                    }

                    valorMaximo =
                            rs.getBigDecimal("valor_questao");
                }
            }

            if (nota.compareTo(valorMaximo) > 0) {

                throw new IllegalArgumentException(
                        "A nota não pode ultrapassar o valor da questão: "
                                + valorMaximo
                );
            }

            // Atualizar nota

            try (
                    PreparedStatement ps = con.prepareStatement(
                            """
                            UPDATE responde

                            SET nota = ?

                            WHERE email_usuario = ?
                              AND id_avaliacao = ?
                              AND id_questao = ?
                            """
                    )
            ) {

                ps.setBigDecimal(1, nota);
                ps.setString(2, email);
                ps.setInt(3, idAvaliacao);
                ps.setInt(4, idQuestao);

                int alteradas = ps.executeUpdate();

                if (alteradas == 0) {

                    throw new IllegalArgumentException(
                            "Resposta do aluno não encontrada."
                    );
                }
            }

            // Atualizar nota total quando possível

            recalcularNotaTotal(
                    con,
                    idAvaliacao,
                    email
            );

            return null;
        });
    }
}
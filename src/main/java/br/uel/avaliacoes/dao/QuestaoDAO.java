
package br.uel.avaliacoes.dao;

import br.uel.avaliacoes.model.Model.Questao;
import br.uel.avaliacoes.model.Model.Questao.Alternativa;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class QuestaoDAO {

    private final DataSource ds;

    public QuestaoDAO(DataSource ds) {
        this.ds = ds;
    }

    // ==========================================
    // CADASTRAR QUESTAO
    // ==========================================

    public int inserir(Questao q) {

        if (q == null) {
            throw new IllegalArgumentException(
                    "Questão inválida."
            );
        }

        if (q.enunciado() == null
                || q.enunciado().isBlank()) {

            throw new IllegalArgumentException(
                    "Informe o enunciado."
            );
        }

        if (!"OBJETIVA".equals(q.tipo())
                && !"SUBJETIVA".equals(q.tipo())) {

            throw new IllegalArgumentException(
                    "Tipo de questão inválido."
            );
        }

        return Db.transacao(ds, con -> {

            int id;

            // Questão principal

            try (
                    PreparedStatement ps =
                            con.prepareStatement(
                                    """
                                    INSERT INTO questao
                                        (enunciado, tipo)
                                    VALUES (?, ?)
                                    RETURNING id_questao
                                    """
                            )
            ) {

                ps.setString(1, q.enunciado());
                ps.setString(2, q.tipo());

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {

                        throw new RuntimeException(
                                "Não foi possível cadastrar a questão."
                        );
                    }

                    id = rs.getInt("id_questao");
                }
            }

            // ==================================
            // QUESTAO OBJETIVA
            // ==================================

            if ("OBJETIVA".equals(q.tipo())) {

                if (q.alternativas() == null
                        || q.alternativas().size() < 2) {

                    throw new IllegalArgumentException(
                            "Informe pelo menos duas alternativas."
                    );
                }

                long corretas = q.alternativas()
                        .stream()
                        .filter(Alternativa::correta)
                        .count();

                if (corretas != 1) {

                    throw new IllegalArgumentException(
                            "Marque exatamente uma alternativa correta."
                    );
                }

                for (Alternativa alternativa : q.alternativas()) {

                    if (alternativa.texto() == null
                            || alternativa.texto().isBlank()) {

                        throw new IllegalArgumentException(
                                "Todas as alternativas precisam ter texto."
                        );
                    }
                }

                try (
                        PreparedStatement ps =
                                con.prepareStatement(
                                        """
                                        INSERT INTO questao_objetiva
                                            (id_questao)
                                        VALUES (?)
                                        """
                                )
                ) {

                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                try (
                        PreparedStatement ps =
                                con.prepareStatement(
                                        """
                                        INSERT INTO alternativa
                                            (id_questao_objetiva, texto, correta)
                                        VALUES (?, ?, ?)
                                        """
                                )
                ) {

                    for (Alternativa a : q.alternativas()) {

                        ps.setInt(1, id);
                        ps.setString(2, a.texto());
                        ps.setBoolean(3, a.correta());

                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

            } else {

                // ==============================
                // QUESTAO SUBJETIVA
                // ==============================

                if (q.expectativa() == null
                        || q.expectativa().isBlank()) {

                    throw new IllegalArgumentException(
                            "Informe a expectativa de resposta."
                    );
                }

                try (
                        PreparedStatement ps =
                                con.prepareStatement(
                                        """
                                        INSERT INTO questao_subjetiva
                                            (id_questao, expectativa)
                                        VALUES (?, ?)
                                        """
                                )
                ) {

                    ps.setInt(1, id);
                    ps.setString(2, q.expectativa());

                    ps.executeUpdate();
                }
            }

            return id;
        });
    }

    // ==========================================
    // LISTAR QUESTOES
    // ==========================================

    public List<Questao> listar() {

        Map<Integer, List<Alternativa>> alternativas =
                new LinkedHashMap<>();

        // Buscar alternativas

        List<Map<String, Object>> listaAlternativas =
                Db.consulta(
                        ds,
                        """
                        SELECT
                            id_questao_objetiva,
                            id_alternativa,
                            texto,
                            correta
                        FROM alternativa
                        ORDER BY id_questao_objetiva, id_alternativa
                        """
                );

        for (Map<String, Object> a : listaAlternativas) {

            int idQuestao =
                    Db.inteiro(a.get("id_questao_objetiva"));

            Alternativa alternativa = new Alternativa(
                    Db.inteiro(a.get("id_alternativa")),
                    (String) a.get("texto"),
                    (Boolean) a.get("correta")
            );

            alternativas
                    .computeIfAbsent(
                            idQuestao,
                            k -> new ArrayList<>()
                    )
                    .add(alternativa);
        }

        // Buscar questões

        List<Map<String, Object>> questoes =
                Db.consulta(
                        ds,
                        """
                        SELECT
                            q.id_questao,
                            q.tipo,
                            q.enunciado,
                            s.expectativa
                        FROM questao q
                        LEFT JOIN questao_subjetiva s
                            ON s.id_questao = q.id_questao
                        ORDER BY q.id_questao
                        """
                );

        List<Questao> resultado = new ArrayList<>();

        for (Map<String, Object> q : questoes) {

            int id = Db.inteiro(q.get("id_questao"));

            resultado.add(
                    new Questao(
                            id,
                            (String) q.get("tipo"),
                            (String) q.get("enunciado"),
                            (String) q.get("expectativa"),
                            alternativas.getOrDefault(
                                    id,
                                    List.of()
                            )
                    )
            );
        }

        return resultado;
    }

    // ==========================================
    // BUSCAR QUESTAO POR ID
    // ==========================================

    public Questao buscar(int id) {

        return listar()
                .stream()
                .filter(q -> q.id() == id)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Questão não encontrada."
                        )
                );
    }
}

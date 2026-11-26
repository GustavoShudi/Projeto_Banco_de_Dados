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

    /**
     * Generalização: grava em questao e na tabela do subtipo (objetiva + alternativas,
     * ou subjetiva + expectativa), tudo na MESMA transação.
     */
    public int inserir(Questao q) {
        return Db.transacao(ds, con -> {
            int id;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO questao (enunciado, tipo) VALUES (?, ?) RETURNING id_questao")) {
                ps.setString(1, q.enunciado());
                ps.setString(2, q.tipo());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    id = rs.getInt(1);
                }
            }
            if ("OBJETIVA".equals(q.tipo())) {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO questao_objetiva (id_questao) VALUES (?)")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO alternativa (id_questao_objetiva, texto, correta) VALUES (?, ?, ?)")) {
                    for (Alternativa a : q.alternativas()) {
                        ps.setInt(1, id);
                        ps.setString(2, a.texto());
                        ps.setBoolean(3, a.correta());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO questao_subjetiva (id_questao, expectativa) VALUES (?, ?)")) {
                    ps.setInt(1, id);
                    ps.setString(2, q.expectativa());
                    ps.executeUpdate();
                }
            }
            return id;
        });
    }

    public List<Questao> listar() {
        Map<Integer, List<Alternativa>> alts = new LinkedHashMap<>();
        for (Map<String, Object> a : Db.consulta(ds,
                "SELECT id_questao_objetiva, texto, correta FROM alternativa ORDER BY id_questao_objetiva, id_alternativa")) {
            alts.computeIfAbsent(Db.inteiro(a.get("id_questao_objetiva")), k -> new ArrayList<>())
                .add(new Alternativa((String) a.get("texto"), (Boolean) a.get("correta")));
        }
        List<Questao> resultado = new ArrayList<>();
        for (Map<String, Object> q : Db.consulta(ds,
                "SELECT q.id_questao, q.tipo, q.enunciado, s.expectativa FROM questao q "
              + "LEFT JOIN questao_subjetiva s ON s.id_questao = q.id_questao ORDER BY q.id_questao")) {
            int id = Db.inteiro(q.get("id_questao"));
            resultado.add(new Questao(id, (String) q.get("tipo"), (String) q.get("enunciado"),
                    (String) q.get("expectativa"), alts.getOrDefault(id, List.of())));
        }
        return resultado;
    }
}
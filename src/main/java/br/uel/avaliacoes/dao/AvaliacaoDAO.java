package br.uel.avaliacoes.dao;

import br.uel.avaliacoes.model.Model.Avaliacao;
import br.uel.avaliacoes.model.Model.Avaliacao.Item;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AvaliacaoDAO {

    private final DataSource ds;

    public AvaliacaoDAO(DataSource ds) {
        this.ds = ds;
    }


    // Grava a Avaliação
    public int inserir(Avaliacao a) {
        return Db.transacao(ds, con -> {
            int id;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO avaliacao (titulo, descricao, data, horario_inicio, horario_fim, email_professor, sigla_materia) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_avaliacao")) {
                ps.setString(1, a.titulo());
                ps.setString(2, a.descricao());
                ps.setObject(3, a.data());
                ps.setObject(4, a.inicio());
                ps.setObject(5, a.fim());
                ps.setString(6, a.emailProfessor());
                ps.setString(7, a.siglaMateria());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    id = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO avaliacao_questao (id_avaliacao, id_questao, valor_questao) VALUES (?, ?, ?)")) {
                for (Item it : a.itens()) {
                    ps.setInt(1, id);
                    ps.setInt(2, it.idQuestao());
                    ps.setBigDecimal(3, it.valor());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO realiza (id_avaliacao, email_aluno) VALUES (?, ?)")) {
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

    public List<Map<String, Object>> listar() {
        return Db.consulta(ds,
                "SELECT a.id_avaliacao, a.titulo, a.descricao, a.data, a.horario_inicio, a.horario_fim, "
              + "       m.nome AS materia, u.nome AS professor, "
              + "       (SELECT COUNT(*) FROM avaliacao_questao aq WHERE aq.id_avaliacao = a.id_avaliacao) AS questoes, "
              + "       (SELECT COUNT(*) FROM realiza re WHERE re.id_avaliacao = a.id_avaliacao) AS alunos "
              + "FROM avaliacao a JOIN materia m ON m.sigla = a.sigla_materia "
              + "JOIN usuario u ON u.email = a.email_professor "
              + "ORDER BY a.data DESC, a.id_avaliacao DESC");
    }

    // Listar avaliações do aluno
    public List<Map<String, Object>> listarDoAluno(String email) {
        return Db.consulta(ds,
                "SELECT a.id_avaliacao, a.titulo, m.nome AS materia, a.data, "
              + "       n.nota_total, n.valor_total, n.respondidas, n.aguardando_correcao "
              + "FROM realiza re "
              + "JOIN avaliacao a ON a.id_avaliacao = re.id_avaliacao "
              + "JOIN materia m ON m.sigla = a.sigla_materia "
              + "JOIN nota_total_aluno n ON n.email_aluno = re.email_aluno AND n.id_avaliacao = re.id_avaliacao "
              + "WHERE re.email_aluno = ? ORDER BY a.data", email);
    }

    // Questões para o Aluno responder
    public List<Map<String, Object>> prova(int idAvaliacao) {
        List<Map<String, Object>> qs = Db.consulta(ds,
                "SELECT q.id_questao AS id, q.tipo, q.enunciado, aq.valor_questao AS valor "
              + "FROM avaliacao_questao aq JOIN questao q ON q.id_questao = aq.id_questao "
              + "WHERE aq.id_avaliacao = ? ORDER BY q.id_questao", idAvaliacao);
        for (Map<String, Object> q : qs) {
            q.put("opcoes", Db.consulta(ds, "SELECT texto FROM alternativa WHERE id_questao_objetiva = ? ORDER BY id_alternativa",
                    q.get("id")).stream().map(m -> m.get("texto")).toList());
        }
        return qs;
    }

    public void salvarRespostas(String email, int idAvaliacao, Map<String, String> respostas) {
        Db.transacao(ds, con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO responde (id_avaliacao, email_aluno, id_questao, resposta) VALUES (?, ?, ?, ?)")) {
                for (Map.Entry<String, String> r : respostas.entrySet()) {
                    if (r.getValue() == null || r.getValue().isBlank()) continue;
                    ps.setInt(1, idAvaliacao);
                    ps.setString(2, email);
                    ps.setInt(3, Integer.parseInt(r.getKey()));
                    ps.setString(4, r.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        });
    }

    public Map<String, Object> resultado(String email, int idAvaliacao) {
        List<Map<String, Object>> total = Db.consulta(ds,
                "SELECT nota_total, valor_total, aguardando_correcao FROM nota_total_aluno "
              + "WHERE email_aluno = ? AND id_avaliacao = ?", email, idAvaliacao);
        if (total.isEmpty()) throw new IllegalArgumentException("Avaliação não designada a este aluno.");
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("itens", Db.consulta(ds,
                "SELECT q.enunciado, aq.valor_questao AS valor, r.resposta AS resposta_aluno, r.nota AS nota_obtida "
              + "FROM avaliacao_questao aq JOIN questao q ON q.id_questao = aq.id_questao "
              + "LEFT JOIN responde r ON r.id_questao = aq.id_questao AND r.id_avaliacao = aq.id_avaliacao "
              + "                     AND r.email_aluno = ? "
              + "WHERE aq.id_avaliacao = ? ORDER BY q.id_questao", email, idAvaliacao));
        resp.put("total", total.get(0));
        return resp;
    }

    // Questões que Professor precisa corrigir
    public List<Map<String, Object>> pendentesDeCorrecao() {
        return Db.consulta(ds,
                "SELECT r.email_aluno, u.nome, r.id_avaliacao, a.titulo, r.id_questao, q.enunciado, "
              + "       s.expectativa, aq.valor_questao AS valor, r.resposta AS resposta_aluno "
              + "FROM responde r "
              + "JOIN usuario u ON u.email = r.email_aluno "
              + "JOIN avaliacao a ON a.id_avaliacao = r.id_avaliacao "
              + "JOIN questao q ON q.id_questao = r.id_questao "
              + "JOIN questao_subjetiva s ON s.id_questao = r.id_questao "
              + "JOIN avaliacao_questao aq ON aq.id_avaliacao = r.id_avaliacao AND aq.id_questao = r.id_questao "
              + "WHERE r.nota IS NULL ORDER BY a.data, u.nome, r.id_questao");
    }

    // Atribuir nota do Aluno
    public void corrigir(String email, int idAvaliacao, int idQuestao, BigDecimal nota) {
        int n = Db.atualiza(ds,
                "UPDATE responde SET nota = ? WHERE email_aluno = ? AND id_avaliacao = ? AND id_questao = ?",
                nota, email, idAvaliacao, idQuestao);
        if (n == 0) throw new IllegalArgumentException("Resposta não encontrada.");
    }
}


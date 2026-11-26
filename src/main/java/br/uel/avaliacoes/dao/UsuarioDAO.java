package br.uel.avaliacoes.dao;

import br.uel.avaliacoes.model.Model.Usuario;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class UsuarioDAO {

    private final DataSource ds;

    public UsuarioDAO(DataSource ds) {
        this.ds = ds;
    }

    public Optional<Usuario> login(String email, String senha) {
        return Db.consulta(ds, "SELECT email, nome, tipo FROM usuario WHERE email = ? AND senha = ?", email, senha)
                 .stream().findFirst()
                 .map(m -> new Usuario((String) m.get("email"), (String) m.get("nome"), (String) m.get("tipo")));
    }

    public List<Map<String, Object>> materiasDoUsuario(String email) {
        return Db.consulta(ds,
                "SELECT m.sigla, m.nome FROM possui p JOIN materia m ON m.sigla = p.sigla_materia "
              + "WHERE p.email_usuario = ? ORDER BY m.nome", email);
    }

    public List<Map<String, Object>> alunosDaMateria(String sigla) {
        return Db.consulta(ds,
                "SELECT u.email, u.nome FROM possui p "
              + "JOIN aluno a ON a.email_usuario = p.email_usuario "
              + "JOIN usuario u ON u.email = a.email_usuario "
              + "WHERE p.sigla_materia = ? ORDER BY u.nome", sigla);
    }

    public List<Map<String, Object>> listarAlunos() {
        return Db.consulta(ds,
                "SELECT u.email, u.nome FROM usuario u JOIN aluno a ON a.email_usuario = u.email ORDER BY u.nome");
    }
}
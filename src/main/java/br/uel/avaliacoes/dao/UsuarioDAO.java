
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

    // ==========================================
    // LOGIN
    // ==========================================

    public Optional<Usuario> login(
            String email,
            String senha
    ) {

        List<Map<String, Object>> resultado =
                Db.consulta(
                        ds,
                        """
                        SELECT email, nome, tipo
                        FROM usuario
                        WHERE LOWER(email) = LOWER(?)
                          AND senha = ?
                        """,
                        email,
                        senha
                );

        return resultado.stream()
                .findFirst()
                .map(m -> new Usuario(
                        (String) m.get("email"),
                        (String) m.get("nome"),
                        (String) m.get("tipo")
                ));
    }

    // ==========================================
    // MATERIAS DO PROFESSOR
    // ==========================================

    public List<Map<String, Object>> materiasDoUsuario(
            String email
    ) {

        return Db.consulta(
                ds,
                """
                SELECT m.sigla, m.nome
                FROM possui p
                JOIN materia m
                    ON m.sigla = p.sigla_materia
                WHERE p.email_usuario = ?
                ORDER BY m.nome
                """,
                email
        );
    }

    // ==========================================
    // ALUNOS DA MATERIA
    // ==========================================

    /*
     * O schema atual não possui relacionamento
     * entre aluno e matéria.
     *
     * Por isso, retornamos todos os alunos
     * cadastrados no sistema.
     */

    public List<Map<String, Object>> alunosDaMateria(
            String sigla
    ) {

        List<Map<String, Object>> materia =
                Db.consulta(
                        ds,
                        """
                        SELECT sigla
                        FROM materia
                        WHERE sigla = ?
                        """,
                        sigla
                );

        if (materia.isEmpty()) {

            throw new IllegalArgumentException(
                    "Matéria não encontrada."
            );
        }

        return listarAlunos();
    }

    // ==========================================
    // LISTAR ALUNOS
    // ==========================================

    public List<Map<String, Object>> listarAlunos() {

        return Db.consulta(
                ds,
                """
                SELECT
                    u.email,
                    u.nome
                FROM usuario u
                JOIN aluno a
                    ON a.email_usuario = u.email
                ORDER BY u.nome
                """
        );
    }

    // ==========================================
    // BUSCAR USUARIO
    // ==========================================

    public Optional<Usuario> buscar(String email) {

        return Db.consulta(
                        ds,
                        """
                        SELECT email, nome, tipo
                        FROM usuario
                        WHERE email = ?
                        """,
                        email
                ).stream()
                .findFirst()
                .map(m -> new Usuario(
                        (String) m.get("email"),
                        (String) m.get("nome"),
                        (String) m.get("tipo")
                ));
    }
}

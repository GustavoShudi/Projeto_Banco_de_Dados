
package br.uel.avaliacoes.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class Model {

    private Model() {
    }

    // ==========================================
    // USUARIO
    // ==========================================

    public record Usuario(
            String email,
            String nome,
            String tipo
    ) {
    }

    // ==========================================
    // QUESTAO
    // ==========================================

    public record Questao(
            Integer id,
            String tipo,
            String enunciado,
            String expectativa,
            List<Alternativa> alternativas
    ) {

        public record Alternativa(
                Integer id,
                String texto,
                boolean correta
        ) {
        }
    }

    // ==========================================
    // AVALIACAO
    // ==========================================

    public record Avaliacao(
            Integer id,
            String titulo,
            String descricao,
            LocalDate data,
            LocalTime inicio,
            LocalTime fim,
            String emailProfessor,
            String siglaMateria,
            List<Item> itens,
            List<String> alunos
    ) {

        public record Item(
                int idQuestao,
                BigDecimal valor
        ) {
        }
    }
}

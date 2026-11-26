package br.uel.avaliacoes.controller;

import br.uel.avaliacoes.dao.AvaliacaoDAO;
import br.uel.avaliacoes.dao.QuestaoDAO;
import br.uel.avaliacoes.dao.RelatorioDAO;
import br.uel.avaliacoes.dao.UsuarioDAO;
import br.uel.avaliacoes.model.Model.Avaliacao;
import br.uel.avaliacoes.model.Model.Questao;
import br.uel.avaliacoes.model.Model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api")
public class Controller {

    public record Login(String email, String senha) {}
    public record Envio(String email, Map<String, String> respostas) {}
    public record Nota(String email, int idAvaliacao, int idQuestao, BigDecimal nota) {}

    private final UsuarioDAO usuarios;
    private final QuestaoDAO questoes;
    private final AvaliacaoDAO avaliacoes;
    private final RelatorioDAO relatorios;

    public Controller(UsuarioDAO usuarios, QuestaoDAO questoes, AvaliacaoDAO avaliacoes, RelatorioDAO relatorios) {
        this.usuarios = usuarios;
        this.questoes = questoes;
        this.avaliacoes = avaliacoes;
        this.relatorios = relatorios;
    }

    /** Qualquer erro vira {"erro": "..."} com status 400, para a tela mostrar a mensagem. */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> erro(RuntimeException e) {
        return Map.of("erro", String.valueOf(e.getMessage()));
    }

    // ---------- login (simples, sem sessão) ----------
    @PostMapping("/login")
    public Usuario login(@RequestBody Login l) {
        return usuarios.login(l.email(), l.senha())
                       .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos."));
    }

    @GetMapping("/materias")
    public List<Map<String, Object>> materias(@RequestParam("email") String email) {
        return usuarios.materiasDoUsuario(email);
    }

    @GetMapping("/materias/{sigla}/alunos")
    public List<Map<String, Object>> alunosDaMateria(@PathVariable("sigla") String sigla) {
        return usuarios.alunosDaMateria(sigla);
    }

    @GetMapping("/alunos")
    public List<Map<String, Object>> alunos() {
        return usuarios.listarAlunos();
    }

    // ---------- RF1: questões ----------
    @GetMapping("/questoes")
    public List<Questao> listarQuestoes() {
        return questoes.listar();
    }

    @PostMapping("/questoes")
    public Map<String, Integer> criarQuestao(@RequestBody Questao q) {
        if (q.enunciado() == null || q.enunciado().isBlank()) {
            throw new IllegalArgumentException("Informe o enunciado.");
        }
        if ("OBJETIVA".equals(q.tipo())) {
            if (q.alternativas() == null || q.alternativas().size() < 2
                    || q.alternativas().stream().filter(Questao.Alternativa::correta).count() != 1) {
                throw new IllegalArgumentException("Informe ao menos 2 alternativas e marque exatamente 1 correta.");
            }
        } else if ("SUBJETIVA".equals(q.tipo())) {
            if (q.expectativa() == null || q.expectativa().isBlank()) {
                throw new IllegalArgumentException("Informe a expectativa de resposta.");
            }
        } else {
            throw new IllegalArgumentException("Tipo de questão inválido.");
        }
        return Map.of("id", questoes.inserir(q));
    }

    // ---------- RF2, RF3, RF4: avaliações ----------
    @GetMapping("/avaliacoes")
    public List<Map<String, Object>> listarAvaliacoes() {
        return avaliacoes.listar();
    }

    @PostMapping("/avaliacoes")
    public Map<String, Integer> criarAvaliacao(@RequestBody Avaliacao a) {
        if (a.titulo() == null || a.titulo().isBlank() || a.siglaMateria() == null || a.data() == null || a.inicio() == null || a.fim() == null) {
            throw new IllegalArgumentException("Preencha título, matéria, data, início e fim.");
        }
        if (a.itens() == null || a.itens().isEmpty() || a.alunos() == null || a.alunos().isEmpty()) {
            throw new IllegalArgumentException("Escolha ao menos uma questão e um aluno.");
        }
        return Map.of("id", avaliacoes.inserir(a));
    }

    // ---------- RF5: aluno responde ----------
    @GetMapping("/avaliacoes/do-aluno")
    public List<Map<String, Object>> doAluno(@RequestParam("email") String email) {
        return avaliacoes.listarDoAluno(email);
    }

    @GetMapping("/avaliacoes/{id}/prova")
    public List<Map<String, Object>> prova(@PathVariable("id") int id) {
        return avaliacoes.prova(id);
    }

    @PostMapping("/avaliacoes/{id}/respostas")
    public Map<String, String> responder(@PathVariable("id") int id, @RequestBody Envio e) {
        avaliacoes.salvarRespostas(e.email(), id, e.respostas());
        return Map.of("ok", "Respostas registradas.");
    }

    @GetMapping("/avaliacoes/{id}/resultado")
    public Map<String, Object> resultado(@PathVariable("id") int id, @RequestParam("email") String email) {
        return avaliacoes.resultado(email, id);
    }

    // ---------- RF6: professor corrige o texto livre ----------
    @GetMapping("/correcao")
    public List<Map<String, Object>> pendentes() {
        return avaliacoes.pendentesDeCorrecao();
    }

    @PutMapping("/correcao")
    public Map<String, String> corrigir(@RequestBody Nota n) {
        avaliacoes.corrigir(n.email(), n.idAvaliacao(), n.idQuestao(), n.nota());
        return Map.of("ok", "Nota lançada.");
    }

    // ---------- RF7: relatórios ----------
    @GetMapping("/relatorios/media-por-avaliacao")
    public List<Map<String, Object>> r1() {
        return relatorios.mediaPorAvaliacao();
    }

    @GetMapping("/relatorios/aluno-vs-media")
    public List<Map<String, Object>> r2(@RequestParam("email") String email) {
        return relatorios.alunoVersusMedia(email);
    }

    @GetMapping("/relatorios/evolucao")
    public List<Map<String, Object>> r3(@RequestParam("email") String email,
                                        @RequestParam(name = "de", defaultValue = "2026-01-01") String de,
                                        @RequestParam(name = "ate", defaultValue = "2026-12-31") String ate) {
        return relatorios.evolucao(email, LocalDate.parse(de), LocalDate.parse(ate));
    }

    @GetMapping("/relatorios/questoes-mais-erradas")
    public List<Map<String, Object>> r4(@RequestParam(name = "tipos", defaultValue = "OBJETIVA,SUBJETIVA") String tipos,
                                        @RequestParam(name = "busca", defaultValue = "") String busca) {
        return relatorios.questoesMaisErradas(Arrays.asList(tipos.split(",")), busca);
    }

    @GetMapping("/relatorios/quem-nao-respondeu")
    public List<Map<String, Object>> r5() {
        return relatorios.quemNaoRespondeu();
    }
}


package br.uel.avaliacoes.controller;

import br.uel.avaliacoes.dao.AvaliacaoDAO;
import br.uel.avaliacoes.dao.QuestaoDAO;
import br.uel.avaliacoes.dao.RelatorioDAO;
import br.uel.avaliacoes.dao.UsuarioDAO;

import br.uel.avaliacoes.model.Model.Avaliacao;
import br.uel.avaliacoes.model.Model.Questao;
import br.uel.avaliacoes.model.Model.Usuario;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class Controller {

    // ==========================================
    // OBJETOS DE RECEBIMENTO
    // ==========================================

    public record Login(
            String email,
            String senha
    ) {}

    public record Envio(
            String email,
            Map<String, String> respostas
    ) {}

    public record Nota(
            String email,
            int idAvaliacao,
            int idQuestao,
            BigDecimal nota
    ) {}

    // ==========================================
    // DAOS
    // ==========================================

    private final UsuarioDAO usuarios;
    private final QuestaoDAO questoes;
    private final AvaliacaoDAO avaliacoes;
    private final RelatorioDAO relatorios;

    public Controller(
            UsuarioDAO usuarios,
            QuestaoDAO questoes,
            AvaliacaoDAO avaliacoes,
            RelatorioDAO relatorios
    ) {

        this.usuarios = usuarios;
        this.questoes = questoes;
        this.avaliacoes = avaliacoes;
        this.relatorios = relatorios;
    }

    // ==========================================
    // TRATAMENTO DE ERROS
    // ==========================================

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> erro(RuntimeException e) {

        return Map.of(
                "erro",
                e.getMessage() == null
                        ? "Erro interno."
                        : e.getMessage()
        );
    }

    // ==========================================
    // LOGIN
    // ==========================================

    @PostMapping("/login")
    public Usuario login(@RequestBody Login l) {

        if (l == null
                || l.email() == null
                || l.senha() == null
                || l.email().isBlank()
                || l.senha().isBlank()) {

            throw new IllegalArgumentException(
                    "Informe e-mail e senha."
            );
        }

        return usuarios.login(
                l.email().trim(),
                l.senha()
        ).orElseThrow(
                () -> new IllegalArgumentException(
                        "E-mail ou senha inválidos."
                )
        );
    }

    // ==========================================
    // USUARIOS
    // ==========================================

    @GetMapping("/alunos")
    public List<Map<String, Object>> alunos() {

        return usuarios.listarAlunos();
    }

    @GetMapping("/materias")
    public List<Map<String, Object>> materias(
            @RequestParam("email") String email
    ) {

        return usuarios.materiasDoUsuario(email);
    }

    @GetMapping("/materias/{sigla}/alunos")
    public List<Map<String, Object>> alunosDaMateria(
            @PathVariable("sigla") String sigla
    ) {

        return usuarios.alunosDaMateria(sigla);
    }

    // ==========================================
    // QUESTOES
    // ==========================================

    @GetMapping("/questoes")
    public List<Questao> listarQuestoes() {

        return questoes.listar();
    }

    @PostMapping("/questoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> criarQuestao(
            @RequestBody Questao q
    ) {

        if (q == null) {

            throw new IllegalArgumentException(
                    "Questão inválida."
            );
        }

        int id = questoes.inserir(q);

        return Map.of("id", id);
    }

    // ==========================================
    // AVALIACOES
    // ==========================================

    @GetMapping("/avaliacoes")
    public List<Map<String, Object>> listarAvaliacoes(
            @RequestParam(name = "professor", required = false)
            String professor
    ) {

        return avaliacoes.listar(professor);
    }

    @PostMapping("/avaliacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> criarAvaliacao(
            @RequestBody Avaliacao a
    ) {

        int id = avaliacoes.inserir(a);

        return Map.of("id", id);
    }

    // ==========================================
    // AVALIACOES DO ALUNO
    // ==========================================

    @GetMapping("/avaliacoes/do-aluno")
    public List<Map<String, Object>> doAluno(
            @RequestParam("email") String email
    ) {

        return avaliacoes.listarDoAluno(email);
    }

    // ==========================================
    // ABRIR PROVA
    // ==========================================

    @GetMapping("/avaliacoes/{id}/prova")
    public List<Map<String, Object>> prova(
            @PathVariable("id") int id
    ) {

        return avaliacoes.prova(id);
    }

    // ==========================================
    // ENVIAR RESPOSTAS
    // ==========================================

    @PostMapping("/avaliacoes/{id}/respostas")
    public Map<String, String> responder(
            @PathVariable("id") int id,
            @RequestBody Envio e
    ) {

        if (e == null
                || e.email() == null
                || e.respostas() == null) {

            throw new IllegalArgumentException(
                    "Dados das respostas inválidos."
            );
        }

        avaliacoes.salvarRespostas(
                e.email(),
                id,
                e.respostas()
        );

        return Map.of(
                "ok",
                "Respostas registradas."
        );
    }

    // ==========================================
    // RESULTADO
    // ==========================================

    @GetMapping("/avaliacoes/{id}/resultado")
    public Map<String, Object> resultado(
            @PathVariable("id") int id,
            @RequestParam("email") String email
    ) {

        return avaliacoes.resultado(
                email,
                id
        );
    }

    // ==========================================
    // CORRECAO DE QUESTOES SUBJETIVAS
    // ==========================================

    @GetMapping("/correcao")
    public List<Map<String, Object>> pendentes(
            @RequestParam(name = "professor", required = false)
            String professor
    ) {

        return avaliacoes.pendentesDeCorrecao(professor);
    }

    @PutMapping("/correcao")
    public Map<String, String> corrigir(
            @RequestBody Nota n
    ) {

        if (n == null
                || n.email() == null
                || n.nota() == null) {

            throw new IllegalArgumentException(
                    "Dados da correção inválidos."
            );
        }

        avaliacoes.corrigir(
                n.email(),
                n.idAvaliacao(),
                n.idQuestao(),
                n.nota()
        );

        return Map.of(
                "ok",
                "Nota lançada com sucesso."
        );
    }

    // ==========================================
    // RELATORIO 1
    // MEDIA POR AVALIACAO
    // ==========================================

    @GetMapping("/relatorios/media-por-avaliacao")
    public List<Map<String, Object>> r1() {

        return relatorios.mediaPorAvaliacao();
    }

    // ==========================================
    // RELATORIO 2
    // ALUNO VERSUS MEDIA DA TURMA
    // ==========================================

    @GetMapping("/relatorios/aluno-vs-media")
    public List<Map<String, Object>> r2(
            @RequestParam("email") String email
    ) {

        return relatorios.alunoVersusMedia(email);
    }

    // ==========================================
    // RELATORIO 3
    // EVOLUCAO DO ALUNO
    // ==========================================

    @GetMapping("/relatorios/evolucao")
    public List<Map<String, Object>> r3(
            @RequestParam("email") String email,

            @RequestParam(
                    name = "de",
                    defaultValue = "2026-01-01"
            )
            String de,

            @RequestParam(
                    name = "ate",
                    defaultValue = "2026-12-31"
            )
            String ate
    ) {

        LocalDate inicio = LocalDate.parse(de);
        LocalDate fim = LocalDate.parse(ate);

        if (fim.isBefore(inicio)) {

            throw new IllegalArgumentException(
                    "A data final não pode ser anterior à inicial."
            );
        }

        return relatorios.evolucao(
                email,
                inicio,
                fim
        );
    }

    // ==========================================
    // RELATORIO 4
    // QUESTOES MAIS ERRADAS
    // ==========================================

    @GetMapping("/relatorios/questoes-mais-erradas")
    public List<Map<String, Object>> r4(

            @RequestParam(
                    name = "tipos",
                    defaultValue = "OBJETIVA,SUBJETIVA"
            )
            String tipos,

            @RequestParam(
                    name = "busca",
                    defaultValue = ""
            )
            String busca
    ) {

        List<String> listaTipos = List.of(
                tipos.split(",")
        );

        return relatorios.questoesMaisErradas(
                listaTipos,
                busca
        );
    }

    // ==========================================
    // RELATORIO 5
    // ALUNOS QUE NAO RESPONDERAM
    // ==========================================

    @GetMapping("/relatorios/quem-nao-respondeu")
    public List<Map<String, Object>> r5() {

        return relatorios.quemNaoRespondeu();
    }
}

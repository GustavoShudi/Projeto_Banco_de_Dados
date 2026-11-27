BEGIN;
INSERT INTO usuario (email, nome, senha, tipo) VALUES

                                                   ('anderson@uel.br', 'Prof. Anderson', 'prof123', 'PROFESSOR'),
                                                   ('andrielber@uel.br', 'Prof. Andrielber', 'prof123', 'PROFESSOR'),

                                                   ('pedro@aluno.uel.br', 'Pedro', '123456', 'ALUNO'),
                                                   ('gustavo@aluno.uel.br', 'Gustavo', '123456', 'ALUNO'),
                                                   ('alef@aluno.uel.br', 'Alef', '123456', 'ALUNO'),
                                                   ('emilly@aluno.uel.br', 'Emilly', '123456', 'ALUNO'),
                                                   ('renan@aluno.uel.br', 'Renan', '123456', 'ALUNO'),
                                                   ('kelissa@aluno.uel.br', 'Kelissa', '123456', 'ALUNO'),
                                                   ('kaua@aluno.uel.br', 'Kaua', '123456', 'ALUNO'),
                                                   ('leonardo@aluno.uel.br', 'Leonardo', '123456', 'ALUNO'),
                                                   ('ruan@aluno.uel.br', 'Ruan', '123456', 'ALUNO'),
                                                   ('gabriel@aluno.uel.br', 'Gabriel', '123456', 'ALUNO'),
                                                   ('heloisa@aluno.uel.br', 'Heloisa', '123456', 'ALUNO'),
                                                   ('marcos@aluno.uel.br', 'Marcos', '123456', 'ALUNO'),
                                                   ('joao@aluno.uel.br', 'Joao', '123456', 'ALUNO'),
                                                   ('mateus@aluno.uel.br', 'Mateus', '123456', 'ALUNO'),
                                                   ('felipe@aluno.uel.br', 'Felipe', '123456', 'ALUNO'),
                                                   ('breno@aluno.uel.br', 'Breno', '123456', 'ALUNO'),
                                                   ('beatriz@aluno.uel.br', 'Beatriz', '123456', 'ALUNO'),
                                                   ('davi@aluno.uel.br', 'Davi', '123456', 'ALUNO');

INSERT INTO professor (email_usuario) VALUES
                                          ('anderson@uel.br'),
                                          ('andrielber@uel.br');

INSERT INTO aluno (email_usuario)
SELECT email
FROM usuario
WHERE tipo = 'ALUNO';


-- ============================================================
-- 3. MATERIAS
-- A sigla tem no maximo 4 caracteres (VARCHAR(4) no schema).
-- ============================================================

INSERT INTO materia (sigla, nome) VALUES

                                      ('M101', 'Aritmetica Basica'),
                                      ('M102', 'Fracoes e Porcentagem'),
                                      ('M103', 'Geometria e Expressoes');


-- ============================================================
-- 4. PROFESSORES E MATERIAS
-- A tabela possui aceita somente professores.
-- ============================================================

INSERT INTO possui (email_usuario, sigla_materia) VALUES

                                                      ('anderson@uel.br', 'M101'),
                                                      ('anderson@uel.br', 'M102'),

                                                      ('andrielber@uel.br', 'M102'),
                                                      ('andrielber@uel.br', 'M103');


-- ============================================================
-- 5. QUESTOES
-- IDs explicitos exigem OVERRIDING SYSTEM VALUE.
-- ============================================================

INSERT INTO questao
(id_questao, enunciado, tipo)
    OVERRIDING SYSTEM VALUE
VALUES

    (1, 'Quanto é 345 + 278?', 'OBJETIVA'),
    (2, 'Quanto é 1000 - 357?', 'OBJETIVA'),
    (3, 'Quanto é 48 + 97 - 35?', 'OBJETIVA'),
    (4, 'Quanto é 5 - 8?', 'OBJETIVA'),
    (5, 'Quanto é 7 x 8?', 'OBJETIVA'),
    (6, 'Quanto é 12 x 15?', 'OBJETIVA'),
    (7, 'Quanto é 144 / 12?', 'OBJETIVA'),
    (8, 'Quanto é 156 / 4?', 'OBJETIVA'),
    (9, 'Quanto é 1/2 + 1/4?', 'OBJETIVA'),
    (10, 'Quanto é 2/3 de 12?', 'OBJETIVA'),
    (11, 'Quanto é 0,5 + 0,25?', 'OBJETIVA'),
    (12, 'A fração 3/5 em decimal é:', 'OBJETIVA'),
    (13, 'Quanto é 1,2 x 3?', 'OBJETIVA'),
    (14, 'Quanto é 10% de 250?', 'OBJETIVA'),
    (15, 'Quanto é 50% de 80?', 'OBJETIVA'),
    (16, 'Quanto é 25% de 200?', 'OBJETIVA'),
    (17, 'Se 3 canetas custam R$ 12, quanto custam 5?', 'OBJETIVA'),
    (18, 'Um produto de R$ 150 tem 20% de desconto. Quanto se paga?', 'OBJETIVA'),
    (19, 'Qual a area de um retangulo de lados 6 e 4?', 'OBJETIVA'),
    (20, 'Qual o perimetro de um quadrado de lado 5?', 'OBJETIVA'),
    (21, 'Quanto é 2 + 3 x 4?', 'OBJETIVA'),
    (22, 'Quanto é (2 + 3) x 4?', 'OBJETIVA'),
    (23, 'Quanto é 2 elevado a 3?', 'OBJETIVA'),
    (24, 'Qual a raiz quadrada de 81?', 'OBJETIVA'),

    (25, 'Explique como resolver 3/4 + 1/8, mostrando os passos.', 'SUBJETIVA'),
    (26, 'Por que nao é possivel dividir um numero por zero?', 'SUBJETIVA'),
    (27, 'Explique a diferenca entre area e perimetro, com exemplo.', 'SUBJETIVA'),
    (28, 'Como calcular 15% de 200? Mostre o raciocinio.', 'SUBJETIVA'),
    (29, 'Explique a ordem de resolucao das operacoes.', 'SUBJETIVA'),
    (30, 'Resolva 2,5 x 4 e explique como chegou ao resultado.', 'SUBJETIVA'),
    (31, 'O que é um numero primo? De tres exemplos.', 'SUBJETIVA'),
    (32, 'Explique a regra de tres simples com um exemplo.', 'SUBJETIVA');


-- ============================================================
-- 6. TIPOS DAS QUESTOES
-- ============================================================

INSERT INTO questao_objetiva (id_questao)
SELECT id_questao
FROM questao
WHERE tipo = 'OBJETIVA';

INSERT INTO questao_subjetiva (id_questao, expectativa) VALUES

                                                            (25, 'Igualar denominadores: 6/8 + 1/8 = 7/8.'),
                                                            (26, 'A divisao por zero e indefinida.'),
                                                            (27, 'Perimetro e a soma dos lados; area mede a superficie.'),
                                                            (28, '15/100 x 200 = 30.'),
                                                            (29, 'Parenteses, potencias, multiplicacao e divisao, soma e subtracao.'),
                                                            (30, '2,5 x 4 = 10.'),
                                                            (31, 'Numero natural maior que 1 com somente dois divisores.'),
                                                            (32, 'Relaciona duas grandezas proporcionais.');


-- ============================================================
-- 7. ALTERNATIVAS
-- A primeira alternativa de cada questao e a correta.
-- IDs sao gerados automaticamente.
-- ============================================================

INSERT INTO alternativa
(id_questao_objetiva, texto, correta)

SELECT
    q.id,
    op.texto,
    op.posicao = 1

FROM (
         VALUES

             (1, '623', '613', '633', '723'),
             (2, '643', '653', '633', '743'),
             (3, '110', '100', '120', '130'),
             (4, '-3', '3', '-13', '13'),
             (5, '56', '54', '58', '64'),
             (6, '180', '170', '190', '160'),
             (7, '12', '11', '13', '14'),
             (8, '39', '38', '42', '44'),
             (9, '3/4', '2/6', '1/6', '2/4'),
             (10, '8', '6', '9', '10'),
             (11, '0,75', '0,30', '0,55', '0,80'),
             (12, '0,6', '0,35', '0,53', '0,8'),
             (13, '3,6', '4,2', '3,2', '36'),
             (14, '25', '2,5', '20', '15'),
             (15, '40', '50', '30', '60'),
             (16, '50', '25', '40', '75'),
             (17, 'R$ 20', 'R$ 15', 'R$ 18', 'R$ 24'),
             (18, 'R$ 120', 'R$ 130', 'R$ 135', 'R$ 110'),
             (19, '24', '10', '20', '48'),
             (20, '20', '10', '25', '15'),
             (21, '14', '20', '24', '12'),
             (22, '20', '14', '24', '12'),
             (23, '8', '6', '9', '5'),
             (24, '9', '8', '7', '10')

     ) AS q(id, correta, errada1, errada2, errada3)

         CROSS JOIN LATERAL (

    VALUES
        (1, q.correta),
        (2, q.errada1),
        (3, q.errada2),
        (4, q.errada3)

        ) AS op(posicao, texto);


-- ============================================================
-- 8. AVALIACOES
-- ============================================================

INSERT INTO avaliacao (
    id_avaliacao,
    titulo,
    descricao,
    data_avaliacao,
    horario_inicio,
    horario_fim,
    email_professor,
    sigla_materia
)
    OVERRIDING SYSTEM VALUE

VALUES

    (1, 'Quiz 1 - Adicao e Subtracao',
     'Quiz de adicao, subtracao e expressoes simples',
     '2026-08-28', '19:15', '20:00',
     'anderson@uel.br', 'M101'),

    (2, 'Prova 1 - Multiplicacao e Divisao',
     'Prova de multiplicacao, divisao e problemas',
     '2026-09-25', '19:15', '21:00',
     'anderson@uel.br', 'M101'),

    (3, 'Simulado - Fracoes e Decimais',
     'Simulado de fracoes e numeros decimais',
     '2026-10-09', '19:15', '20:30',
     'andrielber@uel.br', 'M102'),

    (4, 'Quiz 2 - Porcentagem e Proporcao',
     'Quiz de porcentagem e regra de tres',
     '2026-10-23', '19:15', '20:00',
     'anderson@uel.br', 'M102'),

    (5, 'Prova 2 - Geometria e Expressoes',
     'Prova de geometria basica, potencias e expressoes',
     '2026-11-13', '19:15', '21:00',
     'andrielber@uel.br', 'M103'),

    (6, 'Prova 3 - Revisao Geral',
     'Avaliacao de demonstracao ainda nao respondida',
     '2026-11-27', '19:15', '21:00',
     'andrielber@uel.br', 'M103');


-- ============================================================
-- 9. QUESTOES E VALORES DE CADA AVALIACAO
-- ============================================================

INSERT INTO avaliacao_questao (
    id_avaliacao,
    id_questao,
    valor_questao
)
VALUES

-- Avaliacao 1
(1, 1, 2),
(1, 2, 2),
(1, 3, 2),
(1, 4, 2),
(1, 21, 2),

-- Avaliacao 2
(2, 5, 1.5),
(2, 6, 1.5),
(2, 7, 1.5),
(2, 8, 1.5),
(2, 30, 4),

-- Avaliacao 3
(3, 9, 1.5),
(3, 10, 1.5),
(3, 11, 1.5),
(3, 12, 1.5),
(3, 13, 1.5),
(3, 25, 2.5),

-- Avaliacao 4
(4, 14, 1.5),
(4, 15, 1.5),
(4, 16, 1.5),
(4, 17, 1.5),
(4, 18, 1.5),
(4, 28, 2.5),

-- Avaliacao 5
(5, 19, 1),
(5, 20, 1),
(5, 22, 1),
(5, 23, 1),
(5, 24, 1),
(5, 27, 3),
(5, 29, 2),

-- Avaliacao 6
(6, 5, 2),
(6, 10, 2),
(6, 15, 2),
(6, 21, 2),
(6, 32, 2);


-- ============================================================
-- 10. ALUNOS DESIGNADOS
--
-- O schema nao possui associacao aluno-materia.
-- Portanto, todos os alunos sao designados para as
-- seis avaliacoes de demonstracao.
-- ============================================================

INSERT INTO realiza (
    id_avaliacao,
    email_aluno
)

SELECT
    a.id_avaliacao,
    al.email_usuario

FROM avaliacao a

         CROSS JOIN aluno al;


-- ============================================================
-- 11. RESPOSTAS AUTOMATICAS DE DEMONSTRACAO
--
-- Avaliacoes 1 a 5:
-- Objetivas: alternativa selecionada e nota automatica.
-- Subjetivas: resposta textual e algumas notas pendentes.
--
-- Avaliacao 6:
-- Sem respostas, para demonstrar alunos que ainda
-- nao realizaram a avaliacao.
-- ============================================================

INSERT INTO responde (
    id_avaliacao,
    email_usuario,
    id_questao,
    resposta_subjetiva,
    id_alternativa_escolhida,
    nota
)

SELECT

    re.id_avaliacao,

    re.email_aluno,

    q.id_questao,

    CASE
        WHEN q.tipo = 'SUBJETIVA'
            THEN
            'Resposta demonstrativa do aluno. '
                || 'O professor devera avaliar o raciocinio apresentado.'
        ELSE NULL
        END,

    CASE
        WHEN q.tipo = 'OBJETIVA'
            THEN alt.id_alternativa
        ELSE NULL
        END,

    CASE

        WHEN q.tipo = 'OBJETIVA' THEN

            CASE
                WHEN alt.correta THEN aq.valor_questao
                ELSE 0
                END

        WHEN MOD(
                     ABS(
                             HASHCODE.hash + q.id_questao + re.id_avaliacao
                     ),
                     3
             ) = 0 THEN aq.valor_questao

        ELSE NULL

        END

FROM realiza re

         JOIN avaliacao_questao aq
              ON aq.id_avaliacao = re.id_avaliacao

         JOIN questao q
              ON q.id_questao = aq.id_questao

         CROSS JOIN LATERAL (
    SELECT ABS(
                   HASHTEXT(re.email_aluno)::BIGINT
           ) AS hash
        ) HASHCODE

         LEFT JOIN LATERAL (

    SELECT
        a.id_alternativa,
        a.correta

    FROM alternativa a

    WHERE a.id_questao_objetiva = q.id_questao

    ORDER BY

        CASE
            WHEN MOD(
                         ABS(HASHCODE.hash + q.id_questao + re.id_avaliacao),
                         3
                 ) = 0
                THEN a.correta
            ELSE NOT a.correta
            END DESC,

        a.id_alternativa

        LIMIT 1

) alt ON q.tipo = 'OBJETIVA'

WHERE re.id_avaliacao BETWEEN 1 AND 5;


-- ============================================================
-- 12. CALCULAR NOTAS TOTAIS DOS ALUNOS
--
-- Se ainda existir questao discursiva sem correcao,
-- a nota total permanece 0 ate a correcao completa.
-- ============================================================

UPDATE realiza re

SET nota_total = (

    SELECT COALESCE(SUM(r.nota), 0)

    FROM responde r

    WHERE r.id_avaliacao = re.id_avaliacao
      AND r.email_usuario = re.email_aluno

)

WHERE re.id_avaliacao BETWEEN 1 AND 5

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

);


-- ============================================================
-- 13. SINCRONIZAR SEQUENCIAS IDENTITY
-- ============================================================

SELECT setval(
               pg_get_serial_sequence('questao', 'id_questao'),
               (SELECT MAX(id_questao) FROM questao),
               TRUE
       );

SELECT setval(
               pg_get_serial_sequence('avaliacao', 'id_avaliacao'),
               (SELECT MAX(id_avaliacao) FROM avaliacao),
               TRUE
       );

SELECT setval(
               pg_get_serial_sequence('alternativa', 'id_alternativa'),
               (SELECT MAX(id_alternativa) FROM alternativa),
               TRUE
       );


COMMIT;
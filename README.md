## Integrantes
- Gustavo Shudi Takeda Obuti
- Pedro Otavio Beserra Gabriel
  
**Disciplina:** Bancos de Dados I (2COP014)\
**Universidade Estadual de Londrina (UEL)**

## Natureza do sistema
O sistema tem como natureza a avaliação de aprendizagem, sendo destinado à criação e realização de provas, simulados e quizzes. Ele permite cadastrar questões objetivas e subjetivas, montar avaliações com questões já cadastradas, definir a qual matéria pertence e registrar suas respostas.

## Particularidade do sistema
A particularidade consiste na atribuição de um valor para cada questão dentro de uma avaliação. O sistema registra a nota obtida em cada questão, sendo a correção das questões objetivas realizada automaticamente e a das questões subjetivas feita pelo professor. Ao final, as notas das questões são somadas para calcular a nota total de cada aluno.

## Instruções para rodar o sistema

1. Criar um banco de dados no PostgreSQL com o nome `Projeto de BD`:
   ```sql
   CREATE DATABASE "Projeto de BD";
   ```
2. Executar, nesse banco de dados, os scripts database/schema.sql e database/carga.sql, nessa ordem.
3. Abrir projeto no IntelliJ ou no VS Code
4. Configurar a senha do PostgreSQL no arquivo application.properties, alterando a propriedade:
   ```properties
   spring.datasource.password=${DB_PASSWORD:Senha}
   ```
5. Abrir App.java e executar o programa
6. Em um navegador, acessar http://localhost:8080/
7. Para testar a área do aluno, usar:
   - **Email:** pedro@aluno.uel.br
   - **Senha:** 123456
8. Para testar a área do professor, usar:
   - **Email:** anderson@uel.br
   - **Senha:** prof123

## Relatórios

### Relatório 1
#### Objetivo
Apresentar a média das notas de cada avaliação, considerando somente os alunos que responderam a todas as questões e tiveram todas as questões discursivas corrigidas. O relatório também exibe a quantidade de alunos que concluíram a avaliação e a quantidade total de alunos inscritos.
#### Imagem da tela
<img width="1097" height="662" alt="image" src="https://github.com/user-attachments/assets/c52bde3a-0ddd-4c44-9bdf-e06ac42aacf7" /> 

#### Recursos utilizados
- JOIN
- LEFT JOIN
- SUM
- AVG
- COUNT
- GROUP BY
- HAVING
- ORDER BY
#### SQL
```sql
   WITH notas AS (

    SELECT
        re.id_avaliacao,
        re.email_aluno,

        SUM(r.nota) AS nota_total,

        COUNT(aq.id_questao) AS total_questoes,

        COUNT(r.id_questao) AS respondidas,

        COUNT(*) FILTER (
            WHERE qs.id_questao IS NOT NULL
              AND r.nota IS NULL
        ) AS pendentes

    FROM realiza re

             JOIN avaliacao_questao aq
                  ON aq.id_avaliacao = re.id_avaliacao

             LEFT JOIN responde r
                       ON r.id_avaliacao = aq.id_avaliacao
                           AND r.id_questao = aq.id_questao
                           AND r.email_usuario = re.email_aluno

             LEFT JOIN questao_subjetiva qs
                       ON qs.id_questao = aq.id_questao

    GROUP BY
        re.id_avaliacao,
        re.email_aluno

),

     concluidas AS (

         SELECT *
         FROM notas

         WHERE total_questoes = respondidas
           AND pendentes = 0
     )

SELECT
    a.id_avaliacao,
    a.titulo,
    a.data_avaliacao,

    ROUND(AVG(c.nota_total), 2) AS media,

    COUNT(c.email_aluno) AS alunos_concluintes,

    (
        SELECT COUNT(*)
        FROM realiza re
        WHERE re.id_avaliacao = a.id_avaliacao
    ) AS alunos_inscritos

FROM avaliacao a

         LEFT JOIN concluidas c
                   ON c.id_avaliacao = a.id_avaliacao

GROUP BY
    a.id_avaliacao,
    a.titulo,
    a.data_avaliacao

ORDER BY
    a.data_avaliacao,
    a.id_avaliacao;

```

### Relatório 2
#### Objetivo
Comparar o desempenho de um aluno específico com a média das notas dos demais alunos em cada avaliação concluída. O relatório recebe o e-mail do aluno como parâmetro e apresenta sua nota total e a média dos outros participantes, considerando somente aqueles que responderam a todas as questões e tiveram todas as questões discursivas corrigidas.
#### Imagem da tela
<img width="1070" height="599" alt="image" src="https://github.com/user-attachments/assets/18126401-55bf-4103-ad11-42858877a03a" />

#### Recursos utilizados
- JOIN
- Subconsulta correlacionada
- AVG
- GROUP BY
- ORDER BY
#### SQL
```sql
WITH notas AS (

    SELECT
        re.id_avaliacao,
        re.email_aluno,

        SUM(r.nota) AS nota_total,

        COUNT(aq.id_questao) AS total_questoes,

        COUNT(r.id_questao) AS respondidas,

        COUNT(*) FILTER (
            WHERE qs.id_questao IS NOT NULL
              AND r.nota IS NULL
        ) AS pendentes

    FROM realiza re

             JOIN avaliacao_questao aq
                  ON aq.id_avaliacao = re.id_avaliacao

             LEFT JOIN responde r
                       ON r.id_avaliacao = aq.id_avaliacao
                           AND r.id_questao = aq.id_questao
                           AND r.email_usuario = re.email_aluno

             LEFT JOIN questao_subjetiva qs
                       ON qs.id_questao = aq.id_questao

    GROUP BY
        re.id_avaliacao,
        re.email_aluno

),

     concluidas AS (

         SELECT *
         FROM notas

         WHERE total_questoes = respondidas
           AND pendentes = 0
     )

SELECT
    a.id_avaliacao,
    a.titulo,

    aluno.nota_total AS nota_aluno,

    (
        SELECT ROUND(AVG(outros.nota_total), 2)

        FROM concluidas outros

        WHERE outros.id_avaliacao = a.id_avaliacao
          AND outros.email_aluno <> aluno.email_aluno

    ) AS media_demais_alunos

FROM concluidas aluno

         JOIN avaliacao a
              ON a.id_avaliacao = aluno.id_avaliacao

WHERE aluno.email_aluno = :email

ORDER BY a.data_avaliacao;
```

### Relatório 3
#### Objetivo
Apresentar a evolução do desempenho de um aluno ao longo das avaliações realizadas em um determinado período. O relatório recebe o e-mail do aluno e as datas inicial e final como parâmetros, exibindo as notas obtidas em cada avaliação concluída dentro do intervalo informado. São consideradas somente as avaliações em que o aluno respondeu a todas as questões e teve todas as questões discursivas corrigidas.
#### Imagem da tela
<img width="1066" height="632" alt="image" src="https://github.com/user-attachments/assets/8915c575-6f00-48a0-a9f8-0e0a1de3da0f" />

#### Recursos utilizados
- JOIN
- SUM
- COUNT
- FILTER
- WHERE
- BETWEEN
- ORDER BY
#### SQL
```sql
WITH notas AS (

    SELECT
        re.id_avaliacao,
        re.email_aluno,

        SUM(r.nota) AS nota_total,

        COUNT(aq.id_questao) AS total_questoes,

        COUNT(r.id_questao) AS respondidas,

        COUNT(*) FILTER (
            WHERE qs.id_questao IS NOT NULL
              AND r.nota IS NULL
        ) AS pendentes

    FROM realiza re

             JOIN avaliacao_questao aq
                  ON aq.id_avaliacao = re.id_avaliacao

             LEFT JOIN responde r
                       ON r.id_avaliacao = aq.id_avaliacao
                           AND r.id_questao = aq.id_questao
                           AND r.email_usuario = re.email_aluno

             LEFT JOIN questao_subjetiva qs
                       ON qs.id_questao = aq.id_questao

    GROUP BY
        re.id_avaliacao,
        re.email_aluno

)

SELECT
    a.id_avaliacao,
    a.titulo,
    a.data_avaliacao,

    ROUND(n.nota_total, 2) AS nota

FROM notas n

         JOIN avaliacao a
              ON a.id_avaliacao = n.id_avaliacao

WHERE n.email_aluno = :email

  AND n.total_questoes = n.respondidas
  AND n.pendentes = 0

  AND a.data_avaliacao BETWEEN :de AND :ate

ORDER BY
    a.data_avaliacao,
    a.id_avaliacao;
```

### Relatório 4
#### Objetivo
Identificar as questões que apresentam o maior percentual de respostas incorretas, permitindo analisar quais conteúdos oferecem maior dificuldade aos alunos. É possível filtrar por questões objetivas e discursivas, permitindo também buscar por questão. O relatório apresenta a quantidade total de respostas, a quantidade de respostas erradas e o percentual de erros de cada questão, exibindo as 10 questões com maiores percentuais.
#### Imagem da tela
<img width="828" height="665" alt="image" src="https://github.com/user-attachments/assets/7c00411c-1ccc-444f-9923-603e40dd6347" />

#### Recursos utilizados
- JOIN
- LEFT JOIN
- COUNT
- FILTER
- GROUP BY
- ORDER BY
- FETCH FIRST
- WHERE
- LIKE
#### SQL
```sql
SELECT
    q.id_questao,
    q.enunciado,
    q.tipo,

    COUNT(*) AS total_respostas,

    COUNT(*) FILTER (
        WHERE
            (
                q.tipo = 'OBJETIVA'
                AND alt.correta = FALSE
            )
            OR
            (
                q.tipo = 'SUBJETIVA'
                AND r.nota < aq.valor_questao
            )
    ) AS respostas_erradas,

    ROUND(
            100.0 *

            COUNT(*) FILTER (
            WHERE
                (
                    q.tipo = 'OBJETIVA'
                    AND alt.correta = FALSE
                )
                OR
                (
                    q.tipo = 'SUBJETIVA'
                    AND r.nota < aq.valor_questao
                )
        )

        / NULLIF(COUNT(*), 0),

            2
    ) AS percentual_erros

FROM responde r

         JOIN questao q
              ON q.id_questao = r.id_questao

         JOIN avaliacao_questao aq
              ON aq.id_avaliacao = r.id_avaliacao
                  AND aq.id_questao = r.id_questao

         LEFT JOIN alternativa alt
                   ON alt.id_questao_objetiva = r.id_questao
                       AND alt.id_alternativa = r.id_alternativa_escolhida

WHERE q.tipo IN ('OBJETIVA', 'SUBJETIVA')

  AND q.enunciado ILIKE '%%'

  AND (
      (
          q.tipo = 'OBJETIVA'
          AND alt.correta IS NOT NULL
      )
      OR
      (
          q.tipo = 'SUBJETIVA'
          AND r.nota IS NOT NULL
      )
  )

GROUP BY
    q.id_questao,
    q.enunciado,
    q.tipo

ORDER BY
    percentual_erros DESC,
    total_respostas DESC

    FETCH FIRST 10 ROWS ONLY;
```
### Relatório 5
#### Objetivo
Identificar os alunos inscritos em avaliações que ainda não possuem nenhuma resposta registrada. O relatório apresenta o título e a data da avaliação, além do e-mail e do nome do aluno. Dessa forma, permite acompanhar a participação dos alunos e identificar aqueles que ainda não iniciaram as avaliações.
#### Imagem da tela
<img width="943" height="720" alt="image" src="https://github.com/user-attachments/assets/f2ccfba1-429b-44ec-a7ee-243ec64e8e10" />


#### Recursos utilizados
- LEFT JOIN
- WHERE
- ORDER BY
#### SQL
```sql
SELECT
    a.id_avaliacao,
    a.titulo,
    a.data_avaliacao,

    u.email,
    u.nome

FROM realiza re

         JOIN avaliacao a
              ON a.id_avaliacao = re.id_avaliacao

         JOIN usuario u
              ON u.email = re.email_aluno

         LEFT JOIN responde r
                   ON r.id_avaliacao = re.id_avaliacao
                       AND r.email_usuario = re.email_aluno

WHERE r.id_questao IS NULL

ORDER BY
    a.data_avaliacao,
    a.titulo,
    u.nome;
```

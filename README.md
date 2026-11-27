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

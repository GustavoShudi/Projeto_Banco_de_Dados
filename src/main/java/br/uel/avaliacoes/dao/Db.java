
package br.uel.avaliacoes.dao;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Db {

    private Db() {
    }

    @FunctionalInterface
    public interface Trabalho<T> {
        T executar(Connection con) throws SQLException;
    }

    // ==========================================
    // TRANSACOES
    // ==========================================

    public static <T> T transacao(
            DataSource ds,
            Trabalho<T> trabalho
    ) {

        try (Connection con = ds.getConnection()) {

            con.setAutoCommit(false);

            try {

                T resultado = trabalho.executar(con);

                con.commit();

                return resultado;

            } catch (SQLException | RuntimeException e) {

                con.rollback();

                throw e;
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    mensagem(e),
                    e
            );
        }
    }

    // ==========================================
    // CONSULTAS
    // ==========================================

    public static List<Map<String, Object>> consulta(
            DataSource ds,
            String sql,
            Object... params
    ) {

        try (
                Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            preencher(ps, params);

            try (ResultSet rs = ps.executeQuery()) {

                ResultSetMetaData md = rs.getMetaData();

                List<Map<String, Object>> linhas =
                        new ArrayList<>();

                while (rs.next()) {

                    Map<String, Object> linha =
                            new LinkedHashMap<>();

                    for (int i = 1;
                         i <= md.getColumnCount();
                         i++) {

                        Object valor = rs.getObject(i);

                        if (valor instanceof java.sql.Date
                                || valor instanceof java.sql.Time) {

                            valor = valor.toString();
                        }

                        linha.put(
                                md.getColumnLabel(i),
                                valor
                        );
                    }

                    linhas.add(linha);
                }

                return linhas;
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    mensagem(e),
                    e
            );
        }
    }

    // ==========================================
    // INSERT / UPDATE / DELETE
    // ==========================================

    public static int atualiza(
            DataSource ds,
            String sql,
            Object... params
    ) {

        try (
                Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            preencher(ps, params);

            return ps.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException(
                    mensagem(e),
                    e
            );
        }
    }

    // ==========================================
    // PARAMETROS
    // ==========================================

    public static void preencher(
            PreparedStatement ps,
            Object... params
    ) throws SQLException {

        for (int i = 0; i < params.length; i++) {

            ps.setObject(i + 1, params[i]);
        }
    }

    // ==========================================
    // CONVERSAO
    // ==========================================

    public static int inteiro(Object valor) {

        return ((Number) valor).intValue();
    }

    // ==========================================
    // ERROS
    // ==========================================

    private static String mensagem(SQLException e) {

        if ("23505".equals(e.getSQLState())) {

            return "Registro duplicado.";
        }

        if ("23503".equals(e.getSQLState())) {

            return "Não foi possível realizar a operação "
                    + "porque existe uma referência inválida.";
        }

        if ("23514".equals(e.getSQLState())) {

            return "Um dos valores informados não respeita "
                    + "as regras do banco de dados.";
        }

        String mensagem = e.getMessage();

        if (mensagem == null) {

            return "Erro no banco de dados.";
        }

        return mensagem
                .split("\n")[0]
                .replace("ERROR: ", "");
    }
}

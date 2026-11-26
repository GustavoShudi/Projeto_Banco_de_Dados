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

//Consulta, atualização e transação.
public final class Db {

    private Db() {}

    @FunctionalInterface
    public interface Trabalho<T> {
        T executar(Connection con) throws SQLException;
    }

    public static <T> T transacao(DataSource ds, Trabalho<T> trabalho) {
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
            throw new RuntimeException(mensagem(e), e);
        }
    }

    public static List<Map<String, Object>> consulta(DataSource ds, String sql, Object... params) {
        try (Connection con = ds.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                List<Map<String, Object>> linhas = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> linha = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        Object v = rs.getObject(i);
                        if (v instanceof java.sql.Date || v instanceof java.sql.Time) {
                            v = v.toString();
                        }
                        linha.put(md.getColumnLabel(i), v);
                    }
                    linhas.add(linha);
                }
                return linhas;
            }
        } catch (SQLException e) {
            throw new RuntimeException(mensagem(e), e);
        }
    }

    /** INSERT/UPDATE/DELETE com parâmetros; devolve as linhas afetadas. */
    public static int atualiza(DataSource ds, String sql, Object... params) {
        try (Connection con = ds.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(mensagem(e), e);
        }
    }

    public static int inteiro(Object o) {
        return ((Number) o).intValue();
    }

    private static String mensagem(SQLException e) {
        if ("23505".equals(e.getSQLState())) return "Registro duplicado (essa informação já foi cadastrada).";
        String m = e.getMessage() == null ? "Erro no banco de dados" : e.getMessage();
        return m.split("\n")[0].replace("ERROR: ", "");
    }
}
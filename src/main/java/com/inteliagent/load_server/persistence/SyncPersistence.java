package com.inteliagent.load_server.persistence;

import com.inteliagent.load_server.entity.SyncTables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

@Repository
public class SyncPersistence {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public void genericSave(String sql) throws Exception {
        jdbcTemplate.update(sql, new HashMap<>());
    }

    public List<String> genericSelect(String sql, String dataUltimoSync, String dataAtual) throws Exception {
        MapSqlParameterSource map = new MapSqlParameterSource();
        map.addValue("DATA_ULTIMO_SINCRONISMO", dataUltimoSync);
        map.addValue("DATA_ATUAL", dataAtual);

        return jdbcTemplate.query(sql, map, (rs, rowNum) -> rs.getString(1));
    }


    public String getHoraUltimoSincronismo(String table, int idUsuario) throws Exception {
        String sql = "SELECT ULTIMO_SINCRONISMO FROM sync_confirm WHERE ID_USUARIO = :IDUSUARIO AND SYNC_TABLE = :TABLE";
        MapSqlParameterSource map = new MapSqlParameterSource();
        map.addValue("TABLE", table);
        map.addValue("IDUSUARIO", idUsuario);

        return jdbcTemplate.query(sql, map, (rs) ->{
            if(rs.next()){
                return rs.getString("ULTIMO_SINCRONISMO");
            }
            return null;
        });
    }

    public String getHoraBanco(){
        String sql = "select now(6)";
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), String.class);
    }

    public List<SyncTables> getSyncTables() throws Exception {
        String sql = "SELECT ID, TABELA, ROTINA FROM SYNC_TABLES";
        return jdbcTemplate.query(sql, new SyncTablesRowMapper());
    }

    public void confirmTables(String table, int idUsuario, String dataHoraUltSync) throws Exception {
        String sql = "REPLACE INTO sync_confirm (ULTIMO_SINCRONISMO, SYNC_TABLE, ID_USUARIO) VALUES (:DATAHORA, :TABLE, :IDUSUARIO)";
        MapSqlParameterSource map = new MapSqlParameterSource();
        map.addValue("DATAHORA", dataHoraUltSync);
        map.addValue("TABLE", table);
        map.addValue("IDUSUARIO", idUsuario);

        jdbcTemplate.update(sql, map);
    }

    private static final class SyncTablesRowMapper implements RowMapper<SyncTables> {
        @Override
        public SyncTables mapRow(ResultSet rs, int rowNum) throws SQLException {
            SyncTables syncTables = new SyncTables();
            syncTables.setId(rs.getInt("ID"));
            syncTables.setTableName(rs.getString("TABELA"));
            syncTables.setRotina(rs.getString("ROTINA"));
            return syncTables;
        }
    }
}

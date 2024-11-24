package com.inteliagent.load_server.service;

import com.inteliagent.load_server.entity.*;
import com.inteliagent.load_server.persistence.SyncPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SyncService {

    @Autowired
    private SyncPersistence syncPersistence;

    public ConfirmLoads sync(List<Sync> syncList){

        List<String> confirmIds = new ArrayList<>();

        for(Sync sync: syncList) {
            try{
                syncPersistence.genericSave(sync.getSql());
                confirmIds.add(sync.getId());
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        ConfirmLoads confirmLoads = new ConfirmLoads();
        confirmLoads.setConfirmedIds(confirmIds);
        return confirmLoads;
    }

    public Consumer consumer(int idUsuario, boolean loadFull) throws Exception {
        String horaInicioProcesso = syncPersistence.getHoraBanco();
        List<SyncTables> syncTablesList = syncPersistence.getSyncTables();
        List<ConsumerLoads> consumerLoadsList = getConsumerLoads(syncTablesList, horaInicioProcesso, idUsuario, loadFull);

        Consumer consumer = new Consumer();
        consumer.setDataHoraInicioProcesso(horaInicioProcesso);
        consumer.setConsumerLoadsList(consumerLoadsList);

        return consumer;
    }

    public void consumerConfirm(List<String> tables, String dataHora, int idUsuario ) throws Exception {
        tables.forEach(table->{
            try {
                syncPersistence.confirmTables(table, idUsuario, dataHora);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<ConsumerLoads> getConsumerLoads(List<SyncTables> syncTablesList, String horaInicioProcesso, int idUsuario, boolean loadFull){
        return syncTablesList.stream().map(syncTables -> {
            try {
                String horaUltimoSincronismo = "0000-00-00 00:00";
                if(!loadFull){
                    horaUltimoSincronismo = syncPersistence.getHoraUltimoSincronismo(syncTables.getTableName(), idUsuario);
                    horaUltimoSincronismo = horaUltimoSincronismo == null ? "0000-00-00 00:00" : horaUltimoSincronismo;
                }

                String rotina = formatarRotina(syncTables.getRotina(), horaUltimoSincronismo, horaInicioProcesso);
                List<String> rotinaDados = syncPersistence.genericSelect(rotina, horaUltimoSincronismo, horaInicioProcesso);
                if(rotinaDados.size() > 1){
                    ConsumerLoads consumerLoads = new ConsumerLoads();

                    consumerLoads.setTableName(syncTables.getTableName());
                    consumerLoads.setCabecalho(rotinaDados.getFirst());
                    rotinaDados.removeFirst();

                    StringBuilder registros = new StringBuilder();
                    rotinaDados.forEach(data -> registros.append(data).append(", "));

                    if (!registros.isEmpty()) {
                        registros.setLength(registros.length() - 2);
                        registros.append(";");
                    }

                    consumerLoads.setData(registros.toString());
                    return consumerLoads;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        })
        .filter(Objects::nonNull)
        .toList();
    }

    private String formatarRotina(String rotina, String dataUltimoSincronismo, String dataAtual) {
        String dataUltimoSincronismoEscapada = dataUltimoSincronismo.replace("'", "''");
        String dataAtualEscapada = dataAtual.replace("'", "''");

        String novaRotina = rotina;

        return novaRotina;
    }

}

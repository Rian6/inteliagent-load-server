package com.inteliagent.load_server.entity;

import java.util.List;

public class Consumer {
    private String dataHoraInicioProcesso;
    private List<ConsumerLoads> consumerLoadsList;

    public String getDataHoraInicioProcesso() {
        return dataHoraInicioProcesso;
    }

    public void setDataHoraInicioProcesso(String dataHoraInicioProcesso) {
        this.dataHoraInicioProcesso = dataHoraInicioProcesso;
    }

    public List<ConsumerLoads> getConsumerLoadsList() {
        return consumerLoadsList;
    }

    public void setConsumerLoadsList(List<ConsumerLoads> consumerLoadsList) {
        this.consumerLoadsList = consumerLoadsList;
    }
}

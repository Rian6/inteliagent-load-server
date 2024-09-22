package com.inteliagent.main_server.cargas.service;

import com.inteliagent.main_server.cargas.dto.CargaDTO;

import java.util.ArrayList;
import java.util.List;

public class CargaService {
    public List<CargaDTO> buscarCargas(){
        List<CargaDTO> cargaDTOList = new ArrayList<>();
        cargaDTOList.add(new CargaDTO());
        cargaDTOList.add(new CargaDTO());
        cargaDTOList.add(new CargaDTO());
        cargaDTOList.add(new CargaDTO());
        cargaDTOList.add(new CargaDTO());
        cargaDTOList.add(new CargaDTO());

        return cargaDTOList;
    }
}

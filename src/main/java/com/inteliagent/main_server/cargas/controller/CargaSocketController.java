package com.inteliagent.main_server.cargas.controller;

import com.inteliagent.main_server.cargas.dto.CargaDTO;
import com.inteliagent.main_server.cargas.service.CargaService;
import com.inteliagent.main_server.response.dto.Response;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

public class CargaSocketController extends TextWebSocketHandler {

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        CargaService cargaService = new CargaService();
        List<CargaDTO> cargaDTOList = cargaService.buscarCargas();

        session.sendMessage(new TextMessage(new Response()
                .setMessage("Sucesso ao sincroinizar!")
                .setStatus(200)
                .setContent(cargaDTOList)
                .build()));
        session.close();
    }
}

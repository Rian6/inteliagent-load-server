package com.inteliagent.load_server.service;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SyncDownloadServiceTests {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void identity() {
        var jwt = Jwt.withTokenValue("test").header("alg", "HS256").claim("uid",7).claim("cede",3).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
    @Test void snapshotCannotAdvanceAnUnissuedOffsetOrCrossDevices() {
        identity(); var db=mock(NamedParameterJdbcTemplate.class);
        when(db.queryForList(anyString(),anyMap())).thenReturn(List.of());
        when(db.queryForObject(eq("SELECT NOW(6)"),anyMap(),eq(LocalDateTime.class))).thenReturn(LocalDateTime.now());
        var service=new SyncDownloadService(db);
        String device=UUID.randomUUID().toString();
        var page=service.consumer(7,true,0,"",device);
        String id=(String)page.get("downloadId");
        assertThrows(IllegalArgumentException.class,()->service.consumer(7,true,1,id,device));
        assertThrows(IllegalArgumentException.class,()->service.consumer(7,true,500,id,UUID.randomUUID().toString()));
        assertThrows(AccessDeniedException.class,()->service.consumer(8,true,0,id,device));
        assertThrows(IllegalArgumentException.class,()->service.consumer(7,true,0,id,null));
    }
    @Test void expiredSnapshotCannotAdvanceWatermark() {
        identity(); var db=mock(NamedParameterJdbcTemplate.class);
        when(db.queryForList(anyString(),anyMap())).thenReturn(List.of());
        when(db.queryForObject(eq("SELECT NOW(6)"),anyMap(),eq(LocalDateTime.class))).thenReturn(LocalDateTime.now());
        class AdjustableClock extends Clock {
            long millis=1_000_000;
            public ZoneId getZone(){return ZoneOffset.UTC;}
            public Clock withZone(ZoneId zone){return this;}
            public Instant instant(){return Instant.ofEpochMilli(millis);}
        }
        var clock=new AdjustableClock();var service=new SyncDownloadService(db,clock);
        String device=UUID.randomUUID().toString();
        var page=service.consumer(7,true,0,"",device);
        clock.millis+=300_001;
        assertThrows(IllegalArgumentException.class,()->service.consumerConfirm(List.of("PLANEJAMENTO","VISITA","INSPECAO","AMOSTRA","TRATAMENTO"),(String)page.get("downloadId"),7));
        verify(db,never()).update(startsWith("INSERT INTO sync_confirm"),anyMap());
    }
    @Test void samplesUseSmallPagesWithoutSkippingTheRemainingPhotos() {
        identity(); var db=mock(NamedParameterJdbcTemplate.class);
        when(db.queryForList(anyString(),anyMap())).thenReturn(List.of());
        when(db.queryForObject(eq("SELECT NOW(6)"),anyMap(),eq(LocalDateTime.class))).thenReturn(LocalDateTime.now());
        var rows=new ArrayList<Map<String,Object>>();
        for(int i=0;i<11;i++) rows.add(Map.of("ID",String.format("sample-%02d",i)));
        when(db.queryForList(contains("FROM AMOSTRA t"),anyMap())).thenReturn(rows,List.of(rows.getLast()));
        var service=new SyncDownloadService(db); String device=UUID.randomUUID().toString();
        var page=service.consumer(7,true,0,"",device);
        var loads=(List<Map<String,Object>>)page.get("consumerLoadsList");
        var samples=loads.stream().filter(row->row.get("tableName").equals("AMOSTRA")).findFirst().orElseThrow();
        assertEquals(10,((List<?>)samples.get("rows")).size()); assertEquals(true,samples.get("hasMore"));
        var next=service.consumer(7,true,500,(String)page.get("downloadId"),device);
        var nextLoads=(List<Map<String,Object>>)next.get("consumerLoadsList");
        var remaining=nextLoads.stream().filter(row->row.get("tableName").equals("AMOSTRA")).findFirst().orElseThrow();
        assertEquals(1,((List<?>)remaining.get("rows")).size()); assertEquals(false,remaining.get("hasMore"));
        verify(db,times(2)).queryForList(contains("LIMIT 11"),anyMap());
    }
}

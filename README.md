# Inteliagent load-server

Sincronização v3: operações idempotentes, versões para detecção de conflitos, cursores por dispositivo, JSON tipado, listas de tabelas/colunas permitidas, transações, paginação e watermarks controlados pelo servidor.

Java 21 e Spring Boot 4.1.2. O Maven Wrapper usa Maven 3.9.16 com checksum de distribuição.

## Executar

Configure as variáveis descritas em `.env.example` no processo. Spring Boot não carrega arquivos `.env` automaticamente. O ambiente integrado é iniciado por `runtime.py` na raiz do workspace e fornece credenciais independentes por serviço.

```powershell
.\mvnw.cmd package
java -jar target/load-server-0.0.1-SNAPSHOT.jar
```

Os testes de contexto precisam de um MySQL de teste configurado por `DB_URL`, `DB_USER` e `DB_PASSWORD` e uma chave JWT em Base64 em `JWT_SECRET`. Execute as migrações antes dos serviços. O workspace usa a porta 3308 para não alterar bancos existentes.

Controllers, serviços e persistência usam limites explícitos e injeção por construtor. Consulte os contratos e a arquitetura em `../docs/` no workspace integrado.


## CI/CD e produção

O workflow `.github/workflows/ci.yml` valida pushes e pull requests. Consulte a [infraestrutura e operação](https://github.com/Rian6/inteliagent-infra). Configure variáveis a partir de `.env.example`; segredos ficam em arquivos privados no servidor e GitHub Secrets. Não publique tokens, senhas ou arquivos de assinatura.

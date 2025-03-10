# Inteliagent Load Server

O **Inteliagent Load Server** é um servidor desenvolvido com **Spring Boot** que se conecta a um banco de dados para buscar informações de cargas e as envia para dispositivos móveis. Este projeto foi projetado para permitir a sincronização eficiente de dados em tempo real, garantindo alta disponibilidade e baixo tempo de resposta.

![arquitetura](https://github.com/user-attachments/assets/15c2d25d-0d54-44a9-9e95-27b0a8cb384c)

## Funcionalidades

- Conexão com banco de dados para recuperar informações de cargas.
- API RESTful para expor os dados das cargas.
- Sincronização de dados entre o servidor e dispositivos móveis.

## Requisitos

- **Java 11** ou superior.
- **Maven** para gerenciamento de dependências.
- Banco de dados relacional (MySQL, PostgreSQL, etc.).

## Estrutura do Projeto

- **src/main/java**: Código-fonte do servidor.
- **src/main/resources**: Configurações do servidor.

## Como Rodar o Projeto

1. **Clone o repositório:**
   
   ```bash
   git clone https://github.com/seu-usuario/inteliagent-load-server.git
   cd inteliagent-load-server

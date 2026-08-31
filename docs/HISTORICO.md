# Histórico de Desenvolvimento — Arquivo IV

> Documento de handoff da separação do projeto Valtio-Warranty-Manager.

## Origem

O **Arquivo IV** nasceu como fork do **Valtio Warranty Manager**
(`https://github.com/halexys-dotcom/Valtio-Warranty-Manager`), um gestor de
garantias Android desenvolvido pelo mesmo autor (Hugo Correia / HAConnect).

O fork foi feito porque a arquitetura do Valtio (Kotlin/Compose, MVVM, Hilt, Room,
WorkManager, iText) servia quase inteiramente o novo domínio de negócio:
arquivamento de faturas de fornecedores com retenção legal de 5 anos.

## Transformações de Domínio

| Valtio (original) | Arquivo IV |
|---|---|
| Entidade `Produto` + `Documento` | Entidade `Fatura` (anexo fundido, 1:1) |
| Alertas de fim de garantia | Alertas de fim de retenção legal (5 anos) |
| Multi-idioma (7 idiomas) | Apenas pt-PT |
| Package `com.valtio.app` | Package `pt.haconnect.arquivoiv` |

## Fases de Desenvolvimento (commits do Arquivo IV)

| Commit | Fase | Conteúdo |
|---|---|---|
| `39c62fc` | Fase 1 | Fork e rebranding, remoção do multi-idioma |
| `97ccd0f` | Fase 2 | Entidade `Fatura` substitui `Produto`/`Documento` (Room v4) |
| `89706d5` | Fase 3 | Formulário robusto: DatePicker M3, câmara+SAF, validações (Room v5) |
| `5c4e586` | Ajuste | Remoção do cartão de valor total do dashboard |
| `155e5f6` | Fase 4 | Pesquisa combinada por fornecedor + intervalo de datas |
| `855ceda` | Fase 5 | `RetencaoWorker`, notificações de 5 anos (Room v6) |
| `b96c607` | Fase 6 | Exportação PDF/CSV |
| `d208e4a` | Fase 10 | OCR com ML Kit |
| `e4dc293` | Schema | Remoção do campo "Valor" (Room v7) |
| `a6d2cb6` | Fase 9 | Layout do dashboard (Room v8) |
| `5df264c` | Fix | Correção do crash da partilha individual (FileProvider) |
| `b9c904d` | Fix | Correção das notificações de retenção |

## Histórico Git

Optou-se por **histórico limpo** (Opção A) para a publicação no repositório
`https://github.com/halexys-dotcom/Arquivo_IV`. O histórico antigo do Valtio
(commits herdados) foi descartado para manter o repositório independente e sem
referências a um projeto diferente.

## Decisões de Separação

1. **Licença:** GPL v3 (herdada do Valtio). O autor detém o copyright original
   de ambos os projetos, mantendo a licença copyleft.
2. **Identidade visual:** Paleta azul-marinho `#1B2A4A` + dourado `#B8935A`.
   O nome "Arquivo IV" e selo "DPIV" derivam de uma fase inicial associada à
   Fundação D. Pedro IV — a app é hoje propriedade da HAConnect.
3. **`fallbackToDestructiveMigration()`:** Aceitável em desenvolvimento;
   deve ser substituído por migrations Room reais antes da primeira release
   pública com dados de produção.
4. **Signing:** Credenciais movidas de `app/build.gradle.kts` para
   `keystore.properties` (gitignored). O ficheiro `arquivoiv-release.jks`
   também está gitignored.

## Notas Técnicas

- **`am start` falha em Honor/Magic OS** com notação relativa `.MainActivity`.
  Usar nome de classe totalmente qualificado.
- **Cache do Gradle corrompida:** resolver com `./gradlew --stop` antes de
  apagar a pasta `.gradle`.
- **Princípio do OCR:** "campo vazio é melhor que campo errado."

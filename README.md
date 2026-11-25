# Vale a Corrida

App Android que lê a oferta de corrida do Uber/99 e mostra um balão por cima com:
R$/km, R$/hora, custo de combustível e lucro, em verde, amarelo ou vermelho conforme as suas metas.

O app **só lê e mostra o cálculo**. Ele não aceita nem recusa corridas.

## Rodar no celular (Galaxy A14)

1. Abra a pasta `ValeCorrida` no **Android Studio** (File → Open). Espere o Gradle sincronizar.
2. No celular, ative o **modo desenvolvedor**: Configurações → Sobre o telefone → Informações do software → toque 7x em "Número de compilação".
3. Em Opções do desenvolvedor, ative **Depuração USB**.
4. Ligue o celular no PC pelo cabo, aceite o aviso no celular e clique em ▶ Run no Android Studio.

Para gerar um APK e instalar sem cabo: Build → Build App Bundle(s) / APK(s) → Build APK(s).

## Primeiro uso

1. **Ativar sobreposição**: ligue a chave do Vale a Corrida.
2. **Ativar leitura de tela**: em Apps instalados → Vale a Corrida → ativar.
   - Se a chave estiver cinza ("Configuração restrita"): **Abrir info do app** → ⋮ → *Permitir configurações restritas* → volte e ative.
3. Em Bateria, deixe o app como **Sem restrições**.
4. Preencha o preço do combustível, o consumo e as metas e toque em **Salvar**.
5. Toque em **Mostrar balão de teste**.

## Se o balão não aparecer na oferta

- Na seção **Diagnóstico** aparece o último texto lido do Uber/99. Toque em **Copiar texto lido** para ajustar o leitor (`OfertaParser.kt`).
- Para descobrir o pacote da 99: abra a 99, volte ao app e veja "Último app aberto". Coloque esse pacote em "Apps monitorados" e salve.

## Arquivos

| Arquivo | O que faz |
|---|---|
| `MainActivity.kt` | Tela de permissões, configurações, teste e diagnóstico |
| `CorridaService.kt` | Serviço de acessibilidade: lê a tela do Uber/99 |
| `OfertaParser.kt` | Extrai valor, km e minutos do texto |
| `Calculadora.kt` | R$/km, R$/h, combustível, lucro e cor |
| `Balao.kt` | O balão por cima da tela (toque fecha, arrastar move) |
| `Prefs.kt` | Configurações salvas |

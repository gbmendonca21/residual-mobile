# Residual para Android — versão de teste

Projeto independente do sistema web. Não altera o repositório compartilhado, o GitHub Pages nem a estrutura do Supabase.

O APK abre `https://ragcodebr.github.io/sistema-residual/` em uma WebView Android. Usa o login existente, mantém a sessão no armazenamento privado do aplicativo e oferece seleção de anexos e salvamento de downloads. Links externos abrem em outro aplicativo. Não contém chaves do Supabase nem credenciais de usuários.

## Atualizações

As alterações publicadas no site aparecem ao recarregar o aplicativo. Use o botão Atualizar na barra superior para recarregar. Uma mudança feita apenas no código local ou em outra branch não aparece no APK até ser publicada no site.

Permissões Android, ícones e código nativo exigem outro APK, assinado pela mesma chave e com versionCode maior. A versão de teste depende de internet; não oferece trabalho offline ou notificações push. Uploads usam o seletor de arquivos do Android, sem captura direta de câmera nesta versão. Downloads HTTPS têm limite de 100 MB; arquivos gerados no navegador, 20 MB. Login Google/OAuth e importações pelo Android Share não estão implementados nesta prévia.

## Compilar e testar

Requisitos: JDK 17, Android SDK Platform 36, Build Tools 35.0.0 e `ANDROID_HOME` configurado. Android mínimo: 8.0 (API 26).

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest
```

O segundo comando exige um dispositivo ou emulador Android conectado por ADB. Os testes instrumentados verificam o formulário de login publicado e o seletor de salvamento de um download gerado. Não contêm senhas nem alteram dados reais.

APK: `app/build/outputs/apk/debug/app-debug.apk`. A assinatura de desenvolvimento é apenas para testes. Preserve a chave de assinatura e o identificador `br.com.residual.mobile.preview` para atualizar esta instalação. Para distribuição definitiva, configure uma chave de produção, um identificador definitivo e o processo de atualização/distribuição adequado.

## iPhone e iPad

APK instala somente no Android. O mesmo sistema web já pode ser adicionado à tela inicial no Safari. Um aplicativo iOS distribuído por TestFlight ou App Store exige outro projeto nativo, Xcode, assinatura Apple e acesso ao Apple Developer Program; esta pasta não contém um IPA compilado.

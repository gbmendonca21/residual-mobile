# Residual para Android

Projeto independente do sistema web. Não altera o repositório compartilhado, o GitHub Pages nem a estrutura do Supabase.

O APK abre `https://ragcodebr.github.io/sistema-residual/` em uma WebView Android. Usa o login existente, mantém a sessão no armazenamento privado do aplicativo e oferece seleção de anexos e salvamento de downloads. Links externos abrem em outro aplicativo. Não contém chaves do Supabase nem credenciais de usuários.

Nome: **Residual**. Identificador definitivo: **`br.com.residual.app`**. A versão 1.0.0 tem `versionCode=1` e usa uma chave de assinatura de produção, distinta da chave de desenvolvimento da prévia.

## Instalar e compartilhar

Baixe o APK 1.0.0 nos [Releases](https://github.com/gbmendonca21/residual-mobile/releases). Compartilhe apenas o link do APK com os colaboradores. No Android, abra o arquivo baixado e permita a instalação pelo navegador quando o sistema solicitar. Entre com a mesma conta usada no site.

A versão definitiva é uma instalação separada da prévia `Residual · Teste`. Ela não migra a sessão da prévia; faça login novamente. Depois de validar o acesso, a prévia pode ser desinstalada.

## Atualizações

As alterações publicadas no site aparecem ao recarregar o aplicativo. Use o botão Atualizar na barra superior para recarregar. Uma mudança feita apenas no código local ou em outra branch não aparece no APK até ser publicada no site.

Permissões Android, ícones e código nativo exigem outro APK, assinado pela mesma chave e com versionCode maior. Para atualizações nativas, os colaboradores precisam baixar e instalar o novo APK por cima da versão anterior. Não há instalador automático de APKs.

O aplicativo depende de internet; não oferece trabalho offline ou notificações push. Uploads usam o seletor de arquivos do Android, sem captura direta de câmera nesta versão. Downloads HTTPS têm limite de 100 MB; arquivos gerados no navegador, 20 MB. Login Google/OAuth e importações pelo Android Share não estão implementados.

## Compilar e testar

Requisitos: JDK 17, Android SDK Platform 36, Build Tools 35.0.0 e `ANDROID_HOME` configurado. Android mínimo: 8.0 (API 26).

```sh
./gradlew assembleRelease testReleaseUnitTest lintRelease
./gradlew connectedReleaseAndroidTest
```

Os comandos de produção usam o arquivo de assinatura privado. O segundo exige um dispositivo ou emulador Android conectado por ADB e testa a variante release assinada. Os testes instrumentados verificam o formulário de login publicado e o seletor de salvamento de um download gerado. Não contêm senhas nem alteram dados reais.

APK assinado: `app/build/outputs/apk/release/app-release.apk`. Para desenvolvimento, `./gradlew assembleDebug` gera uma instalação independente com identificador `br.com.residual.app.debug` e assinatura de desenvolvimento.

## Assinatura de produção e backup

A configuração é lida de `~/Documents/Residual/assinatura-android/release.properties`. Para usar outro caminho, configure `RESIDUAL_SIGNING_PROPERTIES` ou a propriedade Gradle `residualSigningProperties`. O arquivo contém:

```properties
storeFile=/caminho/privado/residual-release.p12
storeType=PKCS12
storePassword=SENHA_PRIVADA
keyAlias=residual
keyPassword=SENHA_PRIVADA
```

A chave e as senhas ficam fora do repositório. Preserve a chave privada em backup criptografado e guarde a senha em um gerenciador de senhas. Valide a recuperação do backup antes de depender dele. Não publique esses arquivos nem os coloque no APK. O certificado público pode ser compartilhado; a chave privada e as senhas não.

A impressão SHA-256 pública do certificado está em `app/signing-certificate.sha256`. A compilação de produção confere esse valor para impedir uma atualização assinada com outra chave por engano.

Para cada atualização nativa, aumente `versionCode`, ajuste `versionName`, compile com a mesma chave e confirme o identificador e a impressão SHA-256 do certificado com `apksigner verify --print-certs`. Publique somente o APK e seu checksum. A perda da chave impede atualizar as instalações existentes com o mesmo identificador.

## iPhone e iPad

APK instala somente no Android. O mesmo sistema web já pode ser adicionado à tela inicial no Safari. Um aplicativo iOS distribuído por TestFlight ou App Store exige outro projeto nativo, Xcode, assinatura Apple e acesso ao Apple Developer Program; esta pasta não contém um IPA compilado.

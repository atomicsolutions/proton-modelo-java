package br.com.protoncloud.modelo.mobile;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.protoncloud.modelo.apoio.Config;
import br.com.protoncloud.modelo.apoio.Evidencias;
import br.com.protoncloud.modelo.apoio.Sessao;
import br.com.protoncloud.modelo.apoio.Sessoes;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;

/**
 * A sessão mobile: um aparelho Android pelo Appium (UiAutomator2), por cenário.
 * <p>
 * Configuração ({@link Config}): {@code APPIUM_URL}, {@code MOBILE_DISPOSITIVO} (o serial do
 * {@code adb devices}; vazio usa o primeiro conectado) e {@code MOBILE_APP} (o caminho do
 * .apk; vazio abre o app já instalado, pelo {@code MOBILE_PACOTE} e pela
 * {@code MOBILE_ACTIVITY}). Numa execução do Proton, o aparelho e o app escolhidos no
 * disparo chegam pelo ponto de entrada do Proton.
 * </p>
 * <p>
 * Pré-requisitos na máquina: Android SDK (adb), Appium com o driver UiAutomator2
 * ({@code npm install -g appium} e {@code appium driver install uiautomator2}) e o aparelho
 * com a depuração USB ligada. Se o Appium não estiver no ar no endereço local configurado, a
 * sessão o inicia e o encerra no fim do cenário.
 * </p>
 */
public final class SessaoMobile implements Sessao {

    private static final Logger log = LoggerFactory.getLogger(SessaoMobile.class);
    private static final Duration TEMPO_PARA_O_APPIUM_SUBIR = Duration.ofSeconds(60);

    private AppiumDriverLocalService servico;
    private final AndroidDriver driver;

    private SessaoMobile() {
        URL url = endereco();
        servico = appiumNoAr() ? null : iniciarAppium(url);

        var opcoes = new UiAutomator2Options()
            .setNewCommandTimeout(Duration.ofMinutes(5))
            .setAutoGrantPermissions(true)
            // Muitos apps abrem por uma tela de abertura que some logo: qualquer tela do app
            // serve como sinal de que ele abriu.
            .setAppWaitActivity("*");

        if (Config.mobileDispositivo() != null) {
            opcoes.setUdid(Config.mobileDispositivo());
        }

        if (Config.mobileApp() != null) {
            opcoes.setApp(Path.of(Config.mobileApp()).toAbsolutePath().toString());
        } else {
            opcoes.setAppPackage(Config.mobilePacote()).setAppActivity(Config.mobileActivity());
        }

        try {
            driver = new AndroidDriver(url, opcoes);
        } catch (RuntimeException e) {
            pararAppium();
            throw e;
        }

        var capacidades = driver.getCapabilities();
        log.info("Aparelho {}", Stream.of("appium:deviceUDID", "deviceUDID", "appium:udid", "udid")
            .map(capacidades::getCapability).filter(Objects::nonNull).findFirst().orElse("?"));
    }

    private static SessaoMobile sessao() {
        return Sessoes.obter("mobile", SessaoMobile.class, SessaoMobile::new);
    }

    /** O driver do Appium do cenário. */
    public static AndroidDriver driver() {
        return sessao().driver;
    }

    /** Grava um print da tela do aparelho na pasta de evidências. */
    public static Path printDaTela(String nome) {
        return sessao().evidencia(nome);
    }

    @Override
    public Path evidencia(String nome) {
        Path arquivo = Evidencias.arquivo(nome, "png");

        try {
            Files.copy(driver.getScreenshotAs(OutputType.FILE).toPath(), arquivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Evidencias.registrar(arquivo);
    }

    @Override
    public void close() {
        try {
            driver.quit();
        } finally {
            pararAppium();
        }
    }

    private void pararAppium() {
        if (servico != null) {
            servico.stop();
            servico = null;
        }
    }

    private static URL endereco() {
        try {
            return URI.create(Config.appiumUrl()).toURL();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean appiumNoAr() {
        try (var cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            var pedido = HttpRequest.newBuilder(URI.create(Config.appiumUrl() + "/status")).timeout(Duration.ofSeconds(3)).build();
            return cliente.send(pedido, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static AppiumDriverLocalService iniciarAppium(URL url) {
        if (!url.getHost().equals("127.0.0.1") && !url.getHost().equals("localhost")) {
            throw new IllegalStateException("O Appium não responde em " + Config.appiumUrl() + ".");
        }

        log.info("Iniciando o Appium em {}", Config.appiumUrl());
        AppiumDriverLocalService servico = new AppiumServiceBuilder()
            .withIPAddress(url.getHost())
            .usingPort(url.getPort() > 0 ? url.getPort() : 4723)
            .withLogFile(new File("target/appium.log"))
            .build();
        // O log do Appium vai para o target/appium.log, e não para a saída, que o runner
        // sobe como log da execução.
        servico.clearOutPutStreams();
        servico.start();

        // Confere pelo próprio /status antes de abrir a sessão.
        Instant limite = Instant.now().plus(TEMPO_PARA_O_APPIUM_SUBIR);

        while (!appiumNoAr()) {
            if (Instant.now().isAfter(limite)) {
                servico.stop();
                throw new IllegalStateException("O Appium não respondeu em " + TEMPO_PARA_O_APPIUM_SUBIR.toSeconds()
                    + " s em " + Config.appiumUrl() + ".");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        return servico;
    }
}

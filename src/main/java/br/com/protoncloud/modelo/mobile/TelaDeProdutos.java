package br.com.protoncloud.modelo.mobile;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import br.com.protoncloud.modelo.apoio.Config;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

/**
 * A lista de produtos do My Demo App, o app de demonstração da Sauce Labs
 * (https://github.com/saucelabs/my-demo-app-android), com os mesmos produtos da loja web.
 */
public class TelaDeProdutos {

    static final String ID = "com.saucelabs.mydemoapp.android:id/";

    private final AndroidDriver driver = SessaoMobile.driver();
    private final WebDriverWait espera = new WebDriverWait(driver, Config.timeoutMobile());

    public TelaDoProduto abrirProduto(String produto) {
        espera.until(ExpectedConditions.presenceOfElementLocated(By.id(ID + "productRV")));

        // Rola a lista até o produto e toca na foto dele, que fica no mesmo cartão do nome.
        driver.findElement(AppiumBy.androidUIAutomator(
            "new UiScrollable(new UiSelector().resourceId(\"" + ID + "productRV\"))"
                + ".scrollIntoView(new UiSelector().resourceId(\"" + ID + "titleTV\").text(\"" + produto + "\")"
                + ".fromParent(new UiSelector().resourceId(\"" + ID + "productIV\")))"))
            .click();

        return new TelaDoProduto();
    }
}

package br.com.protoncloud.modelo.mobile;

import static br.com.protoncloud.modelo.mobile.TelaDeProdutos.ID;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import br.com.protoncloud.modelo.apoio.Config;
import io.appium.java_client.android.AndroidDriver;

/** O detalhe de um produto no My Demo App. */
public class TelaDoProduto {

    private final AndroidDriver driver = SessaoMobile.driver();
    private final WebDriverWait espera = new WebDriverWait(driver, Config.timeoutMobile());

    public TelaDoProduto() {
        espera.until(ExpectedConditions.presenceOfElementLocated(By.id(ID + "cartBt")));
    }

    public String nome() {
        return driver.findElement(By.id(ID + "productTV")).getText();
    }

    /** {@code $ 29.99} vira {@code 29.99}. */
    public String preco() {
        return driver.findElement(By.id(ID + "priceTV")).getText().replace("$", "").strip();
    }

    public int itensNoCarrinho() {
        List<WebElement> contador = driver.findElements(By.id(ID + "cartTV"));
        return contador.isEmpty() ? 0 : Integer.parseInt(contador.get(0).getText().strip());
    }

    public void adicionarAoCarrinho() {
        int antes = itensNoCarrinho();
        driver.findElement(By.id(ID + "cartBt")).click();
        espera.withMessage("O carrinho não passou de " + antes + " para " + (antes + 1) + " itens")
            .until(d -> itensNoCarrinho() == antes + 1);
    }
}

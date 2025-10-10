package automation.helpers;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import automation.config.ConfigManager;

public class HighlightHelper {

    public enum Color {
        RED("#FF0000"),
        AMBER("#FFA500"),
        GREEN("#00FF00"),
        BLUE("#0000FF"),
        DARK_RED("#8B0000");

        private final String hex;
        Color(String hex) { this.hex = hex; }
        public String hex() { return hex; }
    }

    public enum Linger { ON, OFF }

    public static void highlight(WebDriver driver, WebElement element, Color color, Linger linger) {
        if (!ConfigManager.getInstance().getConfig().getAutomation().isVisualDebug()) return;

        try {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.border='3px solid " + color.hex() + "';" +
                "arguments[0].style.boxShadow='0 0 8px " + color.hex() + "';" +
                "arguments[0].style.zIndex='9999';" +
                "arguments[0].style.transition='border 0.2s ease';",
                element
            );

            // linger logic: pause only if Linger is OFF
            if (linger == Linger.OFF) {
                Thread.sleep(300); // pause to see highlight
                try {
                    ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].style.border='';" +
                        "arguments[0].style.boxShadow='';" +
                        "arguments[0].style.zIndex='';" +
                        "arguments[0].style.transition='';",
                        element
                    );
                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            // silent fail
        }
    }
}

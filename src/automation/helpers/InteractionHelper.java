package automation.helpers;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

public class InteractionHelper {
    private final JavascriptExecutor js;
    private final boolean highlightEnabled;
    private final String highlightColor;

    public InteractionHelper(JavascriptExecutor js, boolean highlightEnabled, String highlightColor) {
        this.js = js;
        this.highlightEnabled = highlightEnabled;
        this.highlightColor = highlightColor;
    }

    public void performAction(WebElement element, String actionType) {
        if (highlightEnabled) {
            highlightElement(element, highlightColor);
        }
        element.click();
    }

    public void markCompleted(WebElement element) {
        if (highlightEnabled) {
            highlightElement(element, "#00AA00"); // Green for completion
        }
    }

    private void highlightElement(WebElement element, String color) {
        js.executeScript(
            "arguments[0].style.border='2px solid " + color + "';" +
            "arguments[0].style.boxShadow='0 0 5px " + color + "';" +
            "arguments[0].style.transition='border 0.3s ease';",
            element
        );
    }
}
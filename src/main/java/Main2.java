import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class Main2 {

    private static final int MAX_RUN_MINUTES = 345;
    private static final boolean TODAY_OFF = false;

    public static void main(String[] args) {

        if (TODAY_OFF) {
            return;
        }

        String user = System.getenv("GAME_ID_MOBI");
        if (user == null || user.isEmpty()) {
            user = System.getenv("GAME_ID"); // Fallback to standard GAME_ID
        }

        String pass = System.getenv("GAME_PASSWORD_MOBI");
        if (pass == null || pass.isEmpty()) {
            pass = System.getenv("GAME_PASSWORD"); // Fallback to standard GAME_PASSWORD
        }

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            throw new RuntimeException("GAME_ID or GAME_PASSWORD secrets not found.");
        }

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        Random random = new Random();
        Instant startTime = Instant.now();

        try {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));

            // 1. Login Block
            driver.get("https://elem.mobi/login/");
            sleep(2000);

            List<WebElement> userInputs = driver.findElements(By.name("plogin"));
            List<WebElement> passInputs = driver.findElements(By.name("ppass"));
            List<WebElement> submitBtns = driver.findElements(By.cssSelector("input[type='submit']"));

            if (!userInputs.isEmpty() && !passInputs.isEmpty() && !submitBtns.isEmpty()) {
                userInputs.get(0).sendKeys(user);
                passInputs.get(0).sendKeys(pass);
                submitBtns.get(0).click();
                sleep(3000);
            } else {
                return;
            }

            // 2. Navigate to Invasion section (/urfin/)
            boolean navigated = false;
            for (int attempt = 1; attempt <= 5; attempt++) {
                List<WebElement> urfinLinks = driver.findElements(By.cssSelector("a.urfin, a[href*='/urfin/']"));
                if (!urfinLinks.isEmpty()) {
                    urfinLinks.get(0).click();
                    navigated = true;
                    sleep(2000);
                    break;
                }
                sleep(1000);
            }

            if (!navigated) {
                return;
            }

            JavascriptExecutor js = (JavascriptExecutor) driver;

            while (true) {
                if (shouldStopNow(startTime)) {
                    break;
                }

                // 3. Click initial "Напасть" (Start) button if present
                List<WebElement> startBtns = driver.findElements(By.xpath("//a[contains(@href, '/urfin/start/') and not(contains(@class, 'orange'))]"));
                if (!startBtns.isEmpty()) {
                    try {
                        js.executeScript("arguments[0].click();", startBtns.get(0));
                        sleep(1500);
                    } catch (Exception ignored) {}
                }

                // XPath for attack0, attack1, attack2 excluding hide2 / chide2 in href or class
                String attackXPath = "//a[(contains(@href, '/urfin/battle/attack0/') or " +
                        "contains(@href, '/urfin/battle/attack1/') or " +
                        "contains(@href, '/urfin/battle/attack2/')) and " +
                        "not(contains(@href, 'hide2')) and not(contains(@href, 'chide2')) and " +
                        "not(contains(@class, 'hide2')) and not(contains(@class, 'chide2'))]";

                // 4. Attack Loop with 10-second double-check verification (checked twice)
                while (true) {
                    List<WebElement> attacks = driver.findElements(By.xpath(attackXPath));

                    if (!attacks.isEmpty()) {
                        try {
                            js.executeScript("arguments[0].click();", attacks.get(0));
                            sleep(1500);
                        } catch (Exception ignored) {}
                    } else {
                        // Double-check verification: wait 10 seconds and re-check twice
                        boolean foundOnRecheck = false;
                        for (int recheck = 1; recheck <= 2; recheck++) {
                            sleep(10000); // Wait 10 seconds
                            driver.navigate().refresh();
                            sleep(1000);

                            List<WebElement> recheckAttacks = driver.findElements(By.xpath(attackXPath));
                            if (!recheckAttacks.isEmpty()) {
                                foundOnRecheck = true;
                                break;
                            }
                        }

                        if (!foundOnRecheck) {
                            break; // Exit attack loop when confirmed empty twice
                        }
                    }
                }

                // 5. Click "Далее" (Next) button
                List<WebElement> nextBtns = driver.findElements(By.xpath("//a[contains(@href, '/urfin/')]//span[contains(text(), 'Далее')] | //span[contains(text(), 'Далее')]"));
                if (!nextBtns.isEmpty()) {
                    try {
                        js.executeScript("arguments[0].click();", nextBtns.get(0));
                        sleep(1500);
                    } catch (Exception ignored) {}
                }

                // 6. Gold Cost Check for Immediate Attack ("Напасть сразу за ...")
                List<WebElement> goldBtnSpans = driver.findElements(By.xpath("//a[contains(@href, '/urfin/start/')]//span[contains(text(), 'Напасть сразу за')] | //span[contains(text(), 'Напасть сразу за')]"));

                if (!goldBtnSpans.isEmpty()) {
                    try {
                        String text = goldBtnSpans.get(0).getText();
                        String digits = text.replaceAll("[^0-9]", "");

                        if (!digits.isEmpty()) {
                            int goldCost = Integer.parseInt(digits);

                            if (goldCost <= 0) {
                                // Click gold attack button
                                WebElement parentLink = goldBtnSpans.get(0).findElement(By.xpath("./ancestor::a"));
                                js.executeScript("arguments[0].click();", parentLink);
                                sleep(1500);

                                // Click confirmation "Да!"
                                List<WebElement> confirmBtns = driver.findElements(By.xpath("//a[contains(@href, '/urfin/start/confirmed/')] | //span[text()='Да!']"));
                                if (!confirmBtns.isEmpty()) {
                                    js.executeScript("arguments[0].click();", confirmBtns.get(0));
                                    sleep(1500);
                                }

                                continue; // Loop back to attack cycle
                            } else {
                                // Gold cost > 20: Sleep for 16-20 minutes
                                int sleepMinutes = 16 + random.nextInt(5); // Random between 16 and 20
                                sleep(sleepMinutes * 60 * 1000);

                                driver.navigate().to("https://elem.mobi/urfin/");
                                sleep(2000);
                                continue;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // Fallback page refresh cycle
                sleep(2000);
                driver.navigate().refresh();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    public static boolean shouldStopNow(Instant startTime) {
        long elapsedMinutes = Duration.between(startTime, Instant.now()).toMinutes();
        return elapsedMinutes >= MAX_RUN_MINUTES;
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

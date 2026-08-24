import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    private static final int MAX_RUN_MINUTES = 345;
    private static final boolean TODAY_OFF = false;

    public static void main(String[] args) {

        if (TODAY_OFF) {
            System.out.println("Bot OFF today. Exiting.");
            return;
        }

        String user = System.getenv("GAME_ID");
        String pass = System.getenv("GAME_PASSWORD");

        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            throw new RuntimeException("GAME_ID or GAME_PASSWORD not found in GitHub Secrets.");
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
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

            driver.get("https://elem.cards/login/");
            sleep(2000);

            // Crash-proof login block
            List<WebElement> userInputs = driver.findElements(By.name("plogin"));
            List<WebElement> passInputs = driver.findElements(By.name("ppass"));
            List<WebElement> submitBtns = driver.findElements(By.cssSelector("input[type='submit']"));

            if (!userInputs.isEmpty() && !passInputs.isEmpty() && !submitBtns.isEmpty()) {
                userInputs.get(0).sendKeys(user);
                passInputs.get(0).sendKeys(pass);
                submitBtns.get(0).click();
                sleep(4000);
            } else {
                System.out.println("Could not find login fields. Exiting.");
                return;
            }

            // Crash-proof urf.in click with retries
            boolean navigated = false;
            for (int attempt = 1; attempt <= 5; attempt++) {
                List<WebElement> urfinLinks = driver.findElements(By.cssSelector("a.urfin"));
                if (!urfinLinks.isEmpty()) {
                    urfinLinks.get(0).click();
                    navigated = true;
                    sleep(3000);
                    break;
                }
                sleep(2000);
            }

            if (!navigated) {
                System.out.println("Could not find a.urfin link. Exiting.");
                return;
            }

            int consecutiveIdle = 0; // Tracks consecutive loops with no actions

            while (true) {
                long loopStart = System.currentTimeMillis();

                if (shouldStopNow(startTime)) {
                    System.out.println("Stopping now due to runtime limit.");
                    break;
                }

                boolean actionPerformed = false;

                // -------- Collect attack links first --------

                List<String> attackLinks = new ArrayList<>();
                List<WebElement> attack0 = driver.findElements(By.cssSelector("a[href*='attack0']"));
                List<WebElement> attack1 = driver.findElements(By.cssSelector("a[href*='attack1']"));
                List<WebElement> attack2 = driver.findElements(By.cssSelector("a[href*='attack2']"));

                System.out.println(
                        "attack0: " + attack0.size() +
                        " | attack1: " + attack1.size() +
                        " | attack2: " + attack2.size()
                );

                for (WebElement e : attack0) attackLinks.add(e.getAttribute("href"));
                for (WebElement e : attack1) attackLinks.add(e.getAttribute("href"));
                for (WebElement e : attack2) attackLinks.add(e.getAttribute("href"));

                if (!attackLinks.isEmpty()) {
                    actionPerformed = true;
                }

                // -------- Visit each attack --------

                for (String link : attackLinks) {
                    try {
                        driver.get(link);
                        sleep(800);
                    } catch (Exception ignored) {
                    }
                }

                // -------- Attack button --------

                List<WebElement> attackBtn = driver.findElements(By.xpath("//span[text()='Attack']"));
                if (!attackBtn.isEmpty()) {
                    try {
                        attackBtn.get(0).click();
                        actionPerformed = true;
                        sleep(1500);
                    } catch (Exception ignored) {
                    }
                }

                // -------- Gold attack --------

                List<WebElement> goldAttack = driver.findElements(By.xpath("//span[contains(text(),'Attack now for')]"));
                if (!goldAttack.isEmpty()) {
                    try {
                        String text = goldAttack.get(0).getText();
                        String number = text.replaceAll("[^0-9]", "");

                        if (!number.isEmpty()) {
                            int cost = Integer.parseInt(number);

                            if (cost <= 0) {
                                goldAttack.get(0).click();
                                sleep(1200);

                                List<WebElement> yes = driver.findElements(By.xpath("//span[text()='Yes!']"));
                                if (!yes.isEmpty()) {
                                    yes.get(0).click();
                                }

                                actionPerformed = true;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                // -------- Next button --------

                List<WebElement> nextBtn = driver.findElements(By.xpath("//span[text()='Next']"));
                if (!nextBtn.isEmpty()) {
                    try {
                        nextBtn.get(0).click();
                        actionPerformed = true;
                        sleep(1500);
                    } catch (Exception ignored) {
                    }
                }

                if (shouldStopNow(startTime)) {
                    System.out.println("Stopping now due to runtime limit.");
                    break;
                }

                // -------- Dynamic Idle & Sleep Logic --------

                if (actionPerformed) {
                    consecutiveIdle = 0; // Reset counter if we did something
                    long elapsed = System.currentTimeMillis() - loopStart;
                    long remaining = 10000 - elapsed;

                    if (remaining > 0) {
                        sleep((int) remaining);
                    }
                } else {
                    consecutiveIdle++; // Increase counter if nothing was found
                    int sleepTimeMs;

                    if (consecutiveIdle >= 2) {
                        System.out.println("No targets found twice. Sleeping 15-16 minutes...");
                        int minMs = 15 * 60 * 1000;
                        int maxMs = 16 * 60 * 1000;
                        sleepTimeMs = random.nextInt(maxMs - minMs + 1) + minMs;
                    } else {
                        System.out.println("No targets or cost > 20. Sleeping 5-6 minutes...");
                        int minMs = 5 * 60 * 1000;
                        int maxMs = 6 * 60 * 1000;
                        sleepTimeMs = random.nextInt(maxMs - minMs + 1) + minMs;
                    }

                    sleep(sleepTimeMs);
                }

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

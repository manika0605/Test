package MinPrice;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

public class LaunchPage {
	static String lastWindowHandle = new String();
	static String parantWindowHandle = new String();
	static WebDriver driver;
	public static void main (String [] args) throws Exception
	{

		System.setProperty("webdriver.chrome.driver", "C:\\Users\\Msunal\\Downloads\\selenium\\selenium-java-3.141.59\\libs\\chromedriver.exe");
		driver=new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		// open indeed home page
		driver.get("https://www.amazon.in");
		driver.manage().window().maximize();
		driver.findElement(By.xpath("//span[contains(text(), 'Hello. Sign in')]")).click();
		driver.findElement(By.id("ap_email")).sendKeys("8630410767");
		driver.findElement(By.id("continue")).click();
		driver.findElement(By.id("ap_password")).sendKeys("manikasunal");
		driver.findElement(By.id("signInSubmit")).click();


		driver.findElement(By.id("nav-link-shopall")).click();
		//WebElement ele=driver.findElement(By.xpath("//a[@href='Womens-Clothing'][id='shopAllLinks']"));
		driver.findElement(By.id("twotabsearchtextbox")).sendKeys("men tshirts");
		driver.findElement(By.xpath("//input[@type='submit']")).click();
		driver.findElement(By.xpath("//span[.='Sort by:']")).click();


		driver.findElement(By.xpath("//a[.='Price: Low to High']")).click();
		driver.findElement(By.xpath("//*[@data-index='0']")).click();
		// driver.findElement(By.xpath("//select[@name='dropdown_selected_size_name']")).click();
		Set<String> allWindowHandles = driver.getWindowHandles();

		for (String handle : allWindowHandles) {
			lastWindowHandle = handle;
		}
		Thread.sleep(10000);
		driver.switchTo().window(lastWindowHandle).manage().window().maximize();

		driver.findElement(By.id("native_dropdown_selected_size_name")).click();
		// driver.findElement(By.xpath("//option[.='S']")).click();
		WebElement sizeOptions = driver.findElement(By.xpath("//select[contains(@name, 'dropdown_selected_size_name')]"));
		Select sel = new Select(sizeOptions);
		// sel.selectByVisibleText("S");
		sel.selectByVisibleText("L");

		driver.findElement(By.id("buybox-see-all-buying-choices-announce")).click();
		driver.findElement(By.name("submit.addToCart")).click();
		driver.findElement(By.id("hlb-ptc-btn-native")).click();
	}

	//public static void clickOnElement(String locaitorvalue,String locatortype) {
	//	
	//	
	//	switch(locatortype)
	//		
	//	driver.findElement(By.xpath(xpath));

}


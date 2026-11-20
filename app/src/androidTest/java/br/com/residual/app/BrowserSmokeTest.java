package br.com.residual.app;

import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BrowserSmokeTest {
    private WebView findWeb(View view) {
        if (view instanceof WebView) return (WebView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView result = findWeb(group.getChildAt(i));
                if (result != null) return result;
            }
        }
        return null;
    }

    private String js(ActivityScenario<MainActivity> scenario, String script) throws Exception {
        AtomicReference<String> value = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        scenario.onActivity(activity -> findWeb(activity.getWindow().getDecorView()).evaluateJavascript(script, result -> {
            value.set(result); latch.countDown();
        }));
        assertTrue("WebView JavaScript callback", latch.await(10, TimeUnit.SECONDS));
        return value.get();
    }

    private void awaitLogin(ActivityScenario<MainActivity> scenario) throws Exception {
        long deadline = System.currentTimeMillis() + 60000;
        while (System.currentTimeMillis() < deadline) {
            String count = js(scenario, "document.querySelectorAll('input[type=password]').length");
            if ("1".equals(count)) return;
            Thread.sleep(500);
        }
        fail("The hosted login form did not load in WebView");
    }

    @Test public void opensHostedLoginAndKeepsRemoteCode() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitLogin(scenario);
            String url = js(scenario, "window.location.href");
            assertTrue(url.contains("https://ragcodebr.github.io/sistema-residual/"));
            assertEquals("true", js(scenario, "document.querySelector('input[type=email]')!==null"));
            assertEquals("true", js(scenario, "document.body.innerText.includes('Entrar')"));
            scenario.onActivity(activity -> assertFalse(findWeb(activity.getWindow().getDecorView()).getSettings().getAllowFileAccess()));
            scenario.onActivity(activity -> findWeb(activity.getWindow().getDecorView()).reload());
            awaitLogin(scenario);
            UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            assertFalse("GitHub Pages fallback must not show a network error", device.hasObject(By.textContains("Não foi possível abrir o Residual")));
        }
    }

    @Test public void generatedDownloadOffersAndroidFilePicker() throws Exception {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitLogin(scenario);
            assertEquals("true", js(scenario, "typeof ResidualDownloads==='object'"));
            js(scenario, "(()=>{const a=document.createElement('a');a.href=URL.createObjectURL(new Blob(['Residual APK test'],{type:'text/plain'}));a.download='residual-teste.txt';document.body.appendChild(a);a.click();return true})()");
            assertTrue("Android Save As picker", device.wait(Until.hasObject(By.textContains("residual-teste")), 15000));
            device.pressBack();
        }
    }
}

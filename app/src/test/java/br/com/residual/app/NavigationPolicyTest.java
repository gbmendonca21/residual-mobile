package br.com.residual.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationPolicyTest {
    @Test public void allowsApplicationRoutesAndQueries() {
        assertTrue(NavigationPolicy.isInternal(NavigationPolicy.HOME));
        assertTrue(NavigationPolicy.isInternal(NavigationPolicy.HOME + "auth?next=%2Fdocumentos"));
        assertTrue(NavigationPolicy.isInternal(NavigationPolicy.HOME + "documentos#arquivo"));
    }
    @Test public void rejectsOldHostingOtherWorkersAndHostSpoofing() {
        assertFalse(NavigationPolicy.isInternal("https://ragcodebr.github.io/another-project/"));
        assertFalse(NavigationPolicy.isInternal("https://ragcodebr.github.io/sistema-residual/../another-project/"));
        assertFalse(NavigationPolicy.isInternal("https://ragcodebr.github.io.evil.test/sistema-residual/"));
        assertFalse(NavigationPolicy.isInternal("https://evil.test@ragcodebr.github.io/sistema-residual/"));
        assertFalse(NavigationPolicy.isInternal("https://ragcodebr.github.io:8443/sistema-residual/"));
        assertFalse(NavigationPolicy.isInternal("https://another.hotelrovi-ponto.workers.dev/"));
        assertFalse(NavigationPolicy.isInternal("https://residual.hotelrovi-ponto.workers.dev.evil.test/"));
        assertFalse(NavigationPolicy.isInternal("https://evil.test@residual.hotelrovi-ponto.workers.dev/"));
        assertFalse(NavigationPolicy.isInternal("https://residual.hotelrovi-ponto.workers.dev:8443/"));
    }
    @Test public void rejectsUnsafeProtocols() {
        for (String url : new String[] {"javascript:alert(1)", "file:///data/secret", "content://private", "http://ragcodebr.github.io/sistema-residual/", null}) {
            assertFalse(NavigationPolicy.isInternal(url));
            assertFalse(NavigationPolicy.isExternalAllowed(url));
        }
    }
    @Test public void allowsNormalExternalLinksWithoutEmbeddingThem() {
        assertTrue(NavigationPolicy.isExternalAllowed("https://example.com/file.pdf"));
        assertTrue(NavigationPolicy.isExternalAllowed("mailto:contato@example.com"));
        assertTrue(NavigationPolicy.isExternalAllowed("tel:+5511999999999"));
        assertFalse(NavigationPolicy.isExternalAllowed("intent://app/#Intent;scheme=unsafe;end"));
    }
}

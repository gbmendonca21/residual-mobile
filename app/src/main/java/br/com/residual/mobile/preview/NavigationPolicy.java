package br.com.residual.mobile.preview;

import java.net.URI;
import java.net.URISyntaxException;

/** The embedded browser stays within the published Residual application. */
final class NavigationPolicy {
    static final String HOME = "https://ragcodebr.github.io/sistema-residual/";
    static final int MAX_BLOB_BYTES = 20 * 1024 * 1024;
    static final int MAX_DOWNLOAD_BYTES = 100 * 1024 * 1024;

    static boolean isInternal(String value) {
        try {
            URI uri = new URI(value);
            String path = uri.normalize().getPath();
            return "https".equalsIgnoreCase(uri.getScheme())
                && "ragcodebr.github.io".equalsIgnoreCase(uri.getHost())
                && uri.getRawUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && path != null
                && path.startsWith("/sistema-residual/");
        } catch (URISyntaxException | NullPointerException error) {
            return false;
        }
    }

    static boolean isExternalAllowed(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) && uri.getHost() != null
                    && uri.getRawUserInfo() == null)
                || "mailto".equalsIgnoreCase(scheme)
                || "tel".equalsIgnoreCase(scheme);
        } catch (URISyntaxException | NullPointerException error) {
            return false;
        }
    }
}

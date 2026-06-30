package mg.core.mapping;

import java.util.Objects;

public class UrlMethod {
    private String url;
    private String httpMethod;

    public UrlMethod(String url, String httpMethod) {
        this.setUrl(url);
        this.setHttpMethod(httpMethod);
    }

    public UrlMethod() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        UrlMethod that = (UrlMethod) o;

        return url.equals(that.url)
                && httpMethod.equalsIgnoreCase(that.httpMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, httpMethod.toUpperCase());
    }
}

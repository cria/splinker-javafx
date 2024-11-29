package br.org.cria.splinkerapp.models;

import com.google.gson.annotations.SerializedName;

public class GithubAsset {

    @SerializedName("browser_download_url")
    String browserDownloadUrl;
    String name;

    public GithubAsset(String browserDownloadURL, String name) {
        this.browserDownloadUrl = browserDownloadUrl;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrowserDownloadUrl() {
        return browserDownloadUrl;
    }

    public void setBrowserDownloadUrl(String browserDownloadURL) {
        this.browserDownloadUrl = browserDownloadURL;
    }
    
}
